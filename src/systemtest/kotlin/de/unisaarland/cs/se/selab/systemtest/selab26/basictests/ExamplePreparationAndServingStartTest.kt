package de.unisaarland.cs.se.selab.systemtest.selab26.basictests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Uses the example resources to check initialization, evening 1 preparation
 * (pantry procurement and restocking) and the start of evening 1's serving.
 */
class ExamplePreparationAndServingStartTest : ExampleSystemTestExtension() {
    override val name = "ExamplePreparationAndServingStartTest"
    override val description = "Tests initialization, preparation and serving start for evening 1."

    // Paths are relative from the `src/systemtest/resources` directory.
    override val restaurants = "example/restaurants.json"
    override val scenario = "example/scenario.json"
    override val food = "example/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("scenario.json"))
        assertNextLine(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 20, "g", "rice"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(TickStatusTestLogs.servingStart(1))
    }
}
