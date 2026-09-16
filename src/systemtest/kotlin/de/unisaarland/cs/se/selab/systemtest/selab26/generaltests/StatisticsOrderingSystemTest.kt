package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.LogLevel
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs

/**
 * Verifies that final statistics are logged in ascending restaurant ID order.
 */
class StatisticsOrderingSystemTest : ExampleSystemTestExtension() {
    override val name = "StatisticsOrderingSystemTest"
    override val description = "Verifies final statistics are ordered by restaurant ID"
    override val restaurants = "statisticsordering/restaurants.json"
    override val scenario = "statisticsordering/scenario.json"
    override val food = "statisticsordering/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 0

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("scenario.json"))
        assertNextLine(InitialAndPrepTestLogs.SIM_START)
        skipUntilLogType(LogLevel.IMPORTANT, LogType.SIMULATION_STATISTICS)
        assertCurrentLine(StatisticsTestLogs.statsCooked(1, 0))
        assertNextLine(StatisticsTestLogs.statsServed(1, 0))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 0))
        assertNextLine(StatisticsTestLogs.statsCooked(2, 0))
        assertNextLine(StatisticsTestLogs.statsServed(2, 0))
        assertNextLine(StatisticsTestLogs.statsDelivered(2, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(2, 0))
        assertNextLine(StatisticsTestLogs.statsCooked(3, 0))
        assertNextLine(StatisticsTestLogs.statsServed(3, 0))
        assertNextLine(StatisticsTestLogs.statsDelivered(3, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(3, 0))
    }
}
