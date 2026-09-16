package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a STAFF incident with a staff number of 0 is rejected by the parser.
 */
class IncidentZeroStaffNumberRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentZeroStaffNumberRejectedSystemTest"
    override val description = "Tests that an incident with 0 staff members is rejected"

    override val food = "incidentzerostaff/food.json"
    override val restaurants = "incidentzerostaff/restaurants.json"
    override val scenario = "incidentzerostaff/scenario.json"

    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        // Food and restaurants parse and validate successfully
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))

        // Scenario fails validation because of number = 0
        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
