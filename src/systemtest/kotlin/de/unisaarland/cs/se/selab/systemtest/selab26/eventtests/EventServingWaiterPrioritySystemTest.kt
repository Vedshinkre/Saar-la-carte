package de.unisaarland.cs.se.selab.systemtest.selab26.eventtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "Tomato Soup"

/**
 * P04 (spec 2.2 "Front of house"): for an EVENT table the manager assigns as many waiters as needed,
 * prioritizing the waiters in descending number of cooked meals in the kitchen that belong to their
 * assigned tables. EVENT tables are served before CASUAL ones.
 *
 * Restaurant 1 has two waiters, everybody orders the Tomato Soup in tick 2:
 * - EVENT 1 (12 customers) is seated by waiter 1 (10) and waiter 2 (2)
 * - CASUAL 2 (4 customers) is seated by waiter 2, the only one with SEATING capacity left
 * - all 16 soups are cooked in tick 2. Waiter 2 is responsible for 4 of them, waiter 1 for none, so
 *   the manager recruits waiter 2 first: he serves 10 of the event's meals, waiter 1 the other 2
 * - the CASUAL table is served after the EVENT one: waiter 2 has no capacity left, so it is served in
 *   tick 3 by him
 */
class EventServingWaiterPrioritySystemTest : ExampleSystemTestExtension() {
    override val name = "EventServingWaiterPrioritySystemTest"
    override val description = "EVENT serving recruits the waiter with the most cooked meals at own tables first"
    override val restaurants = "eventtests/servingpriority/restaurants.json"
    override val scenario = "eventtests/servingpriority/scenario.json"
    override val food = "eventtests/servingpriority/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 80

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1, 2)))
        skipUntilString(FohArrivalTestLogs.seating(1, 2, 2, listOf(2)))

        skipUntilString(FohServiceTestLogs.serving(1, 2, mapOf(SOUP to 10), 1, 0))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(SOUP to 2), 1, 0))
        // the CASUAL table is not served in this tick
        skipUntilString(FohServiceTestLogs.servingStatus(1, 2, 12))

        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohServiceTestLogs.serving(1, 2, mapOf(SOUP to 4), 2, 1))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 4))
    }
}
