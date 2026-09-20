package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec 2.2 "Front of house", reservation step 1: occupied or reserved tables are ignored, so there
 * is at most one group per (merged) table, and a group without a table is told immediately.
 *
 * - R1: group 1 (10) is bigger than all tables together (8), no table can be reserved
 * - R2: groups 2 and 3 each reserve one of the two tables of 4, group 4 finds nothing left
 * - R3: group 5 (10) fails and must not block anything, so group 6 (8) merges both tables
 * - R4: groups 7 and 8 reserve merged pairs, group 9 finds no table as the parts are reserved
 */
class TableReservationConflictsSystemTest : TableMergingSystemTest() {
    override val name = "TableReservationConflictsSystemTest"
    override val description = "Reserved tables are not reserved again and failed reservations block nothing"
    override val restaurants = "tablemerging/reservationconflicts/restaurants.json"
    override val scenario = "tablemerging/reservationconflicts/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 3

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        skipUntilString(InitialAndPrepTestLogs.fohNoReserving(1, 1))
        skipUntilString(InitialAndPrepTestLogs.fohNoReserving(2, 4))
        skipUntilString(InitialAndPrepTestLogs.fohNoReserving(3, 5))
        skipUntilString(InitialAndPrepTestLogs.fohNoReserving(4, 9))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertSeatedOnSingleTable(2, 2, 1)
        assertSeatedOnSingleTable(2, 3, 2)
        assertSeatedOnMergedTable(3, 6, listOf(1, 2), 1)
        assertSeatedOnMergedTable(4, 7, listOf(1, 2), 1)
        assertSeatedOnMergedTable(4, 8, listOf(3, 4), 3)
    }
}
