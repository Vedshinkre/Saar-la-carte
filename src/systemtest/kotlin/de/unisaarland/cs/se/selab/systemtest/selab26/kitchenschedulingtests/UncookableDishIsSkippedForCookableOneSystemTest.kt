package de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec (p.13): "Orders queue waiting for the next eligible cook to be available." A dish that
 * has no free eligible cook this tick must not block the kitchen from starting a different,
 * cookable dish -- the kitchen must keep evaluating the remaining dishes in the queue.
 *
 * Group 1 ties up the restaurant's only TOURNANT cook for many ticks on a long dish (Roast
 * Beef). Group 2 then places one order containing both the basic dish (Potato Soup, needs
 * TOURNANT -- unavailable) and a non-basic dish (Side Salad, needs ROAST -- free). Basic-dish
 * precedence would normally put Potato Soup first, but since no TOURNANT cook is free, the
 * kitchen must skip it this tick and still start Side Salad with the free ROAST cook.
 */
class UncookableDishIsSkippedForCookableOneSystemTest : ExampleSystemTestExtension() {
    override val name = "UncookableDishIsSkippedForCookableOneSystemTest"
    override val description =
        "A dish with no free eligible cook is skipped so a cookable dish still starts the same tick"
    override val restaurants = "kitchenschedulingtests/uncookableskipped/restaurants.json"
    override val scenario = "kitchenschedulingtests/uncookableskipped/scenario.json"
    override val food = "kitchenschedulingtests/uncookableskipped/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 3

    override suspend fun run() {
        // Tick 1: group 1 orders 2 meals of Roast Beef, taking the only TOURNANT cook for a
        // long time.
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(
            FohArrivalTestLogs.ordering(1, 1, 1, mapOf("Roast Beef" to 2), 1)
        )
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "TOURNANT",
                meals = 2,
                dishName = "Roast Beef",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )

        // Tick 2: group 2 orders Potato Soup (basic, needs TOURNANT) and Side Salad (needs
        // ROAST). The TOURNANT cook is still busy with Roast Beef, so only Side Salad starts.
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(
            FohArrivalTestLogs.ordering(
                1,
                2,
                2,
                mapOf("Potato Soup" to 1, "Side Salad" to 1),
                1
            )
        )
        skipUntilString(FohArrivalTestLogs.orderingStatus(1, 2, 1))

        // The very next Kitchen Dish Assignment line must be Side Salad, not Potato Soup:
        // Potato Soup has basic-dish precedence but no free eligible cook, so it is skipped.
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 2,
                cookType = "ROAST",
                meals = 1,
                dishName = "Side Salad",
                baseOrderId = 2,
                allOrders = listOf(2)
            )
        )
        // No further Kitchen Dish Assignment line follows for Potato Soup this tick: only the
        // 2 Roast Beef meals (still cooking) and the 1 Side Salad meal just started are active.
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 3, 0, 0))
    }
}
