package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val BAKED_POTATO = "baked potato"
private const val POTATO_FRIES = "potato fries"

/**
 * F20/F18: a single TOURNANT cook is occupied by an in-restaurant CASUAL group's order (potato fries,
 * two meals) before it can start the delivery CASUAL group's order (baked potato). This is a corrected,
 * self-contained replacement for [CasualDeliveryTimeoutTest], whose fixture restaurant only had one
 * COMMON table of size 5: a size-1 CASUAL group can never satisfy the three-quarter seating rule on a
 * table that large, so group 1 was always turned away and never queued ahead of the delivery order,
 * meaning the kitchen delay the old test's own comments describe never actually happened (verified by
 * running that fixture: group 1 is sent away with "no table available" and the delivery succeeds on
 * time). Here the table is sized 2 to exactly fit group 1 (size 2), so group 1 is really seated and
 * really orders, and the delay is real:
 *
 * - Group 1 (in-restaurant) and group 2 (delivery) both order in tick 10 (group 2's order tick is
 *   computed per spec adjustment 9: visitingTick 15 - ceil(distance 10 / 5) - 3 = 10).
 * - The cook takes group 1's order first (lower group id), cooks potato fries for 3 ticks (duration 35,
 *   ceil(35/10) - 1 = 3), finishing in tick 13, then starts baked potato in tick 14 and finishes in
 *   tick 17 (3 ticks after being assigned, 7 ticks after group 2's order was taken).
 * - Group 2's delivery patience ends 3 ticks after its visitingTick (spec adjustment 12): tick 18. The
 *   driver is still 1 tick away at that point, so the group gives up in tick 18, and the driver's
 *   delivery attempt in tick 19 fails outright (spec: "All following delivery attempts of this order
 *   will fail"), instead of silently being dropped.
 *
 * All ticks below were cross-checked by running this fixture against our own jar.
 */
class DeliveryQueueDelayTimeoutSystemTest : ExampleSystemTestExtension() {
    override val name = "DeliveryQueueDelayTimeoutSystemTest"
    override val description = "A kitchen queue delay from an earlier order makes a delivery order time out"

    override val restaurants = "deliveryqueuedelayjson/restaurants.json"
    override val food = "deliveryqueuedelayjson/food.json"
    override val scenario = "deliveryqueuedelayjson/scenario.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 20

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(POTATO_FRIES to 2), 1))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 2,
                orderId = 2,
                dishes = mapOf(BAKED_POTATO to 1),
                waitstaffId = null
            )
        )

        // the cook is busy with group 1's order first: group 2's dish is only assigned once it is done
        skipUntilString(
            KitchenTestLogs.kitchenAssign(1, 1, "TOURNANT", 2, POTATO_FRIES, baseOrderId = 1, allOrders = listOf(1))
        )
        skipUntilString(TickStatusTestLogs.tickStart(13, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 1, 2, POTATO_FRIES, 3))

        skipUntilString(TickStatusTestLogs.tickStart(14, 1))
        skipUntilString(
            KitchenTestLogs.kitchenAssign(1, 1, "TOURNANT", 1, BAKED_POTATO, baseOrderId = 2, allOrders = listOf(2))
        )

        skipUntilString(TickStatusTestLogs.tickStart(17, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 1, 1, BAKED_POTATO, 7))
        skipUntilString(FohServiceTestLogs.deliveryHandover(1, 1, mapOf(BAKED_POTATO to 1), 1, 2))
        skipUntilString(DeliveryTestLogs.deliveryPrep(restId = 1, driverId = 1, orderId = 2, groupId = 2, ticks = 2))

        // the group gives up one tick before the driver would have reached them
        skipUntilString(TickStatusTestLogs.tickStart(18, 1))
        skipUntilString(DeliveryTestLogs.deliveryGivenUp(restId = 1, groupId = 2, orderId = 2))
        skipUntilString(FohServiceTestLogs.rating(restId = 1, groupId = 2, rating = "NEGATIVE", pos = 10, neg = 1))

        // the driver still arrives the next tick, but the delivery now fails outright rather than
        // being silently dropped: "all following delivery attempts of this order will fail"
        skipUntilString(TickStatusTestLogs.tickStart(19, 1))
        skipUntilString(DeliveryTestLogs.deliveryArrival(restId = 1, driverId = 1, groupId = 2, orderId = 2))
        assertNextLine(DeliveryTestLogs.deliveryFailed(restId = 1, driverId = 1, orderId = 2, groupId = 2))
    }
}
