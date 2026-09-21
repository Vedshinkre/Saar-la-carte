package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that scenarios containing incidents with non-unique ids are rejected.
 */
class IncidentNonUniqueIdsRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentNonUniqueIdsRejectedSystemTest"
    override val description: String = "Tests that scenarios containing incidents with non-unique ids are rejected."

    override val restaurants: String = "incidentnonuniqueid/restaurants.json"
    override val scenario: String = "incidentnonuniqueid/scenario.json"
    override val food: String = "incidentnonuniqueid/food.json"

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
