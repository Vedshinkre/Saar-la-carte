package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Validates that a STAFF change incident removing a specialized cook
 * and makes their specific dishes unorderable.
 */
class CookChangeNoOrderTest : ExampleSystemTestExtension() {
    override val name = "CookChangeNoOrderTest"
    override val description = "groups cannot order if the only eligible cook is removed by an incident"
    override val restaurants = "cookchangeincident/restaurants.json"
    override val scenario = "cookchangeincident/scenario.json"
    override val food = "cookchangeincident/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 48

    override suspend fun run() {
        // evening 1 Group 1 arrives and successfully orders
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))

        // Assert Group 1 successfully orders Grilled Chicken
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf("Grilled Chicken" to 2),
                waitstaffId = 1
            )
        )

        // EVENING 2: The Incident triggers before preparation
        skipUntilString(TickStatusTestLogs.servingEnd(1))

        // Assert the incident is correctly logged
        assertNextLine(InitialAndPrepTestLogs.incident(1, "STAFF", 2))

        // evening 2, tick 1 Group 2 decides on the restaurant and arrives(as browser doesnt know chef unavailable)
        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        skipUntilString(FohArrivalTestLogs.seating(1, 2, 1, listOf(1)))

        // Assert Group 2 cannot order because Tomato is excluded and the ROAST cook is gone
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 2, 2))
    }
}
