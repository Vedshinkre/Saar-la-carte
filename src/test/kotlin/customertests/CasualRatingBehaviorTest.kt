package customertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.helpers.RatingProcessor
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * tests for F23
 * CASUAL group experience and rating behavior [RatingProcessor] (visiting and delivery timing tested by Deniz)
 */
class CasualRatingBehaviorTest {
    private lateinit var output: StringWriter

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

    private val water = Ingredient("water", MeasurementUnit.ML, 10, 1)
    private val soup = Recipe(1, "soup", 1, emptyList(), mutableMapOf(water to 1), null)

    private fun order(status: DishStatus) = Order(listOf(Dish(soup, false, 0, status)))

    private fun casual(
        likelihood: RatingLikelihood,
        experience: ExperienceType = ExperienceType.NEUTRAL,
        id: Int = 1,
        remaining: Int = 0
    ) = CasualGroup(
        id = id,
        size = 2,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = likelihood
    ).also {
        it.experience = experience
        it.customersRemainingInRestaurant = remaining
    }

    private fun processor(
        inHouse: List<CustomerGroup>,
        removed: MutableList<CustomerGroup> = mutableListOf()
    ) = RatingProcessor(
        deliveryGroups = mutableListOf(),
        turnedAwayGroups = emptyList(),
        eventGroups = mutableListOf(),
        getInHouseGroups = { inHouse },
        getServingPriority = { 2 },
        removeProcessedGroup = { removed.add(it) }
    )

    // the rating likelihood decides whether anything is said at all

    @Test
    fun `a NEVER group leaves the ratings and the log untouched`() {
        val group = casual(RatingLikelihood.NEVER, ExperienceType.NEGATIVE)

        val result = processor(listOf(group)).processRatings(3, 1)

        assertEquals(Pair(3, 1), result)
        assertTrue(ratingLines().isEmpty())
        assertEquals(
            "[DEBUG] Rating Status (R 1): 0 groups performed ratings this tick.",
            lines().last { it.contains("Rating Status") }
        )
    }

    @Test
    fun `a SOME group says nothing about an ordinary evening`() {
        val group = casual(RatingLikelihood.SOME, ExperienceType.NEUTRAL)

        val result = processor(listOf(group)).processRatings(3, 1)

        assertEquals(Pair(3, 1), result)
        assertTrue(ratingLines().isEmpty())
    }

    @Test
    fun `a SOME group complains about a bad evening`() {
        val group = casual(RatingLikelihood.SOME, ExperienceType.NEGATIVE)

        val result = processor(listOf(group)).processRatings(3, 1)

        assertEquals(Pair(3, 2), result)
        assertEquals(
            "[INFO] Rating (R 1): Group 1 rates the restaurant 1 with NEGATIVE rating, " +
                "leading to 3 positive ratings and 2 negative ratings.",
            ratingLines().single()
        )
        assertEquals(
            "[DEBUG] Rating Status (R 1): 1 groups performed ratings this tick.",
            lines().last { it.contains("Rating Status") }
        )
    }

    @Test
    fun `an ALWAYS group turns an ordinary evening into a positive rating`() {
        val group = casual(RatingLikelihood.ALWAYS, ExperienceType.NEUTRAL)

        val result = processor(listOf(group)).processRatings(3, 1)

        assertEquals(Pair(4, 1), result)
        assertEquals(
            "[INFO] Rating (R 1): Group 1 rates the restaurant 1 with POSITIVE rating, " +
                "leading to 4 positive ratings and 1 negative ratings.",
            ratingLines().single()
        )
    }

    @Test
    fun `a good evening is rated positive whatever the likelihood is`() {
        val some = casual(RatingLikelihood.SOME, ExperienceType.POSITIVE, id = 1)
        val always = casual(RatingLikelihood.ALWAYS, ExperienceType.POSITIVE, id = 2)

        val result = processor(listOf(some, always)).processRatings(0, 0)

        assertEquals(Pair(2, 0), result)
        assertEquals(2, ratingLines().size)
    }

    // who gets to rate this tick

    @Test
    fun `only a group that has completely left rates this tick`() {
        val left = casual(RatingLikelihood.ALWAYS, ExperienceType.NEGATIVE, id = 1, remaining = 0)
        val stillSeated = casual(RatingLikelihood.ALWAYS, ExperienceType.NEGATIVE, id = 2, remaining = 1)
        val removed = mutableListOf<CustomerGroup>()

        val result = processor(listOf(left, stillSeated), removed).processRatings(0, 0)

        assertEquals(Pair(0, 1), result)
        assertEquals<List<CustomerGroup>>(
            listOf(left),
            removed,
            "a group still at its table is neither rated nor dropped"
        )
    }

    // closing time turns an unfinished evening negative first

    @Test
    fun `a group that did not finish eating at closing rates negative`() {
        val group = casual(RatingLikelihood.ALWAYS, ExperienceType.NEUTRAL)
        group.currentOrder = order(DishStatus.SERVED)

        val result = processor(listOf(group)).rate(group, 0, 0, closing = true)

        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertEquals(Pair(0, 1), result)
    }

    @Test
    fun `a group that never got an order at closing rates negative`() {
        val group = casual(RatingLikelihood.SOME, ExperienceType.NEUTRAL)

        val result = processor(listOf(group)).rate(group, 0, 0, closing = true)

        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertEquals(Pair(0, 1), result)
    }

    @Test
    fun `a group that finished eating before closing keeps its experience`() {
        val group = casual(RatingLikelihood.ALWAYS, ExperienceType.NEUTRAL)
        group.currentOrder = order(DishStatus.EATEN)

        val result = processor(listOf(group)).rate(group, 0, 0, closing = true)

        assertEquals(ExperienceType.NEUTRAL, group.experience)
        assertEquals(Pair(1, 0), result)
    }

    /** The likelihood still wins: a NEVER group stays silent even about a ruined evening. */
    @Test
    fun `a NEVER group says nothing even at closing`() {
        val group = casual(RatingLikelihood.NEVER, ExperienceType.NEUTRAL)

        val result = processor(listOf(group)).rate(group, 2, 2, closing = true)

        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertEquals(Pair(2, 2), result)
        assertTrue(ratingLines().isEmpty())
    }
}
