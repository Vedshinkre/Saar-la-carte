package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Validates reservation conflicts, ratings for rejected groups, and table merging.
 */
class EventReservationConflictTest : ExampleSystemTestExtension() {
    override val name = "EventReservationConflictTest"
    override val description = "Tests reservation conflict between EVENT and REGULAR groups on Evening 4"
    override val restaurants = "notablereservationjson/restaurants.json"
    override val scenario = "notablereservationjson/scenario.json"
    override val food = "notablereservationjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        //  Tick 1, Evening 1: The EVENT group decides on the restaurant
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))

        //  Evening 4 Prep: EVENT takes the tables, leaving nothing for the REGULAR group (Group 2)
        skipUntilString(InitialAndPrepTestLogs.prepStart(4))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 2))

        //  Evening 4, Tick 1: The rejected REGULAR group leaves a negative rating
        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(FohServiceTestLogs.rating(1, 2, "NEGATIVE", 10, 4))

        // Evening 4, Tick 2: The EVENT group arrives and Waiter 1 merges the tables
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.mergingTables(1, 1, listOf(1, 2, 3), 1))
    }
}
