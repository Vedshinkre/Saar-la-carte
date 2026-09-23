package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val QUICK_BITE = "Quick Bite"
private const val FAMILY_FEAST = "Family Feast"

/**
 * Spec adjustment #7 (delivery orders, Sep 3 2026): "a waiter can deliver meals for one or multiple
 * orders, even if the waiter cannot deliver the whole order. Therefore, multiple waiters may need to
 * deliver meals for a single order to a driver." No existing test forces one delivery order to need
 * more than one waiter's SERVING capacity in the same tick - the closest,
 * `DeliveryMultiOrderWaiterCapacityTest`, shares one waiter across two *separate* orders/drivers, not
 * two waiters for the *same* order/driver.
 *
 * Group 1 (dine-in, size 4) is served first and uses 4 of waiter 1's 10 SERVING capacity. Group 2's
 * delivery order (size 10, the maximum a single group can place) becomes ready the same tick: waiter
 * 1 only has 6 capacity left, so it hands over 6 dishes, and waiter 2 must pick up the remaining 4 -
 * both to the SAME driver, for the SAME order. Only once all 10 are with the driver does it prepare
 * to leave, still in this same tick.
 *
 */
class DeliveryMultipleWaitersServeOneOrderSystemTest : ExampleSystemTestExtension() {
    override val name = "DeliveryMultipleWaitersServeOneOrderSystemTest"
    override val description = "A delivery order too big for one waiter is split across two waiters to a driver"
    override val restaurants = "deliverymultiwaiterjson/restaurants.json"
    override val food = "deliverymultiwaiterjson/food.json"
    override val scenario = "deliverymultiwaiterjson/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.arrival(restId = 1, groupId = 1))
        assertNextLine(FohArrivalTestLogs.seating(restId = 1, groupId = 1, tableId = 1, waitstaffIds = listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf(QUICK_BITE to 4),
                waitstaffId = 1
            )
        )
        skipUntilString(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 2,
                orderId = 2,
                dishes = mapOf(FAMILY_FEAST to 10),
                waitstaffId = null
            )
        )

        skipUntilString(
            KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 4, dishName = QUICK_BITE, ticks = 0)
        )
        assertNextLine(
            KitchenTestLogs.kitchenCooked(restId = 1, cookId = 2, meals = 10, dishName = FAMILY_FEAST, ticks = 0)
        )

        // group 1's dine-in order is served first, using 4 of waiter 1's capacity
        skipUntilString(
            FohServiceTestLogs.serving(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(QUICK_BITE to 4),
                tableId = 1,
                ticks = 0
            )
        )
        // waiter 1's remaining 6 capacity covers 6 of the 10 delivery dishes...
        assertNextLine(
            FohServiceTestLogs.deliveryHandover(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(FAMILY_FEAST to 6),
                driverId = 1,
                orderId = 2
            )
        )
        // ...and waiter 2 hands over the remaining 4, same driver, same order
        assertNextLine(
            FohServiceTestLogs.deliveryHandover(
                restId = 1,
                waitstaffId = 2,
                dishes = mapOf(FAMILY_FEAST to 4),
                driverId = 1,
                orderId = 2
            )
        )

        // only now, with all 10 dishes finally with the driver, does it prepare to leave
        skipUntilString(DeliveryTestLogs.deliveryPrep(restId = 1, driverId = 1, orderId = 2, groupId = 2, ticks = 1))
    }
}
