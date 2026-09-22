package deliveryhandoverandreturntests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * F20: two delivery orders competing for hand-over in the same tick, sharing one waiter whose
 * remaining capacity is not enough for both. `ServingProcessor.serveDeliveryGroups` iterates the
 * ready groups sorted by ascending order id ("readyGroups... sortedBy { it.currentOrder?.id ...")
 * and hands each one to `serveToDriver`, which spends the shared waiter's capacity batch by batch
 * ("assignWaiterForDelivery" is called again for every batch, not reserved per group up front).
 * So the lower-id order is expected to exhaust the waiter's capacity before the higher-id order gets
 * anything, even though both orders already have their own idle driver assigned in the same tick
 * (via `getOrAssignDriver`, which does not check waiter capacity before assigning a driver).
 *
 * This is new coverage: the existing `DeliveryHandOverTest.meals no waiter could hand over this tick
 * follow in the next tick to the same driver` test only has ONE driver and a SECOND group that never
 * gets a driver at all (because the only driver is busy), so it never shows capacity being split
 * across two orders that both already have their own driver in the same tick.
 */
class DeliveryMultiOrderWaiterCapacityTest {
    private lateinit var fx: DeliveryFixtures

    @BeforeEach
    fun setup() {
        fx = DeliveryFixtures()
    }

    private fun waiterWithFreeCapacity(id: Int, freeSlots: Int) = de.unisaarland.cs.se.selab.actors.Waiter().apply {
        this.id = id
        addToTickLoad(ActionType.SERVE, Constants.ACTION_LIMIT - freeSlots)
    }

    @Test
    fun `the lower order id spends the shared waiter's capacity before the higher order id gets any`() {
        // order ids follow creation order, so the group created first (id 1) gets the lower order id
        val first = fx.deliveryGroup(id = 1, dishCount = 4, status = DishStatus.COOKED)
        val second = fx.deliveryGroup(id = 2, dishCount = 4, status = DishStatus.COOKED)
        val onlyWaiter = waiterWithFreeCapacity(1, freeSlots = 6) // enough for the first order, only half the second
        val driverForFirst = Driver()
        val driverForSecond = Driver()

        fx.serving(listOf(onlyWaiter), listOf(driverForFirst, driverForSecond), listOf(first, second))
            .processServing()

        // the first order is fully handed over
        assertTrue(first.currentOrder!!.dishes.all { it.status == DishStatus.SERVED })
        assertEquals(DriverState.WAITING, driverForFirst.state)

        // the second order already has its own driver (assignment does not check waiter capacity),
        // but only 2 of its 4 dishes could be handed over with the capacity left after the first order
        assertEquals(DriverState.WAITING, driverForSecond.state, "a driver is assigned as soon as it is ready")
        val secondDishes = second.currentOrder!!.dishes
        assertEquals(2, secondDishes.count { it.status == DishStatus.SERVED })
        assertEquals(2, secondDishes.count { it.status == DishStatus.COOKED })

        // the waiter's tick load is exactly exhausted: 4 for the first order, 2 for the second
        assertEquals(Constants.ACTION_LIMIT, onlyWaiter.getTickLoad(ActionType.SERVE))

        // two separate "FOH Delivery" log lines: one per order, driver 1 then driver 2
        val lines = fx.logLinesContaining("FOH Delivery")
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("driver 1"), lines[0])
        assertTrue(lines[1].contains("driver 2"), lines[1])
    }

    @Test
    fun `the second order's remaining dishes are handed over next tick once capacity frees up`() {
        val first = fx.deliveryGroup(id = 1, dishCount = 4, status = DishStatus.COOKED)
        val second = fx.deliveryGroup(id = 2, dishCount = 4, status = DishStatus.COOKED)
        val onlyWaiter = waiterWithFreeCapacity(1, freeSlots = 6)
        val driverForFirst = Driver()
        val driverForSecond = Driver()
        val serving = fx.serving(listOf(onlyWaiter), listOf(driverForFirst, driverForSecond), listOf(first, second))

        serving.processServing()
        onlyWaiter.resetActionLoads()
        serving.processServing()

        assertTrue(second.currentOrder!!.dishes.all { it.status == DishStatus.SERVED })
        // the same driver keeps carrying the second order across the two ticks
        assertTrue(driverForSecond.currentOrder === second.currentOrder)
    }
}
