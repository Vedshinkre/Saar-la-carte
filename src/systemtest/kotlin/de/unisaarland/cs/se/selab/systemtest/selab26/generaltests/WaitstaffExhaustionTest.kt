package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * no waiter or table for use
 */
class WaitstaffExhaustionTest : ExampleSystemTestExtension() {
    override val name = "WaitstaffExhaustionTest"
    override val description = "Tests waitstaff capacity limits and table unavailability across ticks"
    override val restaurants = "notableorwaiteravailabejson/restaurants.json"
    override val scenario = "notableorwaiteravailabejson/scenario.json"
    override val food = "notableorwaiteravailabejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        assertEvening1Tick1()
        assertEvening1Tick2()
    }

    private suspend fun assertEvening1Tick1() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        // Both groups pass the Browsing Service because 20 seats > 13 required
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // Group 1 arrives and successfully takes Waiter 1 and Table 1
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("Grilled Chicken" to 9), 1))

        // Group 2 arrives. Needs 4 capacity. Waiter 1 only has 1 left. Failure!
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 2))

        // Status summaries (Group 2 is not counted as seated or ordered)
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 9, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 9, 1))

        skipUntilString(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertEvening1Tick2() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // Group 2 tries again! Waitstaff capacity reset to 10.
        // Waitstaff 1 is assigned! But Table 2 (Size 10) violates the 3/4 rule for 4 people.
        assertNextLine(FohArrivalTestLogs.noSeatingNoTable(1, 1, 2))

        // Group 2 leaves immediately. Statuses reflect 0 new seating.
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        skipUntilString(FohServiceTestLogs.escortingStatus(1, 0, 0))

        // Group 2 leaves a negative rating for being sent away! (Negatives go from 5 to 6)
        assertNextLine(FohServiceTestLogs.rating(1, 2, "NEGATIVE", 5, 6))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))

        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
