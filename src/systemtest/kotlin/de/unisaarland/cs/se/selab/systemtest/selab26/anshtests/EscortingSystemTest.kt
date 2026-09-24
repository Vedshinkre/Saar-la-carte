package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * for F21
 * one waiter, tables 1 and 2 of 4
 *
 * regular group 2 has table 1 reserved, casual group 1 gets table 2, both done eating in tick 3
 * regular escorted first even with the higher id
 * casual group 3 comes in tick 4 -> gets freed table 2, table 1 stays reserved
 */
class EscortingSystemTest : ExampleSystemTestExtension() {
    override val name = "EscortingOrderAndTableReuse"
    override val description = "Escorting by group type before id, escorting status and reuse of freed tables"
    override val food = "anshtests/escorting/food.json"
    override val restaurants = "anshtests/escorting/restaurants.json"
    override val scenario = "anshtests/escorting/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 6

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 4, 2, 1))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 4, 1, 2))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 8))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 4, 2, 1))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 4, 1, 2))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 8))

        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 2, listOf(1)))

        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 4, 3, 2))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 4))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 4, 3, 2))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 4))
    }
}
