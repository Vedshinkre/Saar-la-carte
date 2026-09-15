package de.unisaarland.cs.se.selab.systemtest.selab26.utils

/**
 * delivery logs
 */
object DeliveryTestLogs {
    fun deliveryPrep(restId: Int, driverId: Int, orderId: Int, groupId: Int, ticks: Int) =
        "[INFO] Delivery Preparation (R $restId): Driver $driverId prepares driving order $orderId to group " +
            "$groupId, which will take $ticks ticks."

    fun deliveryDriving(restId: Int, driverId: Int, distance: Int, ticks: Int) =
        "[DEBUG] Delivery Driving (R $restId): Driver $driverId drove $distance km and needs $ticks more ticks."

    fun deliveryArrival(restId: Int, driverId: Int, groupId: Int, orderId: Int) =
        "[INFO] Delivery Arrival (R $restId): Driver $driverId arrived at group $groupId with order $orderId."

    fun deliveryFinished(restId: Int, driverId: Int, orderId: Int, groupId: Int) =
        "[IMPORTANT] Delivery Finished (R $restId): Driver $driverId gave delivery of order $orderId to group $groupId."

    fun deliveryFailed(restId: Int, driverId: Int, orderId: Int, groupId: Int) =
        "[IMPORTANT] Delivery Failed (R $restId): Driver $driverId failed to deliver order $orderId to group $groupId."

    fun deliveryGivenUp(restId: Int, groupId: Int, orderId: Int) =
        "[INFO] Delivery Given Up (R $restId): Group $groupId gave up on waiting for delivery of order $orderId."

    fun deliveryReturned(restId: Int, driverId: Int) =
        "[INFO] Delivery Returned (R $restId): Driver $driverId has returned."

    fun deliveryFinishedEating(restId: Int, groupId: Int) =
        "[INFO] Delivery Finished Eating (R $restId): Group $groupId has finished eating."
}
