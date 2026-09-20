package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec 2.2 "Front of house": the table of a CASUAL group is created ad-hoc with the reservation
 * rules 1-4 only, so unlike for REGULAR and EVENT groups the three quarter limit is never lifted,
 * and BAR tables are not merged. Every restaurant has its own type so that each CASUAL group picks
 * its own restaurant.
 *
 * - R1: a single guest is sent away from the tables of 2 and 10 (1 < 3/4 of 2), this does not use
 *   up a table: a group of 2 later takes table 1
 * - R2: a BAR group of 4 is sent away from two BAR tables of 2 (BAR is never merged), while a
 *   group of 3 on a table of 4 sits exactly on the three quarter limit and is seated
 * - R3: a group of 5 merges tables 2 (2) and 3 (3), ignoring the bigger table 1 with the lower id
 * - R4: a group of 10 is sent away from two tables of 7, the merge (14) is below three quarters
 */
class CasualTableNoLiftSystemTest : TableMergingSystemTest() {
    override val name = "CasualTableNoLiftSystemTest"
    override val description = "CASUAL groups never get the three quarter limit lifted and BAR is not merged"
    override val restaurants = "tablemerging/casualnolift/restaurants.json"
    override val scenario = "tablemerging/casualnolift/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 6

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertArrivedButNoTable(1, 1)
        assertArrivedButNoTable(2, 2)
        assertSeatedOnMergedTable(3, 3, listOf(2, 3), 2)
        assertArrivedButNoTable(4, 4)

        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        assertSeatedOnSingleTable(1, 5, 1)

        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        assertSeatedOnSingleTable(2, 7, 3)
    }
}
