package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec 2.2 "Front of house", table reservation steps 2, 3 and 5 (no merging involved) and the
 * tie-break "smallest size, then lowest id". Each restaurant hosts one REGULAR group of its own id.
 *
 * - R1: a group of 6 has an exact fit on table 2 although table 1 (size 8) would also accept it
 * - R2: a group of 6 on a table of 8 sits exactly on the three quarter limit, which is still allowed,
 *   so step 3 takes table 1 before step 4 could merge the two tables of 3
 * - R3: a group of 5 on a table of 8 is just below three quarters, so step 5 lifts the limit
 * - R4: among the eligible tables 1 (8), 2 (7) and 3 (7) the smallest size wins before the lowest id
 * - R5: among equally sized tables the lowest id wins, regardless of the order in the file
 * - R6: no table reaches three quarters, so step 5 picks the smallest, then lowest id table
 * - R7: a group of 1 fits a table of 2 only once the three quarter limit is lifted
 */
class TableReservationSingleTableSystemTest : TableMergingSystemTest() {
    override val name = "TableReservationSingleTableSystemTest"
    override val description = "REGULAR reservations on a single table: exact fit, three quarters, tie-breaks"
    override val restaurants = "tablemerging/reservationsingle/restaurants.json"
    override val scenario = "tablemerging/reservationsingle/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 3

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertSeatedOnSingleTable(1, 1, 2)
        assertSeatedOnSingleTable(2, 2, 1)
        assertSeatedOnSingleTable(3, 3, 1)
        assertSeatedOnSingleTable(4, 4, 2)
        assertSeatedOnSingleTable(5, 5, 3)
        assertSeatedOnSingleTable(6, 6, 2)
        assertSeatedOnSingleTable(7, 7, 1)
    }
}
