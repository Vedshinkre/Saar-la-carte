package de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val TOMATO_SOUP = "Tomato Soup"

/**
 * Spec (p.13): "Per order in the queue, the dishes of the order are assigned to a cook. Basic
 * dishes take precedence, then the lower id of the dish recipe."
 *
 * The order here contains a non-basic dish with recipe id 1 (Grilled Chicken) and the
 * restaurant's basic dish with the *higher* recipe id 2 (Tomato Soup). Sorting by id alone would
 * put Grilled Chicken first; the spec requires Tomato Soup to be assigned first regardless.
 */
class BasicDishPrecedenceOverLowerIdSystemTest : ExampleSystemTestExtension() {
    override val name = "BasicDishPrecedenceOverLowerIdSystemTest"
    override val description = "Basic dish is assigned before a non-basic dish with a lower recipe id"
    override val restaurants = "kitchenschedulingtests/basicprecedence/restaurants.json"
    override val scenario = "kitchenschedulingtests/basicprecedence/scenario.json"
    override val food = "kitchenschedulingtests/basicprecedence/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        skipUntilString(
            FohArrivalTestLogs.ordering(
                1,
                1,
                1,
                mapOf("Grilled Chicken" to 1, TOMATO_SOUP to 1),
                1
            )
        )
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 2, 1))

        // Basic dish (Tomato Soup, id 2) must be assigned to a cook before the non-basic
        // dish (Grilled Chicken, id 1), even though its recipe id is higher.
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "TOURNANT",
                meals = 1,
                dishName = TOMATO_SOUP,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 2,
                cookType = "ROAST",
                meals = 1,
                dishName = "Grilled Chicken",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        // Tomato Soup's duration (10) resolves to 0 remaining ticks, so it finishes cooking in
        // the same tick it was assigned.
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, TOMATO_SOUP, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 2, 1, 1))
    }
}
