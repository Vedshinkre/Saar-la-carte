package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val QUICK_SOUP = "Quick Soup"
private const val SLOW_ROAST = "Slow Roast"

/**
 * A REGULAR group orders two dishes cooked by two different cooks with different
 * durations, so one finishes the tick it was ordered and the other only the tick after.
 * F19: the table waits for the WHOLE order to be cooked (FOH No Serving while waiting)
 * and is then served in a single batch, with the correct ticks-since-ordering value.
 */
class RegularGradualCookServingTest : ExampleSystemTestExtension() {
    override val name = "RegularGradualCookServingTest"
    override val description = "A table waits for a slower second dish before being served as one batch"
    override val restaurants = "regulargradualcookjson/restaurants.json"
    override val scenario = "regulargradualcookjson/scenario.json"
    override val food = "regulargradualcookjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 4

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(QUICK_SOUP to 1, SLOW_ROAST to 1), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 2, 1))

        // The basic dish (Quick Soup) is assigned first, then the non-basic Slow Roast.
        assertNextLine(
            KitchenTestLogs.kitchenAssign(1, 1, "TOURNANT", 1, QUICK_SOUP, baseOrderId = 1, allOrders = listOf(1))
        )
        assertNextLine(
            KitchenTestLogs.kitchenAssign(1, 2, "ROAST", 1, SLOW_ROAST, baseOrderId = 1, allOrders = listOf(1))
        )
        // Quick Soup (duration 10) finishes in the same tick it was assigned.
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, QUICK_SOUP, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 2, 1, 1))

        // Only one of the two dishes is ready - the table is not served yet.
        assertNextLine(FohServiceTestLogs.noServing(1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))

        skipUntilString(TickStatusTestLogs.restEnd(1))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        // Slow Roast (duration 20) finishes one tick after it started cooking.
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 2, 1, SLOW_ROAST, 1))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 1, 2))

        // Both dishes are now cooked: the whole order is served together, 1 tick after ordering.
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(QUICK_SOUP to 1, SLOW_ROAST to 1), 1, 1))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 2))
    }
}
