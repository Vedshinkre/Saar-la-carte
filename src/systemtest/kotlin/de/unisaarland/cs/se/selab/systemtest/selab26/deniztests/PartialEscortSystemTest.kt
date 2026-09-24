package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** Escorting waits for unserved leavers, then merged tables are split while reserved ones stay. */
class PartialEscortSystemTest : ExampleSystemTestExtension() {
    override val name = "PartialEscortSystemTest"
    override val description = "Escorting waits for leavers, merged tables are split, reserved ones stay"
    override val food = "deniztests/partialescort/food.json"
    override val restaurants = "deniztests/partialescort/restaurants.json"
    override val scenario = "deniztests/partialescort/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 10

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 6, 2, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 2, 6))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))

        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 2, 1, 3))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 2))

        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.noEating(1, 2, 2, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 6, 2, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 6))

        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        assertNextLine(TickStatusTestLogs.restDecision(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(4, 1))
        assertNextLine(TickStatusTestLogs.restNoDecision(5))
        skipUntilString(FohArrivalTestLogs.seating(1, 3, 1, listOf(1)))
        skipUntilString(FohArrivalTestLogs.seating(1, 4, 2, listOf(1)))
    }
}
