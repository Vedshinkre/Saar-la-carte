package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingType

/**
 * Handles FOH serving, eating and escorting logs.
 */
object FohServiceLogger {
    /**
     * Logs a dish being served.
     *
     * @param waitstaffId the id of the waiter
     * @param dishNameToAmount the ordered amount per dish name
     * @param tableId the id of the table
     * @param orderDurationTick the number of ticks between the order and the cooked meal
     */
    fun logFohServing(
        waitstaffId: Id,
        dishNameToAmount: Map<String, Int>,
        tableId: Id,
        orderDurationTick: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Serving (R ${Logger.restaurantID}): Waitstaff $waitstaffId " +
                "serves ${Logger.formatKeyValueMap(dishNameToAmount)} to table $tableId " +
                "$orderDurationTick ticks after ordering."
        )
    }

    /**
     * Logs when meals could not be served.
     *
     * @param waitstaffId the id of the waiter
     * @param mealNumber the number of meals
     * @param tableId the id of the table
     */
    fun logFohNoServing(
        waitstaffId: Id,
        mealNumber: Int,
        tableId: Id
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "FOH No Serving (R ${Logger.restaurantID}): Waitstaff $waitstaffId " +
                "did not serve $mealNumber meals to table $tableId."
        )
    }

    /**
     * Logs a delivery being handed to a driver.
     *
     * @param waitstaffId the id of the waiter
     * @param dishNameToAmount the ordered amount per dish name
     * @param driverId the id of the delivery driver
     * @param orderId the id of the order
     */
    fun logFohDelivery(
        waitstaffId: Id,
        dishNameToAmount: Map<String, Int>,
        driverId: Id,
        orderId: Id
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Delivery (R ${Logger.restaurantID}): Waitstaff $waitstaffId " +
                "serves ${Logger.formatKeyValueMap(dishNameToAmount)} meals to driver " +
                "$driverId for order $orderId."
        )
    }

    /**
     * Logs the serving status.
     *
     * @param waitstaffNumber the number of waiters
     * @param mealTotalNumber the total number of meals
     */
    fun logFohServingStatus(
        waitstaffNumber: Int,
        mealTotalNumber: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "FOH Serving Status (R ${Logger.restaurantID}): " +
                "$waitstaffNumber waitstaff served $mealTotalNumber meals."
        )
    }

    /**
     * Logs when customers leave without eating.
     *
     * @param customerNumber the number of customers
     * @param groupId the id of the customer group
     * @param tableId the id of the table
     */
    fun logRestaurantNoEating(
        customerNumber: Int,
        groupId: Id,
        tableId: Id
    ) {
        Logger.log(
            LogLevel.INFO,
            "Restaurant No Eating (R ${Logger.restaurantID}): $customerNumber " +
                "customers of group $groupId leave table $tableId due to not being served."
        )
    }

    /**
     * Logs customers finishing their meal.
     *
     * @param customerNumber the number of customers
     * @param groupId the id of the customer group
     * @param tableId the id of the table
     */
    fun logFohFinishedEating(
        customerNumber: Int,
        groupId: Id,
        tableId: Id
    ) {
        Logger.log(
            LogLevel.INFO,
            "FOH Finished Eating (R ${Logger.restaurantID}): $customerNumber " +
                "customers of group $groupId have finished eating at table $tableId."
        )
    }

    /**
     * Logs the eating status.
     *
     * @param numberOfEatingCustomers the number of eating customers
     * @param numberOfFinishedCustomers the number of finished customers
     */
    fun logFohEatingStatus(
        numberOfEatingCustomers: Int,
        numberOfFinishedCustomers: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "FOH Eating Status (R ${Logger.restaurantID}): " +
                "$numberOfEatingCustomers customers are eating and " +
                "$numberOfFinishedCustomers customers have finished eating this tick."
        )
    }

    /**
     * Logs customers being escorted out.
     *
     * @param waitstaffId the id of the waiter
     * @param numberOfCustomers the number of customers
     * @param groupId the id of the customer group
     * @param tableId the id of the table
     */
    fun logFohEscorting(
        waitstaffId: Id,
        numberOfCustomers: Int,
        groupId: Id,
        tableId: Id
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Escorting (R ${Logger.restaurantID}): Waitstaff $waitstaffId " +
                "escorts $numberOfCustomers customers of group $groupId from table " +
                "$tableId outside."
        )
    }

    /**
     * Logs a customer rating a restaurant.
     *
     * @param groupID the id of the customer group
     * @param groupRating the rating of the group
     * @param positiveRatingQuantity the number of positive ratings so far
     * @param negativeRatingQuantity the number of negative ratings so far
     */
    fun logCustomerRateRestaurant(
        groupID: Id,
        groupRating: RatingType,
        positiveRatingQuantity: Int,
        negativeRatingQuantity: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "Rating (R ${Logger.restaurantID}): Group $groupID rates the restaurant " +
                "${Logger.restaurantID} with $groupRating rating, leading to " +
                "$positiveRatingQuantity positive ratings and " +
                "$negativeRatingQuantity negative ratings."
        )
    }

    /**
     * Logs the rating status.
     *
     * @param numberOfGroupsGivingRatings the number of groups giving ratings
     */
    fun logRatingStatus(
        numberOfGroupsGivingRatings: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Rating Status (R ${Logger.restaurantID}): " +
                "$numberOfGroupsGivingRatings groups performed ratings this tick."
        )
    }

    /**
     * Logs the escorting status.
     *
     * @param waitstaffNumber the number of waiters
     * @param customerEscortingNumber the customer escorting number
     */
    fun logFohEscortingStatus(
        waitstaffNumber: Int,
        customerEscortingNumber: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "FOH Escorting Status (R ${Logger.restaurantID}): " +
                "$waitstaffNumber waitstaff escorted " +
                "$customerEscortingNumber customers this tick."
        )
    }
}
