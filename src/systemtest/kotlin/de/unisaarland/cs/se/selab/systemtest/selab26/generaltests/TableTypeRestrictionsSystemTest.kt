package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec 2.2 "Table": only tables of the same type can be merged and BAR tables never are.
 * Each restaurant hosts one REGULAR group of its own id.
 *
 * - R1: BAR group of 4, but the two BAR tables have 2 seats each and cannot be merged (a big COMMON
 *   table is of the wrong type) so the reservation fails
 * - R2: BAR group of 3 still gets a single BAR table of 4 (three quarters)
 * - R3: COMMON group of 4, but the COMMON, SEPARATED and BAR tables cannot be merged with each
 *   other, so the reservation fails
 * - R4: SEPARATED tables can be merged, the COMMON tables are left alone
 * - R5: COMMON tables are merged although BAR and SEPARATED tables have lower ids
 * - R6: without a tableType the group wants COMMON tables and ignores the exact SEPARATED table
 */
class TableTypeRestrictionsSystemTest : TableMergingSystemTest() {
    override val name = "TableTypeRestrictionsSystemTest"
    override val description = "Only tables of the same type are merged and BAR tables are never merged"
    override val restaurants = "tablemerging/typerestrictions/restaurants.json"
    override val scenario = "tablemerging/typerestrictions/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 3

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        skipUntilString(InitialAndPrepTestLogs.fohNoReserving(1, 1))
        skipUntilString(InitialAndPrepTestLogs.fohNoReserving(3, 3))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertSeatedOnSingleTable(2, 2, 2)
        assertSeatedOnMergedTable(4, 4, listOf(1, 2), 1)
        assertSeatedOnMergedTable(5, 5, listOf(3, 4), 3)
        assertSeatedOnMergedTable(6, 6, listOf(2, 3), 2)
    }
}
