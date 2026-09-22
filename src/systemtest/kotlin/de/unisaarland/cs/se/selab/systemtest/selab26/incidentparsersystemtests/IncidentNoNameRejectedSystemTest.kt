package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a recipe without a name that contains the adapted ingredient is rejected.
 */
class IncidentNoNameRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentNoNameRejectedSystemTest"
    override val description: String =
        "Tests that a recipe without a name that contains the adapted ingredient is rejected."

    override val restaurants: String = "incidentnoname/restaurants.json"
    override val scenario: String = "incidentnoname/scenario.json"
    override val food: String = "incidentnoname/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks: Int = 0
    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        assertNextLine(InitialAndPrepTestLogs.initFail("food.json"))
    }
}
