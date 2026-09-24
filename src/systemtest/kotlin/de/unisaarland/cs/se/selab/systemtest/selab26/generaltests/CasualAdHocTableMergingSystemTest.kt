package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A CASUAL group of 5 fits neither of the two 3 seat tables, so the tables are merged at seating
 * time (1 and 2 into 1). CASUAL groups reserve nothing, so unlike REGULAR and EVENT groups their
 * merge can only happen when they are seated.
 *
 * Asserts the whole arrival block of tick 1 line by line: arrival, merge, seating on the merged
 * table and the order. Written as tester of the seating feature (Sep 18).
 */
class CasualAdHocTableMergingSystemTest : ExampleSystemTestExtension() {
    override val name = "CasualAdHocTableMergingSystemTest"
    override val description = "A CASUAL group's ad-hoc seating merges two tables to fit"
    override val restaurants = "casualadhocmergejson/restaurants.json"
    override val scenario = "casualadhocmergejson/scenario.json"
    override val food = "casualadhocmergejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 1, listOf(1, 2), 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("Rice Bowl" to 5), 1))
    }
}
