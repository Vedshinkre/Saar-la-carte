package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that an incident with a negative evening value is correctly rejected by the parser.
 */
class IncidentNegativeEveningRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentNegativeEveningRejectedSystemTest"
    override val description = "Tests that an incident with a negative evening is rejected"

    override val food = "incidentnegativeevening/food.json"
    override val restaurants = "incidentnegativeevening/restaurants.json"
    override val scenario = "incidentnegativeevening/scenario.json"

    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        // Food and restaurants parse and validate successfully
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))

        // Scenario fails validation because an evening cannot be negative
        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
