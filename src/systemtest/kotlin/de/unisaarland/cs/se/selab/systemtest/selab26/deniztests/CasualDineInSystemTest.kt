package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val NEGATIVE = "NEGATIVE"

/** Casual dine-in groups decide, retry seating, rate after long waits and switch restaurants. */
class CasualDineInSystemTest : ExampleSystemTestExtension() {
    override val name = "CasualDineInSystemTest"
    override val description = "Casual decisions, seating retry, rating likelihoods and evening visits"
    override val food = "deniztests/casual/food.json"
    override val restaurants = "deniztests/casual/restaurants.json"
    override val scenario = "deniztests/casual/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 25

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 1))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertNextLine(TickStatusTestLogs.restDecision(3, 1))
        assertNextLine(TickStatusTestLogs.restNoDecision(4))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))

        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 2, 3, 4))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 2))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))

        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 2, 1, 2))
        assertNextLine(FohServiceTestLogs.noEating(1, 2, 2, 3))
        skipUntilString(FohServiceTestLogs.rating(1, 5, "POSITIVE", 3, 0))
        assertNextLine(FohServiceTestLogs.rating(1, 1, NEGATIVE, 3, 1))
        assertNextLine(FohServiceTestLogs.rating(1, 2, NEGATIVE, 3, 2))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 3))

        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        assertNextLine(TickStatusTestLogs.restDecision(1, 2))
        assertNextLine(TickStatusTestLogs.restStart(1))
    }
}
