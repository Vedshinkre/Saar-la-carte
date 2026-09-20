package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec 2.2 "Front of house": the manager reserves the tables of EVENT groups first and then those
 * of REGULAR groups, using the same merging rules. The events are made on evening 1 for evening 4,
 * every restaurant has its own type so that each event picks its own restaurant.
 *
 * - R1: event of 20 on tables 1 (12), 2 (8) and 3 (6): all three are merged, then the smallest
 *   table 3 is dropped, so tables 1 and 2 remain
 * - R2: event of 12 prefers merging two tables of 6 to the single table of 30
 * - R3: event of 40 merges the tables of 30 and 10 exactly
 * - R4: event (id 20) and REGULAR group (id 4) both need 6 seats, the event reserves first and so
 *   gets table 1 even though its id is higher, the regular group is left with table 2
 */
class EventTableMergingSystemTest : TableMergingSystemTest() {
    override val name = "EventTableMergingSystemTest"
    override val description = "EVENT reservations merge tables and are reserved before REGULAR ones"
    override val restaurants = "tablemerging/eventmerging/restaurants.json"
    override val scenario = "tablemerging/eventmerging/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 100

    override suspend fun run() {
        assertNoReservationFailsInPreparation(4)

        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        assertSeatedOnMergedTable(1, 1, listOf(1, 2), 1)
        assertSeatedOnMergedTable(2, 2, listOf(2, 3), 2)
        assertSeatedOnMergedTable(3, 3, listOf(1, 2), 1)
        assertSeatedOnSingleTable(4, 20, 1)

        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        assertSeatedOnSingleTable(4, 4, 2)
    }
}
