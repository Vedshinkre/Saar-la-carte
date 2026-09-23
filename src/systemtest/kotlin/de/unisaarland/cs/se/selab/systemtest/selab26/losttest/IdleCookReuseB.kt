package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "Tomato Soup"

/**
 * Validates whether the implementation picks an UNASSIGNED cook first,
 * giving them a new ID (Cook 2) instead of reusing Cook 1.
 */
class IdleCookReuseB : ExampleSystemTestExtension() {
    override val name = "IdleCookReuseB"
    override val description = "Picks unassigned cook first, creating Cook 2"
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
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))

        // STRICT VERIFICATION B: Fresh cook is chosen, receiving Cook ID 2.
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 2,
                cookType = "TOURNANT",
                meals = 2,
                dishName = SOUP,
                baseOrderId = 2,
                allOrders = listOf(2)
            )
        )
    }
}
