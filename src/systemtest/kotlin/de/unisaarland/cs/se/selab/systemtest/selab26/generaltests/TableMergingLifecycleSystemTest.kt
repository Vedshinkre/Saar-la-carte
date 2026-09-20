package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec 2.2 "Front of house" and "End of the opening time": merged tables are separated again once a
 * CASUAL group has left, tables reserved for a REGULAR group stay reserved after it has left, and
 * all reservations and merges are discarded for the next evening. Four tables of 2.
 *
 * - evening 1, tick 2: REGULAR group 1 (4) merges the reserved tables 1 and 2, CASUAL group 2 (4)
 *   merges the tables 3 and 4 ad-hoc
 * - evening 1, tick 14: both groups have left. Groups 3 and 4 (2 each) get the separated tables 3
 *   and 4, they could not if the tables were still merged (2 < 3/4 of 4) and they skip the
 *   tables 1 and 2 that are still reserved for the REGULAR group, although it has left
 * - evening 2, tick 2: the same merges happen again
 */
class TableMergingLifecycleSystemTest : TableMergingSystemTest() {
    override val name = "TableMergingLifecycleSystemTest"
    override val description = "Merged tables are separated again after leaving and across evenings"
    override val restaurants = "tablemerging/lifecycle/restaurants.json"
    override val scenario = "tablemerging/lifecycle/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 30

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertSeatedOnMergedTable(1, 1, listOf(1, 2), 1)
        assertSeatedOnMergedTable(1, 2, listOf(3, 4), 3)

        skipUntilString(TickStatusTestLogs.tickStart(14, 1))
        assertSeatedOnSingleTable(1, 3, 3)
        assertSeatedOnSingleTable(1, 4, 4)

        skipUntilString(TickStatusTestLogs.tickStart(2, 2))
        assertSeatedOnMergedTable(1, 1, listOf(1, 2), 1)
        assertSeatedOnMergedTable(1, 2, listOf(3, 4), 3)
    }
}
