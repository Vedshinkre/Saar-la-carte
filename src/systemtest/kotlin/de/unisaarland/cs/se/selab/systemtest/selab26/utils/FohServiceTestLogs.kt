package de.unisaarland.cs.se.selab.systemtest.selab26.utils

/**
 * serving, eating and escorting
 */
object FohServiceTestLogs {
    fun serving(restId: Int, waitstaffId: Int, dishes: Map<String, Int>, tableId: Int, ticks: Int) =
        "[IMPORTANT] FOH Serving (R $restId): Waitstaff $waitstaffId serves ${TestLogFormatter.formatMap(dishes)} " +
            "to table $tableId $ticks ticks after ordering."

    fun noServing(restId: Int, waitstaffId: Int, meals: Int, tableId: Int) =
        "[DEBUG] FOH No Serving (R $restId): Waitstaff $waitstaffId did not serve $meals meals to table $tableId."

    fun deliveryHandover(restId: Int, waitstaffId: Int, dishes: Map<String, Int>, driverId: Int, orderId: Int) =
        "[IMPORTANT] FOH Delivery (R $restId): Waitstaff $waitstaffId serves ${TestLogFormatter.formatMap(dishes)} " +
            "meals to driver $driverId for order $orderId."

    fun noEating(restId: Int, customers: Int, groupId: Int, tableId: Int) =
        "[INFO] Restaurant No Eating (R $restId): $customers customers of group $groupId leave table $tableId " +
            "due to not being served."

    fun finishedEating(restId: Int, customers: Int, groupId: Int, tableId: Int) =
        "[INFO] FOH Finished Eating (R $restId): $customers customers of group $groupId have finished eating " +
            "at table $tableId."

    fun escorting(restId: Int, waitstaffId: Int, customers: Int, groupId: Int, tableId: Int) =
        "[IMPORTANT] FOH Escorting (R $restId): Waitstaff $waitstaffId escorts $customers customers of group " +
            "$groupId from table $tableId outside."

    fun rating(restId: Int, groupId: Int, rating: String, pos: Int, neg: Int) =
        "[INFO] Rating (R $restId): Group $groupId rates the restaurant $restId with $rating rating, " +
            "leading to $pos positive ratings and $neg negative ratings."

    fun servingStatus(restId: Int, waitstaff: Int, meals: Int) =
        "[DEBUG] FOH Serving Status (R $restId): $waitstaff waitstaff served $meals meals."

    fun eatingStatus(restId: Int, eating: Int, finished: Int) =
        "[DEBUG] FOH Eating Status (R $restId): $eating customers are eating and $finished customers have " +
            "finished eating this tick."

    fun escortingStatus(restId: Int, waitstaff: Int, customers: Int) =
        "[DEBUG] FOH Escorting Status (R $restId): $waitstaff waitstaff escorted $customers customers this tick."

    fun ratingStatus(restId: Int, groups: Int) =
        "[DEBUG] Rating Status (R $restId): $groups groups performed ratings this tick."
}
