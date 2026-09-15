package de.unisaarland.cs.se.selab.systemtest.selab26.basictests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.LogLevel
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.LogType

/**
 * Example system test
 */
class ExampleSystemTest : ExampleSystemTestExtension() {
    override val name = "ExampleTest"
    override val description = "Tests restaurant statistics after 0 evenings."

    // Paths are relative from the `src/systemtest/resources` directory.
    override val restaurants = "example/restaurants.json"
    override val scenario = "example/scenario.json"
    override val food = "example/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 0

    override suspend fun run() {
        skipUntilLogType(LogLevel.IMPORTANT, LogType.SIMULATION_STATISTICS)
        assertCurrentLine("[IMPORTANT] Simulation Statistics: Restaurant 1 cooked 0 meals.")
        assertNextLine("[IMPORTANT] Simulation Statistics: Restaurant 1 served 0 customers.")
        assertNextLine("[IMPORTANT] Simulation Statistics: Restaurant 1 delivered meals to 0 customers.")
        assertNextLine("[IMPORTANT] Simulation Statistics: Restaurant 1 received 0 ratings.")
    }
}
