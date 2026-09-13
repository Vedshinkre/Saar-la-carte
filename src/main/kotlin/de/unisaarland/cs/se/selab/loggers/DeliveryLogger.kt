package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
/**
 * Handles delivery logs.
 */
object DeliveryLogger {
    /**
     * Logs a driver preparing a delivery.
     */
    fun logDeliveryPreparation(
        driverId: Id,
        orderId: Id,
        groupId: Id,
        ticksRequiredToDeliver: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Preparation (R ${Logger.restaurantID}): Driver $driverId" +
                " prepares driving order $orderId to group $groupId, which will take " +
                "$ticksRequiredToDeliver ticks."
        )
    }

    /**
     * Logs a driver during delivery.
     */
    fun logDeliveryDriving(
        driverId: Id,
        distanceCovered: Int,
        ticksRequiredToDelivers: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Delivery Driving (R ${Logger.restaurantID}): Driver $driverId drove" +
                " $distanceCovered km and needs $ticksRequiredToDelivers more ticks."
        )
    }

    /**
     * Logs a driver arriving at a group.
     */
    fun logDeliveryArrival(
        driverId: Id,
        groupId: Id,
        orderId: Id
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Arrival (R ${Logger.restaurantID}): Driver $driverId arrived" +
                " at group $groupId with order $orderId."
        )
    }

    /**
     * Logs a completed delivery.
     */
    fun logDeliveryFinished(
        driverId: Id,
        orderId: Id,
        groupId: Id
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Delivery Finished (R ${Logger.restaurantID}): Driver $driverId" +
                " gave delivery of order $orderId to group $groupId."
        )
    }

    /**
     * Logs a failed delivery.
     */
    fun logDeliveryFailed(
        driverId: Id,
        orderId: Id,
        groupId: Id
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Delivery Failed (R ${Logger.restaurantID}): Driver $driverId" +
                " failed to deliver order $orderId to group $groupId."
        )
    }

    /**
     * Logs when a group gives up waiting.
     */
    fun logDeliveryGivenUp(
        groupId: Id,
        orderId: Id
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Given Up (R ${Logger.restaurantID}): Group $groupId gave up on" +
                " waiting for delivery of order $orderId."
        )
    }

    /**
     * Logs a driver returning.
     */
    fun logDeliveryReturned(
        driverId: Id
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Returned (R ${Logger.restaurantID}): Driver $driverId has" +
                " returned."
        )
    }

    /**
     * Logs a group finishing their delivered meal.
     */
    fun logDeliveryFinishedEating(
        groupId: Id
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Finished Eating (R ${Logger.restaurantID}): Group $groupId has" +
                " finished eating."
        )
    }
}
