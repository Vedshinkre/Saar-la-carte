package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
/**
 * Handles FOH arrival, seating, and ordering logs.
 */
object FohReceptionLogger {
    /**
     * Logs a group's arrival.
     *
     * @param groupId the id of the customer group
     */
    fun logRestaurantArrival(groupId: Id) {
        Logger.log(
            LogLevel.INFO,
            "Restaurant Arrival (R ${Logger.restaurantID}): Group $groupId " +
                "arrived at restaurant ${Logger.restaurantID}."
        )
    }

    /**
     * Logs a group being seated.
     *
     * @param groupId the id of the customer group
     * @param tableId the id of the table
     * @param waitstaffIds the ids of the waiters
     */
    fun logFohSeating(
        groupId: Id,
        tableId: Id,
        waitstaffIds: List<Id>
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Seating (R ${Logger.restaurantID}): Group $groupId seated at " +
                "table $tableId by waitstaff ${Logger.formatIds(waitstaffIds)}."
        )
    }

    /**
     * Logs tables being merged.
     *
     * @param groupId the id of the customer group
     * @param oldTableIds the ids of the tables that were merged
     * @param mergedTableId the id of the merged table
     */
    fun logFohMergingTables(
        groupId: Id,
        oldTableIds: List<Id>,
        mergedTableId: Id
    ) {
        Logger.log(
            LogLevel.INFO,
            "FOH Merging Tables (R ${Logger.restaurantID}): For group $groupId " +
                "the tables ${Logger.formatIds(oldTableIds)} were merged into $mergedTableId."
        )
    }

    /**
     * Logs when no waitstaff is available.
     *
     * @param groupId the id of the customer group
     */
    fun logFohNoSeatingNoWaitstaff(groupId: Id) {
        Logger.log(
            LogLevel.INFO,
            "FOH No Seating (R ${Logger.restaurantID}): No free waitstaff " +
                "available for group $groupId."
        )
    }

    /**
     * Logs when no table is available.
     *
     * @param groupId the id of the customer group
     * @param waitstaffId the id of the waiter
     */
    fun logFohNoSeating(
        groupId: Id,
        waitstaffId: Id
    ) {
        Logger.log(
            LogLevel.INFO,
            "FOH No Seating (R ${Logger.restaurantID}): Assigned waitstaff " +
                "$waitstaffId but no table available, group $groupId is sent away."
        )
    }

    /**
     * Logs an order being placed.
     *
     * @param groupId the id of the customer group
     * @param orderId the id of the order
     * @param dishNameToAmount the ordered amount per dish name
     * @param waitstaffId the id of the waiter
     */
    fun logFohOrdering(
        groupId: Id,
        orderId: Id,
        dishNameToAmount: Map<String, Int>,
        waitstaffId: List<Id>?
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Ordering (R ${Logger.restaurantID}): Group $groupId placed " +
                "order $orderId of ${Logger.formatKeyValueMap(dishNameToAmount)}" +
                if (waitstaffId == null) { "." } else {
                    " with waitstaff ${Logger.formatIds(waitstaffId)}."
                }
        )
    }

    /**
     * Logs when a group cannot place an order.
     *
     * @param groupId the id of the customer group
     * @param customerNumber the number of customers
     */
    fun logFohNoOrdering(
        groupId: Id,
        customerNumber: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH No Ordering (R ${Logger.restaurantID}): Group $groupId could " +
                "not place an order for $customerNumber customers, they leave " +
                "the restaurant."
        )
    }

    /**
     * Logs the seating status.
     *
     * @param waitstaffNumber the number of waiters
     * @param customerNumber the number of customers
     * @param tableNumber the number of tables
     */
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

    /**
     * Logs the ordering status.
     *
     * @param customerNumber the number of customers
     * @param waitstaffNumber the number of waiters
     */
    fun logOrderingStatus(
        customerNumber: Int,
        waitstaffNumber: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "FOH Ordering Status (R ${Logger.restaurantID}): The restaurant " +
                "received orders from $customerNumber customers, $waitstaffNumber " +
                "waitstaff took orders."
        )
    }
}
