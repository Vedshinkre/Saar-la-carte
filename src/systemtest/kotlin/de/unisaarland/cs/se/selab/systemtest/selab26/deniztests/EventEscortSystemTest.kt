package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** Groups are escorted Regular, Event, Casual, and event escorts are split by current waiter load. */
class EventEscortSystemTest : ExampleSystemTestExtension() {
    override val name = "EventEscortSystemTest"
    override val description = "Escorting order Regular, Event, Casual, and Event escorts by current load"
    override val food = "deniztests/eventescort/food.json"
    override val restaurants = "deniztests/eventescort/restaurants.json"
    override val scenario = "deniztests/eventescort/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(4, 4))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 10, 3, 1))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 8, 2, 2))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 2, 1, 3))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 20))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 10, 3, 1))
        assertNextLine(FohServiceTestLogs.escorting(1, 3, 8, 2, 2))
        assertNextLine(FohServiceTestLogs.escorting(1, 2, 2, 1, 3))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 3, 20))

        skipUntilString(TickStatusTestLogs.tickStart(4, 5))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 15, 4, 4))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 15))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 10, 4, 4))
        assertNextLine(FohServiceTestLogs.escorting(1, 2, 5, 4, 4))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 2, 15))
    }
}
