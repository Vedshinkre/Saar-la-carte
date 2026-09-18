package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Validates that customers arriving in the last 3 ticks are rejected,
 * and customers still inside at the end of opening time are escorted out.
 */
class RestaurantClosingTest : ExampleSystemTestExtension() {
    override val name = "RestaurantClosingTest"
    override val description = "Checks last-3-ticks rejection, immediate escorting, and negative rating"

    override val restaurants = "closingtimejson/restaurants.json"
    override val food = "closingtimejson/food.json"
    override val scenario = "closingtimejson/scenario.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        // Group 2 arrives and orders outside restriction limits (orderId = 1)
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 2, 1, mapOf("Slow Grilled Chicken" to 2), 1))

        // Group 1 tries to visit in Tick 4 within last 3 ticks
        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        //  restaurant is in final 3 ticks cannot accept new customers
        assertNextLine(TickStatusTestLogs.restNoDecision(1))

        // Closing tick (Tick 5)
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(TickStatusTestLogs.restStart(1))

        // Group 2 is still waiting for food, so the waitstaff MUST immediately escort them outside.
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 2, 2, 1))

        // Group 2 rates negatively.
        assertNextLine(FohServiceTestLogs.rating(1, 2, "NEGATIVE", 10, 1))
    }
}
