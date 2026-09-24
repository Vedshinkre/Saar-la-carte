package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
/**
 * Handles delivery logs.
 */
object DeliveryLogger {
    /**
     * Logs a driver preparing a delivery.
     *
     * @param driverId the id of the delivery driver
     * @param orderId the id of the order
     * @param groupId the id of the customer group
     * @param ticksRequiredToDeliver the number of ticks the trip to the customer takes
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
     *
     * @param driverId the id of the delivery driver
     * @param distanceCovered the distance in km the driver has driven so far
     * @param ticksRequiredToDelivers the number of ticks the trip to the customer takes
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
     *
     * @param driverId the id of the delivery driver
     * @param groupId the id of the customer group
     * @param orderId the id of the order
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
     *
     * @param driverId the id of the delivery driver
     * @param orderId the id of the order
     * @param groupId the id of the customer group
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
     *
     * @param driverId the id of the delivery driver
     * @param orderId the id of the order
     * @param groupId the id of the customer group
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
     *
     * @param groupId the id of the customer group
     * @param orderId the id of the order
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
     *
     * @param driverId the id of the delivery driver
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
     *
     * @param groupId the id of the customer group
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
