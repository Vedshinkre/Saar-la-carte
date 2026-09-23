package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "Tomato Soup"

/**
 * Validates that between two idle cooks of the same rank, the cook that
 * ALREADY HAS an assigned ID (Cook 1) is chosen over the unassigned cook.
 */
class IdleCookReuseA : ExampleSystemTestExtension() {
    override val name = "IdleCookReuseA"
    override val description = "Prefers cook with existing ID (Cook 1) over unassigned cook"
    override val restaurants = "whocooks/restaurants.json"
    override val scenario = "whocooks/scenario.json"
    override val food = "whocooks/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        // Tick 1: Group 1 orders Tomato Soup. First TOURNANT cook becomes Cook 1.
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "TOURNANT",
                meals = 2,
                dishName = SOUP,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        // Soup duration is 10 (0 ticks), so Cook 1 finishes in Tick 1.
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 1, 2, SOUP, 0))

        // Tick 2: Group 2 orders Tomato Soup.
        // Cook 1 is free (has ID 1) and the second TOURNANT cook is free (no ID).
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))

        // STRICT VERIFICATION A: Cook 1 is reused.
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "TOURNANT",
                meals = 2,
                dishName = SOUP,
                baseOrderId = 2,
                allOrders = listOf(2)
            )
        )
    }
}
