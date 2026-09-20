package de.unisaarland.cs.se.selab.systemtest.selab26.eventtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "Tomato Soup"

/**
 * P03/P04 (spec 2.2 "Front of house"): after the customers of an EVENT table have eaten, the manager
 * prioritizes the waiters with the lowest current load for ESCORTING them. A waiter escorts at most
 * 10 customers per tick, the rest of the group is escorted by the next waiter. The EVENT group then
 * leaves a rating.
 *
 * Restaurant 1 has three waiters, in tick 2 of evening 4:
 * - REGULAR 2 (8 customers) is seated by waiter 1, REGULAR 3 (3 customers) by waiter 2. They order
 *   the Slow Stew that takes until tick 4 to cook, so they are still seated when the event leaves
 * - EVENT 1 (15 customers) is seated by the waiters with the highest current load first: waiter 1
 *   (8) seats 2, waiter 2 (3) seats 7 and waiter 3 (0) seats 6. It orders the Tomato Soup, is served
 *   in tick 2 and finishes eating in tick 4
 * - EVENT customers do not count towards the current load, so waiter 3 has current load 0 and
 *   escorts first (10 customers), waiter 2 (3) the last 5. Waiter 1 (8) is not needed
 */
class EventEscortingSystemTest : ExampleSystemTestExtension() {
    override val name = "EventEscortingSystemTest"
    override val description = "EVENT escorting recruits the waiters with the lowest current load first"
    override val restaurants = "eventtests/escorting/restaurants.json"
    override val scenario = "eventtests/escorting/scenario.json"
    override val food = "eventtests/escorting/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 80

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.seating(1, 2, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 1, mapOf("Slow Stew" to 8), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 2, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 3, 2, mapOf("Slow Stew" to 3), 2))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 3, listOf(1, 2, 3)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 3, mapOf(SOUP to 15), listOf(1, 2, 3)))

        skipUntilString(TickStatusTestLogs.tickStart(4, 4))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 15, 1, 3))
        skipUntilString(FohServiceTestLogs.escorting(1, 3, 10, 1, 3))
        assertNextLine(FohServiceTestLogs.escorting(1, 2, 5, 1, 3))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 2, 15))
        assertNextLine(FohServiceTestLogs.rating(1, 1, "POSITIVE", 6, 2))
    }
}
