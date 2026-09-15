package de.unisaarland.cs.se.selab.systemtest.selab26.utils

/**
 * arrival, seating and ordering logs
 */
object FohArrivalTestLogs {
    fun arrival(restId: Int, groupId: Int) =
        "[INFO] Restaurant Arrival (R $restId): Group $groupId arrived at restaurant $restId."

    fun seating(restId: Int, groupId: Int, tableId: Int, waitstaffIds: List<Int>) =
        "[IMPORTANT] FOH Seating (R $restId): Group $groupId seated at table $tableId by waitstaff " +
            "${TestLogFormatter.formatIds(waitstaffIds)}."

    fun mergingTables(restId: Int, groupId: Int, oldTableIds: List<Int>, mergedId: Int) =
        "[INFO] FOH Merging Tables (R $restId): For group $groupId the tables " +
            "${TestLogFormatter.formatIds(oldTableIds)} were merged into $mergedId."

    fun noSeatingNoWaitstaff(restId: Int, groupId: Int) =
        "[INFO] FOH No Seating (R $restId): No free waitstaff available for group $groupId."

    fun noSeatingNoTable(restId: Int, waitstaffId: Int, groupId: Int) =
        "[INFO] FOH No Seating (R $restId): Assigned waitstaff $waitstaffId but no table available, " +
            "group $groupId is sent away."

    fun ordering(restId: Int, groupId: Int, orderId: Int, dishes: Map<String, Int>, waitstaffId: Int?) =
        if (waitstaffId != null) {
            "[IMPORTANT] FOH Ordering (R $restId): Group $groupId placed order $orderId of " +
                "${TestLogFormatter.formatMap(dishes)} with waitstaff $waitstaffId."
        } else {
            "[IMPORTANT] FOH Ordering (R $restId): Group $groupId placed order $orderId of " +
                "${TestLogFormatter.formatMap(dishes)}."
        }

    fun noOrdering(restId: Int, groupId: Int, customers: Int) =
        "[IMPORTANT] FOH No Ordering (R $restId): Group $groupId could not place an order for " +
            "$customers customers, they leave the restaurant."

    fun seatingStatus(restId: Int, waitstaff: Int, customers: Int, tables: Int) =
        "[DEBUG] FOH Seating Status (R $restId): $waitstaff waitstaff seated $customers customers on $tables tables."

    fun orderingStatus(restId: Int, customers: Int, waitstaff: Int) =
        "[DEBUG] FOH Ordering Status (R $restId): The restaurant received orders from $customers customers, " +
            "$waitstaff waitstaff took orders."
}
