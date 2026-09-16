package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * kill me
 */
class MyParserTest : ExampleSystemTestExtension() {
    override val name = "MyParserTest"
    override val description = "Tests that files parse and validate correctly"
    override val restaurants = "recipeparserscenario/restaurants.json"
    override val scenario = "recipeparserscenario/scenario.json"
    override val food = "recipeparserscenario/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 0

    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("scenario.json"))
        assertNextLine(InitialAndPrepTestLogs.SIM_START)
    }
}
