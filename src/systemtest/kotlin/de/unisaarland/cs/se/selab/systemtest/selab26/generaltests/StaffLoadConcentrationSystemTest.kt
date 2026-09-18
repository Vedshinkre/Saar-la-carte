package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * F17 (FOH - Staff Management): with 2 waiters idle at the start of the tick, the manager's
 * first candidate pool (current load below 10)
 */
class StaffLoadConcentrationSystemTest : ExampleSystemTestExtension() {
    override val name = "StaffLoadConcentrationSystemTest"
    override val description = "Two idle waiters"
    override val restaurants = "staffloadconcentrationjson/restaurants.json"
    override val scenario = "staffloadconcentrationjson/scenario.json"
    override val food = "staffloadconcentrationjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // Group 1 (size 4) is the very first assignment ever, so whichever waiter wins the
        // (irrelevant, both idle) tie becomes waitstaff 1.
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 1, 4))

        // Group 2 (size 4): waitstaff 1 now has current load 4 (< 10) and waitstaff 2 has 0.
        // The manager tops up the busier one first, so group 2 lands on waitstaff 1 again,
        // pushing its SEAT tick load to 8 and current load to 8.
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 2, 4))

        // Group 3 (size 4): waitstaff 1's SEAT tick load (8) + 4 would exceed the action limit
        // of 10, so it is no longer even eligible this tick - despite still being under its
        // current-load cap. Waitstaff 2 (untouched so far) is forced to take it, only now
        // receiving its id.
        assertNextLine(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(2)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 3, 4))

        // Summary: both waiters were used this tick, across all 3 (merge-free) tables.
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 2, 12, 3))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
    }
}
