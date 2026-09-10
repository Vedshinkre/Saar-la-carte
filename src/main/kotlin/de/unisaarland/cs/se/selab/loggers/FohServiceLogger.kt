package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.enums.LogLevel

object FohServiceLogger {

    fun logFohServing(
        waitstaffId: Int,
        dishNameToAmount: Map<String, Int>,
        tableId: Int,
        orderDurationTick: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Serving (R ${Logger.restaurantID}): Waitstaff $waitstaffId " +
                "serves $dishNameToAmount meals to table $tableId " +
                "$orderDurationTick ticks after ordering."
        )
    }

    fun logFohNoServing(
        waitstaffId: Int,
        mealNumber: Int,
        tableId: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "FOH No Serving (R ${Logger.restaurantID}): Waitstaff $waitstaffId " +
                "did not serve $mealNumber meals to table $tableId."
        )
    }

    fun logFohDelivery(
        waitstaffId: Int,
        dishNameToAmount: Map<String, Int>,
        driverId: Int,
        orderId: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Delivery (R ${Logger.restaurantID}): Waitstaff $waitstaffId " +
                "serves $dishNameToAmount meals to driver $driverId for order $orderId."
        )
    }

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

    fun logRestaurantNoEating(
        customerNumber: Int,
        groupId: Int,
        tableId: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "Restaurant No Eating (R ${Logger.restaurantID}): $customerNumber " +
                "customers of group $groupId leave table $tableId due to not being served."
        )
    }

    fun logFohFinishedEating(
        customerNumber: Int,
        groupId: Int,
        tableId: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "FOH Finished Eating (R ${Logger.restaurantID}): $customerNumber " +
                "customers of group $groupId have finished eating at table $tableId."
        )
    }

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

    fun logFohEscorting(
        waitstaffId: Int,
        numberOfCustomers: Int,
        groupId: Int,
        tableId: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH Escorting (R ${Logger.restaurantID}): Waitstaff $waitstaffId " +
                "escorts $numberOfCustomers customers of group $groupId from table " +
                "$tableId outside."
        )
    }

    fun logCustomerRateRestaurant(
        groupID: Int,
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

    fun logRatingStatus(
        numberOfGroupsGivingRatings: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Rating Status (R ${Logger.restaurantID}): " +
                "$numberOfGroupsGivingRatings groups performed ratings this tick."
        )
    }

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
