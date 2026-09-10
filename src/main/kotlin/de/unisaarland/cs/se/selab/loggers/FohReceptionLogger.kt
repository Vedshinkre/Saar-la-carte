package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.enums.LogLevel

object FohReceptionLogger {

    fun logRestaurantArrival(groupId: Int) {
        Logger.log(
            LogLevel.INFO,
            "Restaurant Arrival (R ${Logger.restaurantID}): Group $groupId " +
                "arrived at restaurant ${Logger.restaurantID}."
        )
    }

    fun logFohSeating(
        groupId: Int,
        tableId: Int,
        waitstaffIds: List<Int>
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Seating (R ${Logger.restaurantID}): Group $groupId seated at " +
                "table $tableId by waitstaff $waitstaffIds."
        )
    }

    fun logFohMergingTables(
        groupId: Int,
        oldTableIds: List<Int>,
        mergedTableId: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "FOH Merging Tables (R ${Logger.restaurantID}): For group $groupId " +
                "the tables $oldTableIds were merged into $mergedTableId."
        )
    }

    fun logFohNoSeatingNoWaitstaff(groupId: Int) {
        Logger.log(
            LogLevel.INFO,
            "FOH No Seating (R ${Logger.restaurantID}): No free waitstaff " +
                "available for group $groupId."
        )
    }

    fun logFohNoSeating(
        groupId: Int,
        waitstaffId: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "FOH No Seating (R ${Logger.restaurantID}): Assigned waitstaff " +
                "$waitstaffId but no table available, group $groupId is sent away."
        )
    }

    fun logFohOrdering(
        groupId: Int,
        orderId: Int,
        dishNameToAmount: Map<String, Int>,
        waitstaffId: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Ordering (R ${Logger.restaurantID}): Group $groupId placed " +
                "order $orderId of $dishNameToAmount with waitstaff $waitstaffId."
        )
    }

    fun logFohNoOrdering(
        groupId: Int,
        customerNumber: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH No Ordering (R ${Logger.restaurantID}): Group $groupId could " +
                "not place an order for $customerNumber customers, they leave " +
                "the restaurant."
        )
    }

    fun logSeatingStatus(
        waitstaffNumber: Int,
        customerNumber: Int,
        tableNumber: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "FOH Seating Status (R ${Logger.restaurantID}): $waitstaffNumber " +
                "waitstaff seated $customerNumber customers on $tableNumber tables."
        )
    }

    fun logOrderingStatus(
        customerNumber: Int,
        tableNumber: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "FOH Ordering Status (R ${Logger.restaurantID}): The restaurant " +
                "received orders from $customerNumber customers, $tableNumber " +
                "waitstaff took orders."
        )
    }
}
