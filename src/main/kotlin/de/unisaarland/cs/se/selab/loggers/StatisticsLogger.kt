package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.enums.LogLevel
/**
 * Handles simulation statistics logs.
 */
object StatisticsLogger {
    /**
     * Logs that statistics were calculated.
     */
    fun logSimulationStatsCalculated() {
        Logger.log(LogLevel.IMPORTANT, "Simulation Info: Simulation statistics are calculated.")
    }

    /**
     * Logs the number of cooked meals.
     *
     * @param numberOfCookedMeals the number of cooked meals
     */
    fun logSimulationStatsCooked(numberOfCookedMeals: Int) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation Statistics: Restaurant ${Logger.restaurantID} cooked" +
                " $numberOfCookedMeals meals."
        )
    }

    /**
     * Logs the number of served customers.
     *
     * @param numberOfCustomersServed the number of customers served
     */
    fun logSimulationStatsServed(numberOfCustomersServed: Int) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation Statistics: Restaurant ${Logger.restaurantID} served" +
                " $numberOfCustomersServed customers."
        )
    }

    /**
     * Logs the number of delivered customers.
     *
     * @param numberOfCustomersDelivered the number of customers delivered
     */
    fun logSimulationStatsDelivered(numberOfCustomersDelivered: Int) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation Statistics: Restaurant ${Logger.restaurantID} delivered" +
                " meals to $numberOfCustomersDelivered customers."
        )
    }

    /**
     * Logs the number of groups that gave ratings.
     *
     * @param numberOfCustomersGivingRatings the number of customers giving ratings
     */
    fun logSimulationStatsRatingsGiven(numberOfCustomersGivingRatings: Int) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation Statistics: Restaurant ${Logger.restaurantID} received" +
                " $numberOfCustomersGivingRatings ratings."
        )
    }
}
