package customertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * integration tests for F23 (Customer - Casual)
 * a CASUAL group's rating going through the real [FrontOfHouse]
 * which decides who is eligible to rate this tick and drops the groups that have rated
 */
class CasualRatingIntegrationTest {
    private lateinit var output: StringWriter

    private val water = Ingredient("water", MeasurementUnit.ML, 5, 1000)
    private val soup = Recipe(1, "soup", 10, listOf(CookType.TOURNANT), mutableMapOf(water to 1), null)
    private val menu = listOf(soup)

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
        Time.evening = 1
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun ratingLines(): List<String> = lines().filter { it.contains("Rating (R") }

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse {
        val pantry = Pantry(
            inventory = mutableListOf(IngredientPackage(water)),
            supplier = Supplier(Stock(listOf(water)))
        )
        val countertop = Countertop(
            pantry,
            ArrayDeque<Order>(),
            listOf(Cook(CookType.TOURNANT)),
            RestaurantType.EUROPEAN
        )
        return FrontOfHouse(tables, waiters, emptyList(), countertop)
    }

    private fun casual(id: Int, likelihood: RatingLikelihood, size: Int = 2) = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = likelihood,
    )

    private fun finishEating(group: CustomerGroup) {
        val order = checkNotNull(group.currentOrder) { "the group should have ordered, got ${lines()}" }
        order.dishes.forEach { it.status = DishStatus.EATEN }
    }

    /** seats the group, lets it eat and escorts it, leaving it ready to rate this tick */
    private fun seatEatAndEscort(foh: FrontOfHouse, group: CasualGroup) {
        foh.processArrival(group, menu)
        finishEating(group)
        foh.processEscorting()
    }

    @Test
    fun `a group that ate and left rates the restaurant and moves its counters`() {
        val foh = frontOfHouse(listOf(Table(1, 2, TableType.COMMON)), listOf(Waiter()))
        val group = casual(1, RatingLikelihood.ALWAYS)
        seatEatAndEscort(foh, group)

        val result = foh.processRatings(4, 2)

        assertEquals(Pair(5, 2), result)
        assertEquals(
            "[INFO] Rating (R 1): Group 1 rates the restaurant 1 with POSITIVE rating, " +
                "leading to 5 positive ratings and 2 negative ratings.",
            ratingLines().single()
        )
    }

    @Test
    fun `a NEVER group leaves the restaurant without rating it`() {
        val foh = frontOfHouse(listOf(Table(1, 2, TableType.COMMON)), listOf(Waiter()))
        val group = casual(1, RatingLikelihood.NEVER)
        seatEatAndEscort(foh, group)

        val result = foh.processRatings(4, 2)

        assertEquals(Pair(4, 2), result)
        assertTrue(ratingLines().isEmpty())
        assertEquals(
            "[DEBUG] Rating Status (R 1): 0 groups performed ratings this tick.",
            lines().last { it.contains("Rating Status") }
        )
    }

    /** a SOME group doesn't rate about an evening that was neutral */
    @Test
    fun `a SOME group says nothing about a neutral evening`() {
        val foh = frontOfHouse(listOf(Table(1, 2, TableType.COMMON)), listOf(Waiter()))
        val group = casual(1, RatingLikelihood.SOME)
        seatEatAndEscort(foh, group)

        assertEquals(Pair(0, 0), foh.processRatings(0, 0))
        assertTrue(ratingLines().isEmpty())
    }

    @Test
    fun `a group still sitting at its table does not rate yet`() {
        val foh = frontOfHouse(listOf(Table(1, 2, TableType.COMMON)), listOf(Waiter()))
        val group = casual(1, RatingLikelihood.ALWAYS)
        foh.processArrival(group, menu)

        val result = foh.processRatings(0, 0)

        assertEquals(Pair(0, 0), result)
        assertTrue(ratingLines().isEmpty())
    }

    /** the group must be dropped once it has rated, or it would rate again every later tick */
    @Test
    fun `a group rates once and not again in the following ticks`() {
        val foh = frontOfHouse(listOf(Table(1, 2, TableType.COMMON)), listOf(Waiter()))
        val group = casual(1, RatingLikelihood.ALWAYS)
        seatEatAndEscort(foh, group)

        val afterFirst = foh.processRatings(0, 0)
        Time.tick = 6
        foh.clearActionLoads()
        val afterSecond = foh.processRatings(afterFirst.first, afterFirst.second)

        assertEquals(Pair(1, 0), afterFirst)
        assertEquals(Pair(1, 0), afterSecond, "the group has left and must not rate twice")
        assertEquals(1, ratingLines().size)
    }

    /** still waiting for food when the restaurant closes is a negative experience */
    @Test
    fun `a group that never got its food rates negative at closing`() {
        val foh = frontOfHouse(listOf(Table(1, 2, TableType.COMMON)), listOf(Waiter()))
        val group = casual(1, RatingLikelihood.SOME)
        foh.processArrival(group, menu)

        foh.startFohClosing()
        val result = foh.processRatings(3, 3)

        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertEquals(Pair(3, 4), result)
        assertTrue(ratingLines().single().contains("with NEGATIVE rating"))
    }

    /** a group that had finished eating when the restaurant closed keeps its neutral experience */
    @Test
    fun `a group that had finished eating is not affected by closing`() {
        val foh = frontOfHouse(listOf(Table(1, 2, TableType.COMMON)), listOf(Waiter()))
        val group = casual(1, RatingLikelihood.ALWAYS)
        foh.processArrival(group, menu)
        finishEating(group)

        foh.startFohClosing()
        val result = foh.processRatings(0, 0)

        assertEquals(ExperienceType.NEUTRAL, group.experience)
        assertEquals(Pair(1, 0), result)
    }

    /** several groups leaving in the same tick are all counted by the status log */
    @Test
    fun `the rating status counts every group that rated this tick`() {
        val foh = frontOfHouse(
            listOf(Table(1, 2, TableType.COMMON), Table(2, 2, TableType.COMMON)),
            listOf(Waiter(), Waiter())
        )
        val first = casual(1, RatingLikelihood.ALWAYS)
        val second = casual(2, RatingLikelihood.NEVER)
        foh.processArrival(first, menu)
        foh.processArrival(second, menu)
        finishEating(first)
        finishEating(second)
        foh.processEscorting()

        foh.processRatings(0, 0)

        assertEquals(
            "[DEBUG] Rating Status (R 1): 1 groups performed ratings this tick.",
            lines().last { it.contains("Rating Status") },
            "the NEVER group left without rating and is not counted"
        )
    }
}
