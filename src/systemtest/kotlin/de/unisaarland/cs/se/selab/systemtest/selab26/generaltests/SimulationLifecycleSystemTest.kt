package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.LogLevel
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.LogType
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Verifies the simulation lifecycle through the real command-line test harness.
 */
class SimulationLifecycleSystemTest : ExampleSystemTestExtension() {
    override val name = "SimulationLifecycleSystemTest"
    override val description = "Runs one real simulation tick and verifies its lifecycle logs"
    override val restaurants = "simplejson/restaurants.json"
    override val scenario = "simplejson/scenario.json"
    override val food = "simplejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("scenario.json"))
        assertNextLine(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        skipUntilString(TickStatusTestLogs.servingStart(1))
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilLogType(LogLevel.IMPORTANT, LogType.SIMULATION_STATISTICS)
        assertCurrentLine(StatisticsTestLogs.statsCooked(1, 0))
        assertNextLine(StatisticsTestLogs.statsServed(1, 0))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 0))
    }
}
