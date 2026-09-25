package seatingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for F16 through the real [FrontOfHouse] wiring (its actual
 * `recruitWaitersForEventGroup`/`getNextWaiterId` closures and shared table/customer
 * state), rather than constructing [de.unisaarland.cs.se.selab.restaurant.helpers.ArrivalProcessor]
 * directly. Unlike the unit tests, these give the group a real orderable dish so the whole
 * arrival-to-order pipeline runs cleanly end to end.
 */
class SeatingIntegrationTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
    }

    @AfterEach
    fun tearDown() {
        Logger.setup(PrintWriter(System.out))
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private val flour = Ingredient("flour", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)
    private val breadRecipe = Recipe(
        id = 1,
        name = "Bread",
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(flour to 10),
        basicDishFor = null
    )
    private val menu = listOf(breadRecipe)

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse {
        val stock = Stock(listOf(flour))
        val pantry = Pantry(inventory = mutableListOf(IngredientPackage(flour)), supplier = Supplier(stock))
        val countertop = Countertop(
            pantry = pantry,
            orderQueue = ArrayDeque(),
            cooks = listOf(Cook(CookType.TOURNANT)),
            restaurantType = RestaurantType.EUROPEAN
        )
        return FrontOfHouse(tables = tables, waiters = waiters, drivers = emptyList(), countertop = countertop)
    }

    private fun foodPreferences(count: Int): List<FoodPreference> =
        List(count) { FoodPreference(emptyList(), emptyList(), emptyList()) }

    private fun casualGroup(id: Int, size: Int, visitingAt: Int = 1): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = foodPreferences(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    private fun regularGroup(id: Int, size: Int, visitingAt: Int): RegularGroup = RegularGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = foodPreferences(size),
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1
    )

    private fun eventGroup(id: Int, size: Int, visitingAt: Int): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = foodPreferences(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = 1,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "Bread")
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    @Test
    fun `a CASUAL group with a real order gets an ad-hoc table and the pipeline succeeds end to end`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val waiter = Waiter()
        val foh = frontOfHouse(listOf(table), listOf(waiter))
        val group = casualGroup(id = 1, size = 4)

        val removedFromQueue = foh.processArrival(group, menu)

        assertTrue(removedFromQueue)
        assertEquals(TableStatus.OCCUPIED, table.status)
        assertNotNull(group.currentOrder)
        assertEquals(4, group.customersRemainingInRestaurant)
    }

    @Test
    fun `once the only eligible table is taken a second CASUAL group is turned away for lack of a table`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(Waiter(), Waiter()))
        val firstGroup = casualGroup(id = 1, size = 4)
        val secondGroup = casualGroup(id = 2, size = 4)

        foh.processArrival(firstGroup, menu)
        foh.processArrival(secondGroup, menu)

        assertEquals(TableStatus.OCCUPIED, table.status)
        assertEquals(ExperienceType.NEGATIVE, secondGroup.experience)
        assertTrue(lines().any { it.endsWith("but no table available, group 2 is sent away.") }, lines().toString())
        assertEquals(4, secondGroup.customersRemainingInRestaurant, "no one from group 2 was ever seated or served")
    }

    @Test
    fun `a REGULAR group blocked by a saturated waiter is re-queued, not turned away`() {
        val tableOne = Table(id = 1, size = 10, tableType = TableType.COMMON)
        val tableTwo = Table(id = 2, size = 3, tableType = TableType.COMMON)
        val foh = frontOfHouse(listOf(tableOne, tableTwo), listOf(Waiter()))
        val groupOne = regularGroup(id = 1, size = 10, visitingAt = 5)
        val groupTwo = regularGroup(id = 2, size = 3, visitingAt = 5)
        foh.reserveTables(groupOne)
        foh.reserveTables(groupTwo)
        Time.tick = 5

        foh.processArrival(groupOne, menu)
        val removedFromQueue = foh.processArrival(groupTwo, menu)

        assertFalse(removedFromQueue, "group two should stay in the queue for a retry, not be dropped")
        assertEquals(ExperienceType.NEUTRAL, groupTwo.experience)
        assertEquals(TableStatus.RESERVED, tableTwo.status)
    }

    @Test
    fun `a REGULAR and an EVENT group seated in the same tick hand out ids lazily without corrupting each other`() {
        // waiter A is already a little busier than B and C, so it deterministically wins the
        // REGULAR group's assignment (rule 2: most customers below a load of 10).
        val waiterA = Waiter().also { it.currentLoad = 2 }
        val waiterB = Waiter()
        val waiterC = Waiter()
        val regularTable = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val eventTable = Table(id = 2, size = 8, tableType = TableType.COMMON)
        val foh = frontOfHouse(listOf(regularTable, eventTable), listOf(waiterA, waiterB, waiterC))
        val regular = regularGroup(id = 1, size = 4, visitingAt = 5)
        val event = eventGroup(id = 2, size = 8, visitingAt = 5)
        foh.reserveTables(regular)
        foh.reserveTables(event)
        Time.tick = 5

        foh.processArrival(regular, menu)
        foh.processArrival(event, menu)

        // ids are handed out lazily, in the order waiters first actually act
        assertEquals(1, waiterA.id, "A acted first, for the REGULAR group")
        assertEquals(2, waiterB.id, "B acted next, recruited for the remainder of the EVENT group")
        assertNull(waiterC.id, "C was never needed by either group")

        // the REGULAR group's own assignment must survive the EVENT recruitment untouched
        assertTrue(
            lines().any { it.contains("FOH Ordering (R 1): Group 1") && it.endsWith("with waitstaff 1.") },
            lines().toString()
        )
        assertEquals(6, waiterA.currentLoad, "A's currentLoad only reflects the REGULAR group (2 + 4)")
        assertEquals(0, waiterB.currentLoad, "EVENT groups do not count towards currentLoad")

        // the EVENT group was seated using exactly A and B, not C
        assertTrue(
            lines().single { it.contains("FOH Seating (R") && it.contains("Group 2 ") }
                .endsWith("by waitstaff 1,2."),
            lines().toString()
        )
    }
}
