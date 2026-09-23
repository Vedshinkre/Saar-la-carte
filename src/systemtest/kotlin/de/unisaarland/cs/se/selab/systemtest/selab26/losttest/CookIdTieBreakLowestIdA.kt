package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "Tomato Soup"
private const val CHICKEN = "Grilled Chicken"
private const val TOURNANT = "TOURNANT"

/**
 * Validates that between two idle cooks of the same rank who BOTH have assigned IDs,
 * the cook with the LOWEST ID (Cook 1) is chosen per specification.
 */
class CookIdTieBreakLowestIdA : ExampleSystemTestExtension() {
    override val name = "CookIdTieBreakLowestIdA"
    override val description = "Prefers lowest existing cook ID (Cook 1) when both cooks have IDs"
    override val restaurants = "whocooks2/restaurants.json"
    override val scenario = "whocooks2/scenario.json"
    override val food = "whocooks2/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        // Tick 1: Group 1 orders Soup -> Cook 1 assigned.
        // Group 2 orders Chicken -> Cook 1 is busy, so Cook 2 assigned.
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = TOURNANT,
                meals = 2,
                dishName = SOUP,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 2,
                cookType = TOURNANT,
                meals = 2,
                dishName = CHICKEN,
                baseOrderId = 2,
                allOrders = listOf(2)
            )
        )

        // Both dishes finish in Tick 1 (duration 10 = 0 ticks after ordering)
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 1, 2, SOUP, 0))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 2, 2, CHICKEN, 0))

        // Tick 2: Group 3 orders Soup. Both Cook 1 and Cook 2 are free with IDs.
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))

        // VERIFICATION A: Cook 1 (lowest ID) is selected
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = TOURNANT,
                meals = 2,
                dishName = SOUP,
                baseOrderId = 3,
                allOrders = listOf(3)
            )
        )
    }
}
