package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.enums.LogLevel

object StatisticsLogger {
    fun logSimulationStatsCalculated() {
        Logger.log(LogLevel.IMPORTANT, "Simulation Info: Simulation statistics are calculated.")
    }
    fun logSimulationStatsCooked(numberOfCookedMeals: Int) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation Statistics: Restaurant ${Logger.restaurantID} cooked" +
                "$numberOfCookedMeals meals."
        )
    }
    fun logSimulationStatsServed(numberOfCustomersServed: Int) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation Statistics: Restaurant ${Logger.restaurantID} served" +
                "$numberOfCustomersServed customers."
        )
    }
    fun logSimulationStatsDelivered(numberOfCustomersDelivered: Int) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation Statistics: Restaurant ${Logger.restaurantID} delivered" +
                "meals to $numberOfCustomersDelivered customers."
        )
    }
    fun logSimulationStatsRatingsGiven(numberOfCustomersGivingRatings: Int) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation Statistics: Restaurant ${Logger.restaurantID} received" +
                "$numberOfCustomersGivingRatings ratings."
        )
    }
}
