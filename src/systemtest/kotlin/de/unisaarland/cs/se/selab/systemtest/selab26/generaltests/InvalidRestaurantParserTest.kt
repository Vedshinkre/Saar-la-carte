package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * kill me
 */
class InvalidRestaurantParserTest : ExampleSystemTestExtension() {
    override val name = "InvalidRestaurantParserTest"
    override val description = "Tests that an invalid restaurants configuration file is correctly rejected"
    override val restaurants = "invalidrestaurants/restaurants.json"
    override val scenario = "invalidrestaurants/scenario.json"
    override val food = "invalidrestaurants/food.json"
    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertInitAndPres()
    }

    private suspend fun assertInitAndPres() {
        // Food parses and validates successfully first
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food"))

        // Restaurants fails validation, so the simulation aborts here
        assertNextLine(InitialAndPrepTestLogs.initFail("restaurants"))
    }
}
