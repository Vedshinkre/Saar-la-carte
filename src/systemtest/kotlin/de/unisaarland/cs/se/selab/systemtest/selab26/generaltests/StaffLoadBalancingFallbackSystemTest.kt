package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * F17 (FOH - Staff Management): when every waiter's current load has reached the action
 * limit (10),
 */
class StaffLoadBalancingFallbackSystemTest : ExampleSystemTestExtension() {
    override val name = "StaffLoadBalancingFallbackSystemTest"
    override val description = "SEATING still succeeds via the fallback "
    override val restaurants = "staffloadbalancingfallbackjson/restaurants.json"
    override val scenario = "staffloadbalancingfallbackjson/scenario.json"
    override val food = "staffloadbalancingfallbackjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() { // Groups 1 and 2 each saturate one waiter to current load 10 and successfully order;
        // covered here only far enough to establish who has which waiter, then skipped past
        // their (multi-tick) cooking/serving/eating - not this test's concern.
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("Rice Bowl" to 10), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 2, mapOf("Rice Bowl" to 10), 2))

        // Tick 5: groups 1 and 2 are still eating (they only finish and get escorted in tick
        // 6), so both waiters still show current load 10. Group 3 (size 2) arrives and is
        // still seated successfully - the fallback pool contains both waiters (tied at
        // current load 10), and the tie is broken by the lowest id.
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 3, 3, mapOf("Rice Bowl" to 2), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 2, 1))
    }
}
