package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs

/**
 * A CASUAL group of 7 on COMMON tables. No table matches 7 exactly.
 */
class CasualMergeTrimTieBreakSystemTest : ExampleSystemTestExtension() {
    override val name = "CasualMergeTrimTieBreakSystemTest"
    override val description = "A merge is trimmed by dropping the smallest, lowest id table first"
    override val restaurants = "casualmergetrimtiebreakjson/restaurants.json"
    override val scenario = "casualmergetrimtiebreakjson/scenario.json"
    override val food = "casualmergetrimtiebreakjson/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 6

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 1, listOf(4, 5), 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 4, listOf(1)))

        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        skipUntilString("[IMPORTANT] FOH Seating (R 1): Group 2 seated at table 2 by")
    }
}
