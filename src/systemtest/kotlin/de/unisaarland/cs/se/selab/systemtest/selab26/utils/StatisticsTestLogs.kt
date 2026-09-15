package de.unisaarland.cs.se.selab.systemtest.selab26.utils

/**
 * statistics logs
 */
object StatisticsTestLogs {
    const val STATS_CALCULATED = "[IMPORTANT] Simulation Info: Simulation statistics are calculated."

    fun statsCooked(restId: Int, meals: Int) =
        "[IMPORTANT] Simulation Statistics: Restaurant $restId cooked $meals meals."

    fun statsServed(restId: Int, customers: Int) =
        "[IMPORTANT] Simulation Statistics: Restaurant $restId served $customers customers."

    fun statsDelivered(restId: Int, customers: Int) =
        "[IMPORTANT] Simulation Statistics: Restaurant $restId delivered meals to $customers customers."

    fun statsReceived(restId: Int, ratings: Int) =
        "[IMPORTANT] Simulation Statistics: Restaurant $restId received $ratings ratings."
}
