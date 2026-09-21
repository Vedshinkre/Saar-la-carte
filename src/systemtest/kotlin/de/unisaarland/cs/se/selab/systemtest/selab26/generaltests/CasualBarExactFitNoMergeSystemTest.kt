package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A CASUAL group of 10 wants a BAR table. The restaurant has the BAR tables 1 (5), 2 (5) and
 * 3 (10). The group must be seated directly on table 3 (exact fit) and the tables 1 and 2 must
 * not be merged, even though they have the lower ids.
 */
class CasualBarExactFitNoMergeSystemTest : TableMergingSystemTest() {
    override val name = "CasualBarExactFitNoMergeSystemTest"
    override val description = "A CASUAL BAR group of 10 sits on the BAR table of 10 without merging"
    override val restaurants = "tablemerging/casualbarexactfit/restaurants.json"
    override val scenario = "tablemerging/casualbarexactfit/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 3

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertSeatedOnSingleTable(1, 1, 3)
    }
}
