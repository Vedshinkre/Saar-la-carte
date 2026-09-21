package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a recipe change with zero adaptation is rejected.
 */
class IncidentZeroAdaptationRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentZeroAdaptationRejectedSystemTest"
    override val description: String = "Tests that a recipe change with zero adaptation is rejected."

    override val restaurants: String = "incidentzeroadaptation/restaurants.json"
    override val scenario: String = "incidentzeroadaptation/scenario.json"
    override val food: String = "incidentzeroadaptation/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks: Int = 0
    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))

        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
