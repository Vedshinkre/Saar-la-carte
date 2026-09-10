package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.enums.LogLevel

object DeliveryLogger {

    fun logDeliveryPreparation(
        driverId: Int,
        orderId: Int,
        groupId: Int,
        ticksRequiredToDeliver: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Preparation (R ${Logger.restaurantID}): Driver $driverId\n" +
                "prepares driving order $orderId to group $groupId, which will take\n" +
                "$ticksRequiredToDeliver ticks."
        )
    }

    fun logDeliveryDriving(
        driverId: Int,
        distanceCovered: Int,
        ticksRequiredToDelivers: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Delivery Driving (R ${Logger.restaurantID}): Driver $driverId drove\n" +
                "$distanceCovered km and needs $ticksRequiredToDelivers more ticks."
        )
    }

    fun logDeliveryArrival(
        driverId: Int,
        groupId: Int,
        orderId: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Arrival (R ${Logger.restaurantID}): Driver $driverId arrived\n" +
                "at group $groupId with order $orderId."
        )
    }

    fun logDeliveryFinished(
        driverId: Int,
        orderId: Int,
        groupId: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Delivery Finished (R ${Logger.restaurantID}): Driver $driverId\n" +
                "gave delivery of order $orderId to group $groupId."
        )
    }

    fun logDeliveryFailed(
        driverId: Int,
        orderId: Int,
        groupId: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Delivery Failed (R ${Logger.restaurantID}): Driver $driverId\n" +
                "failed to deliver order $orderId to group $groupId."
        )
    }

    fun logDeliveryGivenUp(
        groupId: Int,
        orderId: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Given Up (R ${Logger.restaurantID}): Group $groupId gave up on\n" +
                "waiting for delivery of order $orderId."
        )
    }

    fun logDeliveryReturned(
        driverId: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Returned (R ${Logger.restaurantID}): Driver $driverId has\n" +
                "returned."
        )
    }

    fun logDeliveryFinishedEating(
        groupId: Int
    ) {
        Logger.log(
            LogLevel.INFO,
            "Delivery Finished Eating (R ${Logger.restaurantID}): Group $groupId has\n" +
                "finished eating."
        )
    }
}
