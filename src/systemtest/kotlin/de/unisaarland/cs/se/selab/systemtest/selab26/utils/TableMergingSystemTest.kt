package de.unisaarland.cs.se.selab.systemtest.selab26.utils

import de.unisaarland.cs.se.selab.systemtest.api.SystemTestAssertionError

/**
 * Base class of the table merging system tests.
 *
 * The tests only care about *which tables* a group is seated on, not about which waiter
 * seated it, so the seating checks ignore the trailing waitstaff ids.
 */
abstract class TableMergingSystemTest : ExampleSystemTestExtension() {

    /** The group arrives and is seated on the single, unmerged table [tableId]. */
    protected suspend fun assertSeatedOnSingleTable(restId: Int, groupId: Int, tableId: Int) {
        skipUntilString(FohArrivalTestLogs.arrival(restId, groupId))
        assertNextSeating(restId, groupId, tableId)
    }

    /** The group arrives, its [oldTableIds] are merged into [mergedId] and it is seated there. */
    protected suspend fun assertSeatedOnMergedTable(
        restId: Int,
        groupId: Int,
        oldTableIds: List<Int>,
        mergedId: Int
    ) {
        skipUntilString(FohArrivalTestLogs.arrival(restId, groupId))
        assertNextLine(FohArrivalTestLogs.mergingTables(restId, groupId, oldTableIds, mergedId))
        assertNextSeating(restId, groupId, mergedId)
    }

    /** The group arrives, a waiter is assigned but no table can be found, so the group is sent away. */
    protected suspend fun assertArrivedButNoTable(restId: Int, groupId: Int) {
        skipUntilString(FohArrivalTestLogs.arrival(restId, groupId))
        val prefix = "[INFO] FOH No Seating (R $restId): Assigned waitstaff "
        val suffix = " but no table available, group $groupId is sent away."
        val line = nextLine()
        if (!line.startsWith(prefix) || !line.endsWith(suffix)) {
            throw SystemTestAssertionError("Expected '$prefix<id>$suffix' but got '$line'")
        }
    }

    /** No reservation of the preparation phase of [evening] may fail. */
    protected suspend fun assertNoReservationFailsInPreparation(evening: Int) {
        skipUntilString(InitialAndPrepTestLogs.prepStart(evening))
        val servingStart = TickStatusTestLogs.servingStart(evening)
        var line = nextLine()
        while (line != servingStart) {
            if (line.startsWith("[IMPORTANT] FOH No Reserving")) {
                throw SystemTestAssertionError("No reservation should fail but got '$line'")
            }
            line = nextLine()
        }
    }

    private suspend fun assertNextSeating(restId: Int, groupId: Int, tableId: Int) {
        val prefix = "[IMPORTANT] FOH Seating (R $restId): Group $groupId seated at table $tableId by waitstaff "
        val line = nextLine()
        if (!line.startsWith(prefix)) {
            throw SystemTestAssertionError("Expected a line starting with '$prefix' but got '$line'")
        }
    }

    private suspend fun nextLine(): String =
        getNextLine() ?: throw SystemTestAssertionError("End of log reached when there should be more.")
}
