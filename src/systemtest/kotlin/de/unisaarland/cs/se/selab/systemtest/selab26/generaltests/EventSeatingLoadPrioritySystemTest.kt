package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * P02 (FOH - Event Seating): the manager recruits the waiters in descending order of their current
 * load, not by id, and EVENT customers never add to the current load of a waiter.
 *
 * Build-up on evening 4 (3 waiters): in tick 1 CASUAL group 3 (5) is seated by waiter 1 and
 * CASUAL group 4 (8) by waiter 2, as waiter 1 can not take another 8 customers in that tick.
 * The current loads are therefore 5 (id 1), 8 (id 2) and 0 (no id yet).
 * - tick 2: EVENT group 1 (10) is seated completely by the busiest waiter 2 alone
 * - tick 3: EVENT group 2 (12): waiter 2 seats 10, then waiter 1 (load 5) seats the last 2
 * - tick 4: CASUAL group 5 (2) goes to the busiest waiter below 10. As the EVENT customers do
 *   not count, that is still waiter 2 (load 8) and not waiter 1
 */
class EventSeatingLoadPrioritySystemTest : ExampleSystemTestExtension() {
    override val name = "EventSeatingLoadPrioritySystemTest"
    override val description = "EVENT seating recruits waiters by descending current load; EVENTs add no load"
    override val restaurants = "eventseating/loadpriority/restaurants.json"
    override val scenario = "eventseating/loadpriority/scenario.json"
    override val food = "eventseating/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(1)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 4, listOf(2)))

        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(2)))
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 1, 10, 1))

        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1, 2)))
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 2, 12, 1))

        skipUntilString(TickStatusTestLogs.tickStart(4, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 5))
        assertNextLine(FohArrivalTestLogs.seating(1, 5, 5, listOf(2)))
    }
}
