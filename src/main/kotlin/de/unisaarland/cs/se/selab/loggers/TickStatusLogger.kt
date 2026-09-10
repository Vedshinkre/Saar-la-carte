package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Time.evening
import de.unisaarland.cs.se.selab.Time.tick
import de.unisaarland.cs.se.selab.enums.LogLevel

object TickStatusLogger {

    fun logServingStart() {
        Logger.log(
            LogLevel.IMPORTANT,
            "Serving: Serving of evening $evening starts."
        )
    }

    fun logCurrentTick() {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation: Tick $tick started."
        )
    }

    fun logRestaurantDecision(
        groupId: Int,
        restId: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Restaurant Decision: Group $groupId decided on restaurant $restId."
        )
    }

    fun logRestaurantNoDecision(
        groupId: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Restaurant No Decision: Group $groupId could not decide for a restaurant."
        )
    }

    fun logRestaurantStart() {
        Logger.log(
            LogLevel.DEBUG,
            "Restaurant Start (R ${Logger.restaurantID}): Restaurant ${Logger.restaurantID} simulates a tick."
        )
    }

    fun logRestaurantEnd() {
        Logger.log(
            LogLevel.DEBUG,
            "Restaurant End (R ${Logger.restaurantID}): Restaurant ${Logger.restaurantID} finished simulating the tick."
        )
    }

    fun logServingEnd() {
        Logger.log(
            LogLevel.IMPORTANT,
            "Serving: Serving of evening $evening ends."
        )
    }
}
