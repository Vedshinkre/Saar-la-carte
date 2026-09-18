package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A CASUAL group too big for either single table gets seated by merging both of them
 * ad-hoc, at SEATING time (F16's assignTables), rather than through a pre-existing F14
 * reservation - REGULAR/EVENT groups get their table before the evening starts, but a
 * CASUAL group's table (merged or not) is only decided the moment they're seated.
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
