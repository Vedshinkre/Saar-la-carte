package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "potato soup"

/**
 * for P02
 * 4 waiters, casuals in ticks 1-2 get waiter 1 to load 15 and waiter 2 to 5
 *
 * tick 3: regular 5 -> waiter 2 (load 11), then event 6 (22) split 10/4/8 over waiters 1, 2, 3
 * casual 1 seated after the event even with the lower id
 *
 * tick 4: events don't add load, so event 7 (25) goes to 1, 2, 4 instead of 1, 2, 3
 * event 8 (16) only has 15 seats left -> not seated, rates negative
 */
class EventSeatingSystemTest : ExampleSystemTestExtension() {
    override val name = "EventSeatingWaitstaffSplit"
    override val description = "EVENT seating split by current load, events not counted, too big event fails"
    override val food = "anshtests/eventseating/food.json"
    override val restaurants = "anshtests/eventseating/restaurants.json"
    override val scenario = "anshtests/eventseating/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 76

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 6, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 1, mapOf(SOUP to 9), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 7, listOf(2)))

        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 8, listOf(1)))

        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 5))
        assertNextLine(FohArrivalTestLogs.seating(1, 5, 5, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 5, 4, mapOf(SOUP to 6), 2))
        assertNextLine(FohArrivalTestLogs.arrival(1, 6))
        assertNextLine(FohArrivalTestLogs.seating(1, 6, 1, listOf(1, 2, 3)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 9, listOf(4)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 6, mapOf(SOUP to 3), 4))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 4, 31, 3))

        skipUntilString(TickStatusTestLogs.tickStart(4, 4))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 7))
        assertNextLine(FohArrivalTestLogs.seating(1, 7, 2, listOf(1, 2, 4)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 8))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 8))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 3, 25, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 8, "NEGATIVE", 0, 1))
    }
}
