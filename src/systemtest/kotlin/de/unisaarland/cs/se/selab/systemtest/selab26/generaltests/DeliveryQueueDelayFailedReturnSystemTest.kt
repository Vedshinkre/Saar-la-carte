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
 * F20: a failed delivery attempt still drives back over the same distance it drove out, exactly as
 * `deliveryhandoverandreturntests.DeliveryReturnTripTest`'s "a failed delivery is driven back over
 * the same distance" unit test already confirms at the component level. This system test checks the
 * same rule end to end against a real scenario file, continuing the exact timeline that
 * [DeliveryQueueDelayTimeoutSystemTest] already establishes and cross-checked against our own jar:
 *
 * - Driver 1 is prepared in tick 17 for a 2-tick one-way trip to group 2 (`ticks = 2` in the
 *   preparation log there), drives ticks 18 and 19, and fails on arrival in tick 19 because group 2
 *   already gave up in tick 18.
 * - `Driver.handOverToCustomer` calls `startReturnTrip()` on every hand-over attempt, success or
 *   failure, so the driver immediately turns around in the same tick 19 it fails in, with
 *   `ticksToDest = totalTripTicks / 2 = (2 * 2) / 2 = 2` (the same 2 ticks the outbound trip took).
 * - `Driver.driveTowardsRestaurant` is not logged per tick (only the outbound leg is), so the two
 *   return ticks (20 and 21) are silent, and "Delivery Returned" is the next delivery log line,
 *   logged in tick 21 once `finishReturnTrip()` runs.
 *
 */
class DeliveryQueueDelayFailedReturnSystemTest : ExampleSystemTestExtension() {
    override val name = "DeliveryQueueDelayFailedReturnSystemTest"
    override val description = "A failed delivery attempt still returns after the same number of ticks it drove out"

    override val restaurants = "deliveryqueuedelayjson/restaurants.json"
    override val food = "deliveryqueuedelayjson/food.json"
    override val scenario = "deliveryqueuedelayjson/scenario.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        // Re-establish the same timeline DeliveryQueueDelayTimeoutSystemTest already verified, up to
        // the failed arrival in tick 19 - see its own docstring for the full derivation of these ticks.
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
        skipUntilString(TickStatusTestLogs.tickStart(18, 1))
        skipUntilString(DeliveryTestLogs.deliveryGivenUp(restId = 1, groupId = 2, orderId = 2))
        skipUntilString(TickStatusTestLogs.tickStart(19, 1))
        skipUntilString(DeliveryTestLogs.deliveryArrival(restId = 1, driverId = 1, groupId = 2, orderId = 2))
        assertNextLine(DeliveryTestLogs.deliveryFailed(restId = 1, driverId = 1, orderId = 2, groupId = 2))

        // New: the return trip after the failure. Two silent driving ticks (20, 21), then the driver
        // is back, exactly as long as its 2-tick outbound trip took. skipUntilString (rather than
        // assertNextLine right after the tick 21 marker) avoids assuming exactly which other per-tick
        // log lines (restaurant status, decisions) precede it, since only the delivery timeline itself
        // was cross-checked against our jar for this fixture.
        skipUntilString(TickStatusTestLogs.tickStart(21, 1))
        skipUntilString(DeliveryTestLogs.deliveryReturned(restId = 1, driverId = 1))
        // and not any later than tick 21 either: the very next tick marker after it is tick 22
        skipUntilString(TickStatusTestLogs.tickStart(22, 1))
    }
}
