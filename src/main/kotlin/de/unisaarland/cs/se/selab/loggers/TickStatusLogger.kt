package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time.evening
import de.unisaarland.cs.se.selab.Time.tick
import de.unisaarland.cs.se.selab.enums.LogLevel
/**
 * Handles simulation and tick status logs.
 */
object TickStatusLogger {
    /**
     * Logs the start of the serving phase.
     */
    fun logServingStart() {
        Logger.log(
            LogLevel.IMPORTANT,
            "Serving: Serving of evening $evening starts."
        )
    }

    /**
     * Logs the current tick.
     */
    fun logCurrentTick() {
        Logger.log(
            LogLevel.IMPORTANT,
            "Simulation: Tick $tick started."
        )
    }

    /**
     * Logs a group's restaurant decision.
     */
    fun logRestaurantDecision(
        groupId: Id,
        restId: Id
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Restaurant Decision: Group $groupId decided on restaurant $restId."
        )
    }

    /**
     * Logs when a group cannot choose a restaurant.
     */
    fun logRestaurantNoDecision(
        groupId: Id
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Restaurant No Decision: Group $groupId could not decide for a restaurant."
        )
    }

    /**
     * Logs the start of a restaurant tick.
     */
    fun logRestaurantStart() {
        Logger.log(
            LogLevel.DEBUG,
            "Restaurant Start (R ${Logger.restaurantID}): Restaurant ${Logger.restaurantID} simulates a tick."
        )
    }

    /**
     * Logs the end of a restaurant tick.
     */
    fun logRestaurantEnd() {
        Logger.log(
            LogLevel.DEBUG,
            "Restaurant End (R ${Logger.restaurantID}): Restaurant ${Logger.restaurantID} finished simulating the tick."
        )
    }

    /**
     * Logs the end of serving.
     */
    fun logServingEnd() {
        Logger.log(
            LogLevel.IMPORTANT,
            "Serving: Serving of evening $evening ends."
        )
    }
}
