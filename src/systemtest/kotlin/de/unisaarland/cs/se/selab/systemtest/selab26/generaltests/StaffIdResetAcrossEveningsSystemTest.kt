package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * F17 (FOH - Staff Management): waiter ids are evening-scoped, not permanent.
 */
class StaffIdResetAcrossEveningsSystemTest : ExampleSystemTestExtension() {
    override val name = "StaffIdResetAcrossEveningsSystemTest"
    override val description = "Waiter ids restart from 1 each evening rather than continuing to increment"
    override val restaurants = "staffidresetjson/restaurants.json"
    override val scenario = "staffidresetjson/scenario.json"
    override val food = "staffidresetjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 25

    override suspend fun run() { // Evening 1: groups 1 and 2 (size 6 each) fill one waiter's SEAT tick load (6 + 6 >
        // 10), forcing the second group onto the other waiter - so both configured waiters
        // are used and get ids 1 and 2 this evening.
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 1, 6))
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(2)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 2, 6))

        // Evening 2, tick 1: group 3 is a brand-new visitor (visitingStart 2). Both waiters
        // are freshly reset (id null, current load 0) from the evening-end cleanup, so
        // whichever is picked becomes waitstaff 1 again - never waitstaff 3.
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 1, listOf(1)))
    }
}
