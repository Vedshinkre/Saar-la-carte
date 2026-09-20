package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec 2.2 "Front of house", table reservation steps 4 and 6 (merging). Each restaurant hosts one
 * REGULAR group of its own id, all COMMON tables.
 *
 * - R1: the merged table operates on the lowest id, which is not the id of the smallest table
 * - R2: three tables are merged first, then the smallest one is dropped again while the size holds
 * - R3: of two equally small tables only the lowest id is dropped in the trimming
 * - R4: no single table fits and the merge is above three quarters, so step 6 lifts the limit
 * - R5: step 5 (single table without limit) is tried before step 6 (merge without limit)
 * - R6: the tables are merged from the smallest to the largest, not in id order
 * - R7: five tables are merged, the merge takes the lowest ids whatever the order in the file
 * - R8: the merge covers exactly three quarters of the merged size, which is still allowed
 * - R9: the merge is just below three quarters, so the single table of 20 is taken instead
 */
class TableReservationMergeSystemTest : TableMergingSystemTest() {
    override val name = "TableReservationMergeSystemTest"
    override val description = "REGULAR reservations that merge tables: order, trimming, ids, three quarters"
    override val restaurants = "tablemerging/reservationmerge/restaurants.json"
    override val scenario = "tablemerging/reservationmerge/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 3

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertSeatedOnMergedTable(1, 1, listOf(1, 3), 1)
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 1, 6, 1))
        assertSeatedOnMergedTable(2, 2, listOf(2, 3), 2)
        assertSeatedOnMergedTable(3, 3, listOf(4, 5), 4)
        assertSeatedOnMergedTable(4, 4, listOf(1, 2), 1)
        assertSeatedOnSingleTable(5, 5, 3)
        assertSeatedOnMergedTable(6, 6, listOf(3, 4), 3)
        assertSeatedOnMergedTable(7, 7, listOf(1, 2, 3, 4, 5), 1)
        skipUntilString(FohArrivalTestLogs.seatingStatus(7, 1, 10, 1))
        assertSeatedOnMergedTable(8, 8, listOf(1, 2), 1)
        assertSeatedOnSingleTable(9, 9, 3)
    }
}
