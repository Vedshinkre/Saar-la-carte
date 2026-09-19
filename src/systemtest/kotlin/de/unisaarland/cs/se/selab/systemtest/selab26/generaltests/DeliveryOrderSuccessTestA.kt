package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Validates that Delivery groups bypass the seating phase, order without waitstaff,
 * and correctly increment the delivered meals statistic.
 */
class DeliveryOrderSuccessTestA : ExampleSystemTestExtension() {
    override val name = "DeliveryOrderSuccessTestA"
    override val description = "Checks delivery order flow and statistics (served = 0))"

    override val restaurants = "deliverytestjson/restaurants.json"
    override val food = "deliverytestjson/food.json"
    override val scenario = "deliverytestjson/scenario.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        // Skip to the start of Evening 1, Tick 1
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        // Wait for them to decide on the restaurant
        skipUntilString(TickStatusTestLogs.restDecision(1, 1))

        // Skip the restaurant start log
        skipUntilString(TickStatusTestLogs.restStart(1))

        // Because they are delivery, they dont visit the restaurant physically
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf("Grilled Chicken" to 2),
                waitstaffId = null
            )
        )

        // Skip to the end of the simulation to check stats
        skipUntilString(StatisticsTestLogs.STATS_CALCULATED)

        // Skip the cooked meals stat
        skipUntilString(StatisticsTestLogs.statsCooked(1, 2))

        //  ASSERTION A: are delivery customers "served"
        // Testing if delivery customers are excluded from the "served" total.
        assertNextLine(StatisticsTestLogs.statsServed(1, 0))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 2))
    }
}
