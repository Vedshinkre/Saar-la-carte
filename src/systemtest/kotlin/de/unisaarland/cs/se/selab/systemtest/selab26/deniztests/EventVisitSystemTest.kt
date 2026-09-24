package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val G = "g"

/** Whole Event visit: decision, planning, failed bar reservation, ordering, eating and rating. */
class EventVisitSystemTest : ExampleSystemTestExtension() {
    override val name = "EventVisitSystemTest"
    override val description = "Event decision, planning, failed reservation, ordering, visit and rating"
    override val food = "deniztests/eventvisit/food.json"
    override val restaurants = "deniztests/eventvisit/restaurants.json"
    override val scenario = "deniztests/eventvisit/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 78

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 2))
        assertNextLine(TickStatusTestLogs.restNoDecision(3))

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))

        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(1, 10, G, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 10, G, "tomato"))

        skipUntilString(InitialAndPrepTestLogs.prepStart(4))
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(1, 10, G, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 70, G, "tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(2, 2))

        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(FohServiceTestLogs.rating(2, 2, "NEGATIVE", 0, 1))

        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("soup" to 5, "stew" to 1), listOf(1)))

        skipUntilString(TickStatusTestLogs.tickStart(6, 4))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 6, 1, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 6))
        assertNextLine(FohServiceTestLogs.rating(1, 1, "POSITIVE", 6, 0))
    }
}
