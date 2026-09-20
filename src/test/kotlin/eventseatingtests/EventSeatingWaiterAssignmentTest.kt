package eventseatingtests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P02 (FOH - Event Seating)
 *
 * covers how FOH recruits and distributes waiters for an EVENT group's SEATING action
 * tests preset each waiter's `currentLoad` and SEATING tick load directly
 * so the recruitment order and distribution logic can be
 * checked without populating that state through prior seatings
 */
class EventSeatingWaiterAssignmentTest {
    private lateinit var output: StringWriter

    private val broth = Ingredient("broth", MeasurementUnit.ML, 1000, 1000)
    private val soup = Recipe(1, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(broth to 1), null)
    private val menu = listOf(soup)

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
        Time.evening = 1
        Time.ticksElapsed = 0
        Order.resetIds()
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.ticksElapsed = 0
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun countertop(): Countertop {
        val supplier = Supplier(Stock(listOf(broth)))
        val pantry = Pantry(mutableListOf(IngredientPackage(broth)), supplier)
        return Countertop(pantry, ArrayDeque(), listOf(Cook(CookType.TOURNANT)), RestaurantType.EUROPEAN)
    }

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse =
        FrontOfHouse(tables = tables, waiters = waiters, drivers = emptyList(), countertop = countertop())

