package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * P02 (FOH - Event Seating): an EVENT seating only counts as fulfilled if all customers could be
 * seated within the tick, otherwise the group leaves. Each restaurant has 2 waiters, so 20 seats
 * per tick. CASUAL groups arrive after the EVENT seating has been completed.
 *
 * - R1: EVENT group 1 (20) exactly uses the capacity of both waiters and is seated by 1 and 2.
 *   CASUAL group 3 arriving in the same tick finds both waiters at their limit and retries in
 *   tick 3, where the tick loads are reset and the EVENT customers did not add to the current
 *   load, so waiter 1 (lowest id) takes it.
 * - R2: EVENT group 2 (21) is one customer above the capacity and fails. The two waiters did
 *   seat 10 customers each before the failure and keep that tick load, so CASUAL group 4 of the
 *   same tick has no free waitstaff either, and is seated by waiter 1 in tick 3. The failed EVENT
 *   group leaves a negative rating.
 */
class EventSeatingCapacityBoundarySystemTest : ExampleSystemTestExtension() {
    override val name = "EventSeatingCapacityBoundarySystemTest"
    override val description = "EVENT of 20 fits 2 waiters, EVENT of 21 fails and leaves the tick load spent"
    override val restaurants = "eventseating/capacity/restaurants.json"
    override val scenario = "eventseating/capacity/scenario.json"
    override val food = "eventseating/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))

        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1, 2)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 3))

        skipUntilString(FohArrivalTestLogs.arrival(2, 2))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(2, 2))
        skipUntilString(FohArrivalTestLogs.arrival(2, 4))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(2, 4))
        skipUntilString(FohServiceTestLogs.rating(2, 2, "NEGATIVE", 0, 1))

        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.seating(1, 3, 2, listOf(1)))
        skipUntilString(FohArrivalTestLogs.seating(2, 4, 2, listOf(1)))
    }
}
