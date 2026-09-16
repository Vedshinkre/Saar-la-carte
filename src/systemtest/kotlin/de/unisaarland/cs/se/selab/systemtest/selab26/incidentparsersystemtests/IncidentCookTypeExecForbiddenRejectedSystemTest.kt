package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a STAFF incident targeting an EXEC cook is rejected by the parser.
 */
class IncidentCookTypeExecForbiddenRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentCookTypeExecForbiddenRejectedSystemTest"
    override val description = "Tests that an incident targeting an EXEC cook is rejected"

    override val food = "incidentcooktypeexec/food.json"
    override val restaurants = "incidentcooktypeexec/restaurants.json"
    override val scenario = "incidentcooktypeexec/scenario.json"

    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        // Food and restaurants parse and validate successfully
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))

        // Scenario fails validation because cookType = EXEC is forbidden
        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