    private fun eventGroup(id: Int, size: Int): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = Time.evening,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "Soup")
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    /** Reserves a single table exactly the group's size so seating can succeed. */
    private fun reserveTableFor(foh: FrontOfHouse, group: EventGroup) {
        assertTrue(foh.reserveTables(group), "table reservation is a precondition for these tests")
    }

    /**
     * example from spec: a 15-customer group is split 10, then 4, then 1 across three waiters,
     * based on their descending currentLoad
     */
    @Test
    fun `the spec example splits 15 customers as 10, then 4, then 1`() {
        val busiest = Waiter().also { it.currentLoad = 15 }
        val middle = Waiter().also {
            it.currentLoad = 11
            it.tickLoads[ActionType.SEAT] = 6
        }
        val idle = Waiter()
        val waiters = listOf(busiest, middle, idle)
        val table = Table(1, 15, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), waiters)
        val group = eventGroup(1, 15)
        reserveTableFor(foh, group)

        assertTrue(foh.processArrival(group, menu))

        assertEquals(10, busiest.getTickLoad(ActionType.SEAT))
        assertEquals(10, middle.getTickLoad(ActionType.SEAT))
        assertEquals(1, idle.getTickLoad(ActionType.SEAT))
        assertEquals(15, group.currentOrder?.dishes?.size)
        assertTrue(
            lines().any { it.contains("FOH Seating (R 1): Group 1 seated at table 1 by waitstaff") },
            "the group is seated as a whole once its distribution succeeds"
        )
    }

    @Test
    fun `waiters are recruited in strictly descending currentLoad order`() {
        val low = Waiter().also { it.currentLoad = 1 }
        val high = Waiter().also { it.currentLoad = 7 }
        val mid = Waiter().also { it.currentLoad = 3 }
        // scrambled list order: the sort should decide priority, input order shouldn't matter
        val waiters = listOf(low, high, mid)
        val table = Table(1, 12, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), waiters)
        val group = eventGroup(1, 12)
        reserveTableFor(foh, group)

        assertTrue(foh.processArrival(group, menu))

        val seat = ActionType.SEAT
        assertEquals(10, high.getTickLoad(seat), "the highest currentLoad waiter is filled first")
        assertEquals(2, mid.getTickLoad(seat), "the remainder goes to the next-highest currentLoad waiter")
        assertEquals(0, low.getTickLoad(seat), "the lowest currentLoad waiter is never reached")
        assertNull(low.id, "a waiter that is never consumed never receives an id")
    }

    @Test
    fun `waiters with equal currentLoad are recruited in lowest id order`() {
        val highId = Waiter().also {
            it.currentLoad = 5
            it.id = 9
        }
        val lowId = Waiter().also {
            it.currentLoad = 5
            it.id = 2
        }
        // list order is the opposite of id order, so only the tie-break rule can explain the result
        val waiters = listOf(highId, lowId)
        val table = Table(1, 3, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), waiters)
        val group = eventGroup(1, 3)
        reserveTableFor(foh, group)

        assertTrue(foh.processArrival(group, menu))

        val seat = ActionType.SEAT
        assertEquals(3, lowId.getTickLoad(seat), "the lower id wins the tie and takes the whole group")
        assertEquals(0, highId.getTickLoad(seat), "the higher id is never reached")
    }

    @Test
    fun `a waiter already at the SEATING action limit is skipped entirely`() {
        val maxed = Waiter().also { it.tickLoads[ActionType.SEAT] = Constants.ACTION_LIMIT }
        val free = Waiter()
        val waiters = listOf(maxed, free)
        val table = Table(1, 3, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), waiters)
        val group = eventGroup(1, 3)
        reserveTableFor(foh, group)

        assertTrue(foh.processArrival(group, menu))

        assertEquals(Constants.ACTION_LIMIT, maxed.getTickLoad(ActionType.SEAT))
        assertNull(maxed.id, "the maxed-out waiter is filtered out before recruitment, so it is never touched")
        assertEquals(3, free.getTickLoad(ActionType.SEAT))
    }

    @Test
    fun `a partially used waiter contributes only their remaining capacity`() {
        val partial = Waiter().also {
            it.currentLoad = 1
            it.tickLoads[ActionType.SEAT] = 6
        }
        val untouched = Waiter()
        val waiters = listOf(partial, untouched)
        val table = Table(1, 4, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), waiters)
        val group = eventGroup(1, 4)
        reserveTableFor(foh, group)

        assertTrue(foh.processArrival(group, menu))

        val seat = ActionType.SEAT
        assertEquals(10, partial.getTickLoad(seat), "6 + the remaining 4 fills the partial waiter exactly")
        assertEquals(0, untouched.getTickLoad(seat), "the group was fully covered before reaching this waiter")
        assertNull(untouched.id)
    }

    @Test
    fun `a group matching the waiters' total remaining capacity is fully seated`() {
        val first = Waiter().also { it.currentLoad = 2 }
        val second = Waiter().also {
            it.currentLoad = 1
            it.tickLoads[ActionType.SEAT] = 2
        }
        // total remaining capacity: 10 + 8 = 18
        val waiters = listOf(first, second)
        val table = Table(1, 18, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), waiters)
        val group = eventGroup(1, 18)
        reserveTableFor(foh, group)

        assertTrue(foh.processArrival(group, menu))

        val seat = ActionType.SEAT
        assertEquals(10, first.getTickLoad(seat))
        assertEquals(10, second.getTickLoad(seat))
        assertEquals(
            ExperienceType.NEUTRAL,
            group.experience,
            "a successful seating does not mark a negative experience"
        )
        assertTrue(lines().any { it.contains("FOH Seating (R 1): Group 1 seated") })
    }

    @Test
    fun `a group one customer over capacity fails but the charged tick load is not rolled back`() {
        val first = Waiter().also { it.currentLoad = 2 }
        val second = Waiter().also {
            it.currentLoad = 1
            it.tickLoads[ActionType.SEAT] = 2
        }
        // total remaining capacity is 18; one customer over that must fail as a whole
        val waiters = listOf(first, second)
        val table = Table(1, 19, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), waiters)
        val group = eventGroup(1, 19)
        reserveTableFor(foh, group)

        assertTrue(foh.processArrival(group, menu))

        val seat = ActionType.SEAT
        assertEquals(10, first.getTickLoad(seat), "both waiters are still charged for the failed attempt")
        assertEquals(10, second.getTickLoad(seat))
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(
            lines().any { it.contains("FOH No Seating (R 1): No free waitstaff available for group 1.") }
        )
        assertTrue(
            lines().none { it.contains("FOH Seating (R 1): Group 1 seated") },
            "the group itself is never seated"
        )
    }

    @Test
    fun `currentLoad is untouched by event seating and ids are assigned even on failure`() {
        val first = Waiter().also { it.currentLoad = 4 }
        val second = Waiter().also { it.currentLoad = 2 }
        val waiters = listOf(first, second)
        val table = Table(1, 21, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), waiters)
        // one more than the waiters' combined 20 remaining capacity: the attempt must fail
        val group = eventGroup(1, 21)
        reserveTableFor(foh, group)

        assertTrue(foh.processArrival(group, menu))

        assertEquals(4, first.currentLoad, "EVENTS never count toward a waiter's currentLoad")
        assertEquals(2, second.currentLoad)
        assertTrue(first.id != null && second.id != null, "both consumed waiters receive an id despite the failure")
        assertEquals(ExperienceType.NEGATIVE, group.experience)
    }
}
