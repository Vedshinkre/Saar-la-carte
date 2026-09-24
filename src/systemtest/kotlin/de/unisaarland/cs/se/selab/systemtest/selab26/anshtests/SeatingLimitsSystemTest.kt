package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "potato soup"

/**
 * for F16
 * 2 waiters, regular groups 1 (4), 2 (6), 3 (1) in tick 1, group 4 (2) in tick 2
 * waiter 1 seats 4 + 6 = exactly 10, still allowed -> group 3 goes to waiter 2
 * tick 2: waiter 1 current load is 10, not below it anymore -> busiest one under 10 is waiter 2
 */
class SeatingLimitsSystemTest : ExampleSystemTestExtension() {
    override val name = "SeatingLimitBoundaries"
    override val description = "SEATING tick load may reach 10 exactly, current load 10 leaves the first pool"
    override val food = "anshtests/seatinglimits/food.json"
    override val restaurants = "anshtests/seatinglimits/restaurants.json"
    override val scenario = "anshtests/seatinglimits/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(SOUP to 4), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 2, mapOf(SOUP to 6), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 3, 3, mapOf(SOUP to 1), 2))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 2, 11, 3))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 11, 2))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 4, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 4, 4, mapOf(SOUP to 2), 2))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
    }
}
