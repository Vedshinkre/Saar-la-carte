package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a STAFF incident targeting a DRIVER but defining a cookType is rejected.
 */
class IncidentDriverWithCookTypeRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentDriverWithCookTypeRejectedSystemTest"
    override val description = "Tests that a DRIVER incident with a defined cookType is rejected"

    override val food = "incidentdrivercooktype/food.json"
    override val restaurants = "incidentdrivercooktype/restaurants.json"
    override val scenario = "incidentdrivercooktype/scenario.json"

    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        // Food and restaurants parse and validate successfully
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))

        // Scenario fails validation because a driver cannot have a cookType
        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
