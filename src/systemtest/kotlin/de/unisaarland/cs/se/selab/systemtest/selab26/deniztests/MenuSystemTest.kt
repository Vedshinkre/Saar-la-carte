package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/** Only listed dishes with an eligible cook and enough pantry stock can be ordered. */
class MenuSystemTest : ExampleSystemTestExtension() {
    override val name = "MenuSystemTest"
    override val description = "Only listed, cookable dishes with enough pantry stock can be ordered"
    override val food = "deniztests/menu/food.json"
    override val restaurants = "deniztests/menu/restaurants.json"
    override val scenario = "deniztests/menu/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(1, 50, "g", "truffle"))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("tomato soup" to 2, "truffle pasta" to 2), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 4, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 4, 1))
    }
}
