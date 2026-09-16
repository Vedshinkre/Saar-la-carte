package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a STAFF incident containing an 'ingredient' property is correctly rejected.
 */
class IncidentStaffWithIngredientRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentStaffWithIngredientRejectedSystemTest"
    override val description = "Tests that a STAFF incident defining an ingredient is rejected"

    override val food = "incidentstaffwithingredient/food.json"
    override val restaurants = "incidentstaffwithingredient/restaurants.json"
    override val scenario = "incidentstaffwithingredient/scenario.json"

    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        // Food and restaurants parse and validate successfully
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))

        // Scenario fails validation because a STAFF incident should not have an ingredient
        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
