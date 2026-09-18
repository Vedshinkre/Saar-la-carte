package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * F17 (FOH - Staff Management): tick load is tracked per waiter and per action type, and
 * resets every tick - independently of a waiter's (evening-scoped) current load. This test
 * uses 2 waiters, each individually only at current load 6 (well under the action limit of
 * 10), yet BOTH are already ineligible for a further SEATING this tick because their SEAT
 * tick load would exceed 10. This shows the tick-load gate is a genuinely separate resource
 * from current load and that it is enforced across the whole waitstaff, not just a single
 * waiter (unlike [WaitstaffExhaustionTest], which only has one waiter to exhaust). The
 * blocked group is not turned away yet (still its first attempt this evening) and, once tick
 * loads reset next tick, retries and succeeds - again on waitstaff 1, since a tie on current
 * load (6 vs 6) resolves to the lowest id.
 */
class StaffMultiWaiterExhaustionSystemTest : ExampleSystemTestExtension() {
    override val name = "StaffMultiWaiterExhaustionSystemTest"
    override val description =
        "Both of 2 waiters hit their per-tick SEAT limit despite low current load; retry next tick"
    override val restaurants = "staffmultiwaiterexhaustionjson/restaurants.json"
    override val scenario = "staffmultiwaiterexhaustionjson/scenario.json"
    override val food = "staffmultiwaiterexhaustionjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // Group 1 (size 6): first-ever assignment, becomes waitstaff 1.
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 1, 6))

        // Group 2 (size 6): waitstaff 1's SEAT tick load (6) + 6 > 10, so only waitstaff 2 is
        // eligible - it is forced to take group 2 even though waitstaff 1's current load (6)
        // would otherwise have made it the "busier, top it up first" choice.
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(2)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 2, 6))

        // Group 3 (size 6): now BOTH waiters have a SEAT tick load of 6, so 6 + 6 > 10 for
        // both. Neither is eligible this tick, even though both have current load only 6 -
        // well below the action limit. No free waitstaff, despite two waiters existing.
        assertNextLine(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 3))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 2, 12, 2))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        // Next tick, SEAT tick loads reset to 0 for both waiters, so group 3 retries and
        // succeeds. Current load is now tied at 6 for both waiters, so the tie is broken by
        // the lowest id - waitstaff 1 again.
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(1)))
    }
}
