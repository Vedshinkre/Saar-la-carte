package de.unisaarland.cs.se.selab.systemtest.selab26.basictests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Uses the example resources to check initialization, evening 1 preparation
 * (pantry procurement and restocking) and the start of evening 1's serving.
 */
class ExamplePreparationAndServingStartTest : ExampleSystemTestExtension() {
    override val name = "ExamplePreparationAndServingStartTest"
    override val description = "Tests initialization, preparation and serving start for evening 1."

    // Paths are relative from the `src/systemtest/resources` directory.
    override val restaurants = "example/restaurants.json"
    override val scenario = "example/scenario.json"
    override val food = "example/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("scenario.json"))
        assertNextLine(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 20, "g", "rice"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(TickStatusTestLogs.servingStart(1))
    }
}

/**
 * Uses the example resources to check the tick 1 status logs for evening 1,
 * from the tick starting to the restaurant finishing simulating an empty tick.
 */
class ExampleEmptyTickOneCycleTest : ExampleSystemTestExtension() {
    override val name = "ExampleEmptyTickOneCycleTest"
    override val description = "Tests the restaurant's tick 1 status logs when no customers arrive."

    // Paths are relative from the `src/systemtest/resources` directory.
    override val restaurants = "example/restaurants.json"
    override val scenario = "example/scenario.json"
    override val food = "example/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.servingStart(1))

        assertNextLine(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
