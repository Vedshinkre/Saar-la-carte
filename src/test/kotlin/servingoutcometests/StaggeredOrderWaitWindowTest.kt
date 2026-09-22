package servingoutcometests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
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

/**
 * F27/F18, "FrontOfHouse" angle rather than `EatingProcessor`: two REGULAR groups order their food in
 * different ticks (staggered), each partly cooked, so `ServingProcessor.isWithinTimeWindow` has to
 * track each order's own `firstDishCookedAt` independently rather than off of a single shared clock.
 * `order.firstDishCookedAt` is set once by `Kitchen.recordFirstCookedTicks`; this test, like the
 * existing single-order `ServingOutcomeTest` "partial serving" tests, sets it directly to isolate
 * `ServingProcessor` from kitchen timing.
 *
 * `ServingOutcomeTest` only ever drives one order's clock at a time; this test's point is that the
 * two groups' clocks do not interfere with each other in the same `processServing()` call, i.e.
 * whichever order's own window has elapsed is served partially, and the other is not, purely because
 * of when *that* order's first dish was cooked - not the tick, not the other order's dishes.
 */
class StaggeredOrderWaitWindowTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Order.resetIds()
    }

    private fun recipe(id: Int, name: String): Recipe = Recipe(
        id,
        name,
        duration = 10,
        cookType = emptyList(),
        ingredients = mutableMapOf(),
        basicDishFor = null
    )

    private fun regularGroup(id: Int, order: Order): RegularGroup {
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

    private fun waiterWithId(id: Int) = Waiter().apply { this.id = id }

    private fun processor(
        waiters: List<Waiter>,
        groups: List<CustomerGroup>,
        waiterFor: Map<CustomerGroup, Waiter>,
        tableFor: Map<CustomerGroup, Int>
    ) = ServingProcessor(
        waiters = waiters,
        drivers = emptyList<Driver>(),
        deliveryGroups = emptyList(),
        getInHouseGroups = { groups },
        waiterFor = { waiterFor[it] },
        getServingPriority = { 0 },
        getAssignedTableId = { tableFor[it] },
        recruitWaitersForEventGroup = { _, _ -> emptyList() },
        getNextWaiterId = { 0 }
    )

    @Test
    fun `an order whose own wait window elapsed is served partially, an order that just started cooking is not`() {
        // group A ordered long ago: its first dish was cooked well outside PARTIAL_SERVING_WAIT_TICKS
        val cookedA = Dish(recipe(1, "Soup A")).apply { status = DishStatus.COOKED }
        val stillCookingA = Dish(recipe(2, "Bread A"))
        val orderA = Order(listOf(cookedA, stillCookingA)).apply { firstDishCookedAt = 1 }
        val groupA = regularGroup(1, orderA)

        // group B ordered just now, in the very same tick its first dish finished cooking
        val cookedB = Dish(recipe(3, "Soup B")).apply { status = DishStatus.COOKED }
        val stillCookingB = Dish(recipe(4, "Bread B"))
        val orderB = Order(listOf(cookedB, stillCookingB))
        val groupB = regularGroup(2, orderB)

        Time.tick = 1 + Constants.PARTIAL_SERVING_WAIT_TICKS + 5
        orderB.firstDishCookedAt = Time.tick // group B's window has not elapsed at all yet

        val waiterA = waiterWithId(1)
        val waiterB = waiterWithId(2)

        processor(
            listOf(waiterA, waiterB),
            listOf(groupA, groupB),
            mapOf(groupA to waiterA, groupB to waiterB),
            mapOf(groupA to 1, groupB to 2)
        ).processServing()

        assertEquals(DishStatus.SERVED, cookedA.status, "group A's window elapsed, so it is served partially")
        assertEquals(DishStatus.COOKED, cookedB.status, "group B's window just started, so it must still wait")
    }

    @Test
    fun `once the later order's own window also elapses, it too gets served partially, on its own clock`() {
        val cookedA = Dish(recipe(1, "Soup A")).apply { status = DishStatus.COOKED }
        val orderA = Order(listOf(cookedA, Dish(recipe(2, "Bread A")))).apply { firstDishCookedAt = 1 }
        val groupA = regularGroup(1, orderA)

        val cookedB = Dish(recipe(3, "Soup B")).apply { status = DishStatus.COOKED }
        val orderB = Order(listOf(cookedB, Dish(recipe(4, "Bread B"))))
        val groupB = regularGroup(2, orderB)

        val waiterA = waiterWithId(1)
        val waiterB = waiterWithId(2)

        // tick 1: only group A's window has elapsed (as above)
        Time.tick = 1 + Constants.PARTIAL_SERVING_WAIT_TICKS + 5
        orderB.firstDishCookedAt = Time.tick
        processor(
            listOf(waiterA, waiterB),
            listOf(groupA, groupB),
            mapOf(groupA to waiterA, groupB to waiterB),
            mapOf(groupA to 1, groupB to 2)
        ).processServing()
        assertEquals(DishStatus.COOKED, cookedB.status, "still within its own window")

        // later: group B's own window has now elapsed too, independent of group A's already-served order
        Time.tick += Constants.PARTIAL_SERVING_WAIT_TICKS + 1
        waiterB.resetActionLoads()
        processor(
            listOf(waiterA, waiterB),
            listOf(groupA, groupB),
            mapOf(groupA to waiterA, groupB to waiterB),
            mapOf(groupA to 1, groupB to 2)
        ).processServing()

        assertEquals(DishStatus.SERVED, cookedB.status, "group B's own window has now elapsed")
    }
}
