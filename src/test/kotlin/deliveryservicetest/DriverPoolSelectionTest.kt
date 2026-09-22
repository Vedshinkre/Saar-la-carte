package deliveryservicetest

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * F20: `ServingProcessor.getOrAssignDriver` (private, exercised through `processServing`) picks a
 * free driver with `drivers.find { it.state == DriverState.IDLE }`, i.e. the first IDLE driver in
 * restaurant-list order, not the one with the lowest id, and once a driver has taken an order it is
 * "locked" to it across ticks: the very first thing `getOrAssignDriver` does is look for a driver
 * `it.targetGroup == group && it.currentOrder == order` already assigned, before ever touching the
 * IDLE pool again. Neither of those two rules has a dedicated test: the existing
 * `DeliveryHandOverTest` / `DeliveryOutboundTripTest` suites only ever exercise a single driver
 * (or several, but always added to the list in exactly the order they should be picked), so they
 * cannot tell "first in list" apart from "lowest id" or "lowest id keeps its own order across ticks"
 * apart from "reassigned freshly every tick, coincidentally to the same driver".
 */
class DriverPoolSelectionTest {

    @BeforeEach
    fun setup() {
        Time.tick = 1
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Order.resetIds()
    }

    private val recipe = Recipe(1, "Soup", 10, emptyList(), mutableMapOf(), null)

    private fun deliveryGroup(id: Int): CasualGroup {
        val group = CasualGroup(
            id = id,
            size = 1,
            tableType = TableType.COMMON,
            visitingAt = 10,
            foodPreferences = emptyList(),
            restaurantTypes = emptyList(),
            visitingEvenings = listOf(1),
            deliveryDistance = 5,
            ratingLikelihood = RatingLikelihood.NEVER
        )
        group.currentOrder = Order(listOf(Dish(recipe).apply { status = DishStatus.COOKED }))
        return group
    }

    private fun serving(waiters: List<Waiter>, drivers: List<Driver>, groups: List<CasualGroup>) = ServingProcessor(
        waiters = waiters,
        drivers = drivers,
        deliveryGroups = groups,
        getInHouseGroups = { emptyList() },
        waiterFor = { null },
        getServingPriority = { 2 },
        getAssignedTableId = { null },
        recruitWaitersForEventGroup = { _, _ -> emptyList() },
        getNextWaiterId = { 1 }
    )

    @Test
    fun `an idle driver already holding a higher id is still picked before a fresh, id-less one later in the list`() {
        // a driver that already delivered once this evening and is back IDLE, keeping its id
        val alreadyUsed = Driver().apply {
            id = 9
            state = DriverState.IDLE
        }
        // a driver nobody has ever assigned yet, listed after it
        val neverUsed = Driver().apply { state = DriverState.IDLE }
        val group = deliveryGroup(1)

        serving(listOf(Waiter().apply { id = 1 }), listOf(alreadyUsed, neverUsed), listOf(group)).processServing()

        // list order wins: the driver earlier in the list is picked even though its id (9) is higher
        // than a plain, never-assigned driver would get if the pool searched by id instead
        assertEquals(DriverState.WAITING, alreadyUsed.state, "first IDLE driver in list order is picked")
        assertEquals(DriverState.IDLE, neverUsed.state, "the later driver in the list is left untouched")
        assertTrue(neverUsed.id == null, "never having been picked, it still has no id")
    }

    @Test
    fun `a driver already carrying an order keeps it across ticks instead of a newly freed idle driver stealing it`() {
        val group = deliveryGroup(1)
        val carrying = Driver().apply {
            id = 1
            state = DriverState.WAITING
            targetGroup = group
            currentOrder = group.currentOrder
        }
        // a second driver that only becomes idle on the later tick (e.g. just returned from another
        // delivery); it must not displace the driver already locked to this order
        val laterIdle = Driver().apply {
            id = 2
            state = DriverState.IDLE
        }
        val waiter = Waiter().apply { id = 1 }
        val processor = serving(listOf(waiter), listOf(carrying, laterIdle), listOf(group))

        processor.processServing() // tick 1: carrying already has the order, nothing to (re)assign
        waiter.resetActionLoads()
        processor.processServing() // tick 2: the group's order is still with the same driver

        assertTrue(carrying.targetGroup === group, "the original driver keeps the group across ticks")
        assertEquals(DriverState.IDLE, laterIdle.state, "the idle driver was never touched")
    }
}
