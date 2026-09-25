package servingoutcometests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val REGULAR_PRIORITY = 0
private const val EVENT_PRIORITY = 1
private const val CASUAL_PRIORITY = 2

/**
 * Unit tests for the REGULAR/CASUAL half of F19 (single-assigned-waiter serving):
 * [ServingProcessor.processServing]'s `serveAssignedWaiterTable`/`serveBatch` path, dish
 * and group priority, and the "wait for a complete table, unless capacity runs out" rule.
 * EVENT serving (`serveEventTable`) is out of scope here - see the F19 split notes.
 */
class ServingOutcomeTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
    }

    // Mirrors FrontOfHouse's private getServingPriority: REGULAR, then EVENT, then CASUAL.
    private fun servingPriority(group: CustomerGroup): Int = when (group) {
        is RegularGroup -> REGULAR_PRIORITY
        is EventGroup -> EVENT_PRIORITY
        else -> CASUAL_PRIORITY
    }

    private fun processor(
        waiters: List<Waiter>,
        groups: List<CustomerGroup>,
        waiterFor: Map<CustomerGroup, Waiter>,
        tableFor: Map<CustomerGroup, Int>
    ): ServingProcessor = ServingProcessor(
        waiters = waiters,
        drivers = emptyList<Driver>(),
        deliveryGroups = emptyList(),
        getInHouseGroups = { groups },
        waiterFor = { waiterFor[it] },
        getServingPriority = ::servingPriority,
        getAssignedTableId = { tableFor[it] },
        recruitWaitersForEventGroup = { _, _ -> emptyList() },
        getNextWaiterId = { 0 }
    )

    private fun recipe(id: Int, name: String, basicDishFor: RestaurantType? = null): Recipe = Recipe(
        id,
        name,
        duration = 10,
        cookType = emptyList(),
        ingredients = mutableMapOf(),
        basicDishFor = basicDishFor
    )

    private fun regularGroup(id: Int, order: Order?): RegularGroup {
        val group = RegularGroup(
            id = id,
            size = 4,
            tableType = TableType.COMMON,
            visitingAt = 1,
            foodPreferences = emptyList(),
            visitingStart = 1,
            visitingPeriod = 1,
            restaurantId = 1
        )
        group.currentOrder = order
        return group
    }

    private fun casualGroup(id: Int, order: Order?): CasualGroup {
        val group = CasualGroup(
            id = id,
            size = 4,
            tableType = TableType.COMMON,
            visitingAt = 1,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = 0,
            ratingLikelihood = RatingLikelihood.NEVER
        )
        group.currentOrder = order
        return group
    }

    // Waiters reach ServingProcessor already id'd by SEATING (F16/F17); ServingProcessor
    // itself never assigns one, so a bare Waiter() here would silently suppress every log.
    private fun waiterWithId(id: Int): Waiter = Waiter().apply { this.id = id }

    private fun saturateExceptOneSlot(waiter: Waiter) {
        waiter.addToTickLoad(ActionType.SERVE, Constants.ACTION_LIMIT - 1)
    }

    // ---- Waiting for a complete table ----

    @Test
    fun `a table with some dishes still uncooked is not served yet`() {
        val cookedDish = Dish(recipe(1, "Soup")).apply { status = DishStatus.COOKED }
        val uncookedDish = Dish(recipe(2, "Bread"))
        val order = Order(listOf(cookedDish, uncookedDish))
        val group = regularGroup(1, order)
        val waiter = waiterWithId(1)

        processor(listOf(waiter), listOf(group), mapOf(group to waiter), mapOf(group to 1)).processServing()

        assertEquals(DishStatus.COOKED, cookedDish.status, "not yet served")
        assertTrue(output.toString().contains("FOH No Serving (R 1): Waitstaff"))
        assertFalse(output.toString().contains("FOH Serving (R 1)"))
    }

    @Test
    fun `a fully cooked table with enough capacity is served in one batch`() {
        val dishOne = Dish(recipe(1, "Soup")).apply { status = DishStatus.COOKED }
        val dishTwo = Dish(recipe(2, "Bread")).apply { status = DishStatus.COOKED }
        val order = Order(listOf(dishOne, dishTwo))
        val group = regularGroup(1, order)
        val waiter = waiterWithId(1)

        processor(listOf(waiter), listOf(group), mapOf(group to waiter), mapOf(group to 7)).processServing()

        assertEquals(DishStatus.SERVED, dishOne.status)
        assertEquals(DishStatus.SERVED, dishTwo.status)
        assertTrue(output.toString().contains("FOH Serving (R 1): Waitstaff"))
        assertTrue(output.toString().contains("to table 7"))
    }

    @Test
    fun `insufficient capacity for a complete table serves nothing this tick, then catches up next tick`() {
        val dishes = List(3) { Dish(recipe(it + 1, "Dish $it")).apply { status = DishStatus.COOKED } }
        val order = Order(dishes)
        val group = regularGroup(1, order)
        val waiter = waiterWithId(1)
        saturateExceptOneSlot(waiter) // only 1 of 10 SERVE slots left, but 3 dishes are ready

        processor(listOf(waiter), listOf(group), mapOf(group to waiter), mapOf(group to 1)).processServing()

        assertTrue(dishes.all { it.status == DishStatus.COOKED }, "all-or-nothing: nothing served yet")
        assertTrue(output.toString().contains("FOH No Serving (R 1): Waitstaff"))

        // Next tick: the waiter's SEATING/SERVE load resets, order has already startServing()'d.
        waiter.resetActionLoads()
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        processor(listOf(waiter), listOf(group), mapOf(group to waiter), mapOf(group to 1)).processServing()

        assertTrue(dishes.all { it.status == DishStatus.SERVED })
    }

    // ---- Dish priority within a table ----

    @Test
    fun `basic dishes are served before non basic ones, tied by ascending recipe id`() {
        val nonBasic = Dish(recipe(5, "Fries")).apply { status = DishStatus.COOKED }
        val basicHigh = Dish(recipe(9, "Basic Stew", RestaurantType.EUROPEAN)).apply { status = DishStatus.COOKED }
        val basicLow = Dish(recipe(2, "Basic Soup", RestaurantType.EUROPEAN)).apply { status = DishStatus.COOKED }
        val order = Order(listOf(nonBasic, basicHigh, basicLow))
        order.startServing() // simulate already being in partial-serving mode
        val group = regularGroup(1, order)
        val waiter = waiterWithId(1)
        saturateExceptOneSlot(waiter) // capacity for exactly 1 dish

        processor(listOf(waiter), listOf(group), mapOf(group to waiter), mapOf(group to 1)).processServing()

        assertEquals(DishStatus.SERVED, basicLow.status, "lowest-id basic dish goes first")
        assertEquals(DishStatus.COOKED, basicHigh.status)
        assertEquals(DishStatus.COOKED, nonBasic.status)
    }

    // ---- Ticks-since-ordering ----

    @Test
    fun `ticksSinceOrdering reflects the tick elapsed since the order was placed`() {
        Time.tick = 1
        val dish = Dish(recipe(1, "Soup")).apply { status = DishStatus.COOKED }
        val order = Order(listOf(dish)) // orderedAt = 1
        val group = regularGroup(1, order)
        val waiter = waiterWithId(1)
        Time.tick = 4

        processor(listOf(waiter), listOf(group), mapOf(group to waiter), mapOf(group to 1)).processServing()

        assertTrue(output.toString().contains("3 ticks after ordering."))
    }

    // ---- Group priority ----

    @Test
    fun `a REGULAR group is served before a CASUAL group even with a higher id`() {
        val regularDish = Dish(recipe(1, "Soup")).apply { status = DishStatus.COOKED }
        val casualDish = Dish(recipe(2, "Bread")).apply { status = DishStatus.COOKED }
        val regular = regularGroup(5, Order(listOf(regularDish)))
        val casual = casualGroup(1, Order(listOf(casualDish)))
        val waiter = waiterWithId(1)
        saturateExceptOneSlot(waiter) // capacity for exactly 1 dish total, across both groups

        processor(
            listOf(waiter),
            listOf(casual, regular),
            mapOf(regular to waiter, casual to waiter),
            mapOf(regular to 1, casual to 2)
        ).processServing()

        assertEquals(DishStatus.SERVED, regularDish.status, "REGULAR takes the only remaining capacity")
        assertEquals(DishStatus.COOKED, casualDish.status, "CASUAL gets nothing this tick")
    }

    @Test
    fun `within the same group type the lower id is served first`() {
        val firstDish = Dish(recipe(1, "Soup")).apply { status = DishStatus.COOKED }
        val secondDish = Dish(recipe(2, "Bread")).apply { status = DishStatus.COOKED }
        val groupOne = casualGroup(1, Order(listOf(firstDish)))
        val groupTwo = casualGroup(2, Order(listOf(secondDish)))
        val waiter = waiterWithId(1)
        saturateExceptOneSlot(waiter)

        processor(
            listOf(waiter),
            listOf(groupTwo, groupOne),
            mapOf(groupOne to waiter, groupTwo to waiter),
            mapOf(groupOne to 1, groupTwo to 2)
        ).processServing()

        assertEquals(DishStatus.SERVED, firstDish.status)
        assertEquals(DishStatus.COOKED, secondDish.status)
    }

    // ---- Defensive: no order yet ----

    @Test
    fun `a group with no current order is skipped without error`() {
        val group = regularGroup(1, order = null)
        val waiter = waiterWithId(1)

        processor(listOf(waiter), listOf(group), mapOf(group to waiter), mapOf(group to 1)).processServing()

        assertFalse(output.toString().contains("FOH Serving (R 1): Waitstaff"))
        assertFalse(output.toString().contains("FOH No Serving (R 1): Waitstaff"))
    }

    // ---- Partial serving after the wait window ----

    /**
     * Once [Constants.PARTIAL_SERVING_WAIT_TICKS] has passed since the first dish of
     * an order was cooked, the table is served one dish at a time even though the rest of the order
     * is still cooking. `Kitchen.recordFirstCookedTicks` sets `order.firstDishCookedAt`. This unit
     * test runs only the ServingProcessor, so it sets that field itself.
     */
    @Test
    fun `a partially cooked table waits out the window and is served one dish at a time right after it`() {
        val cookedDish = Dish(recipe(1, "Soup")).apply { status = DishStatus.COOKED }
        val stillCooking = Dish(recipe(2, "Bread"))
        val order = Order(listOf(cookedDish, stillCooking)).apply { firstDishCookedAt = 1 }
        val group = regularGroup(1, order)
        val waiter = waiterWithId(1)

        // spec p. 16: not served in the tick the first meal was cooked nor in the following tick
        Time.tick = 1 + Constants.PARTIAL_SERVING_WAIT_TICKS
        processor(listOf(waiter), listOf(group), mapOf(group to waiter), mapOf(group to 1)).processServing()
        assertEquals(DishStatus.COOKED, cookedDish.status, "still inside the wait window")

        Time.tick += 1
        processor(listOf(waiter), listOf(group), mapOf(group to waiter), mapOf(group to 1)).processServing()
        assertEquals(DishStatus.SERVED, cookedDish.status, "first tick after the window")
        assertEquals(DishStatus.UNCOOKED, stillCooking.status)
    }
}
