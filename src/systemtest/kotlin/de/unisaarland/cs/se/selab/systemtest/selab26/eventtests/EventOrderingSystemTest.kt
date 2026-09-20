package de.unisaarland.cs.se.selab.systemtest.selab26.eventtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "Tomato Soup"
private const val CURRY = "Chicken Curry"
private const val STEW = "Lentil Stew"

/**
 * P03 (spec 2.2 "Front of house" and 2.3 "Ordering"): among the dishes where they eat all
 * ingredients, event customers prioritize the favorite dish of the event over their own favorite
 * dish or preferred ingredients. Only if they cannot eat it, their own preferences apply. They order
 * from the dishes that are available, customers who find no dish leave without ordering.
 *
 * Restaurant 1 (one waiter) serves Tomato Soup (basic dish and favorite dish of both events),
 * Chicken Curry, Lentil Stew and Almond Tart. Nobody in the kitchen can cook the Almond Tart, so it
 * is on the menu but cannot be ordered.
 *
 * EVENT 1 (8 customers), tick 2:
 * - 2 customers favor Chicken Curry and 2 prefer Lentil: the event dish wins, Tomato Soup
 * - 1 customer excludes Tomato and favors Lentil Stew: cannot eat the event dish, Lentil Stew
 * - 2 customers exclude Tomato and prefer Chicken: Chicken Curry contains it
 * - 1 customer excludes Tomato only: highest recipe id of the available dishes he eats (the
 *   unavailable Almond Tart does not count), Lentil Stew
 *
 * EVENT 2 (5 customers), tick 3: 3 customers favor Lentil Stew, but eat Tomato Soup. The other 2
 * exclude everything but Almond Tart, which nobody can cook: they leave without ordering.
 */
class EventOrderingSystemTest : ExampleSystemTestExtension() {
    override val name = "EventOrderingSystemTest"
    override val description = "EVENT customers order the event favorite dish before their own preferences"
    override val restaurants = "eventtests/ordering/restaurants.json"
    override val scenario = "eventtests/ordering/scenario.json"
    override val food = "eventtests/ordering/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 80

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(SOUP to 4, CURRY to 2, STEW to 2), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 8, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 8, 1))

        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 2, mapOf(SOUP to 3), 1))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 2, 2))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 5, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 3, 1))
    }
}
