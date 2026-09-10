package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
/**
 * Handles FOH serving, eating and escorting logs.
 */
object FohServiceLogger {
    /**
     * Logs a dish being served.
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
                "serves $dishNameToAmount meals to table $tableId " +
                "$orderDurationTick ticks after ordering."
        )
    }

    /**
     * Logs when meals could not be served.
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
                "serves $dishNameToAmount meals to driver $driverId for order $orderId."
        )
    }

    /**
     * Logs the serving status.
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
     */
    fun logCustomerRateRestaurant(
        groupID: Id,
        groupRating: Int,
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
