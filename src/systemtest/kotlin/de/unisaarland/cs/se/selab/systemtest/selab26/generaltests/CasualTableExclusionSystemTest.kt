package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec 2.2 "Front of house", reservation step 1 for ad-hoc CASUAL tables: occupied or reserved
 * tables are ignored. Five tables of 2, table 1 is reserved for the REGULAR group 3 (tick 10).
 *
 * - group 1 (4) merges the tables 2 and 3, skipping the reserved table 1
 * - group 2 (2) is seated on table 4, neither the reserved table 1 nor the occupied parts of the
 *   merged table 2 are candidates
 * - group 4 (2) gets the last free table 5
 * - the REGULAR group finally finds its table 1 still reserved for it
 */
class CasualTableExclusionSystemTest : TableMergingSystemTest() {
    override val name = "CasualTableExclusionSystemTest"
    override val description = "CASUAL groups skip reserved tables and the parts of occupied merged tables"
    override val restaurants = "tablemerging/casualexclusion/restaurants.json"
    override val scenario = "tablemerging/casualexclusion/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 10

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertSeatedOnMergedTable(1, 1, listOf(2, 3), 2)
        assertSeatedOnSingleTable(1, 2, 4)

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertSeatedOnSingleTable(1, 4, 5)

        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        assertSeatedOnSingleTable(1, 3, 1)
    }
}
