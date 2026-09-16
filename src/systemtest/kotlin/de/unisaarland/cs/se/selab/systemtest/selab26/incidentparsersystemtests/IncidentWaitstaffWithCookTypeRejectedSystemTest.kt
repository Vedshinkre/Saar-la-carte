package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a STAFF incident targeting WAITSTAFF but defining a cookType is rejected.
 */
class IncidentWaitstaffWithCookTypeRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentWaitstaffWithCookTypeRejectedSystemTest"
    override val description = "Tests that a WAITSTAFF incident with a defined cookType is rejected"

    override val food = "incidentwaitstaffcooktype/food.json"
    override val restaurants = "incidentwaitstaffcooktype/restaurants.json"
    override val scenario = "incidentwaitstaffcooktype/scenario.json"

    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        // Food and restaurants parse and validate successfully
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))

        // Scenario fails validation because waitstaff cannot have a cookType
        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
