package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * F27: customers wait 5 ticks for their food. One ROAST cook needs 3 ticks per order, so group 1 (ordered in
 * tick 2) is served in time, while groups 2 and 3 (ordered in tick 3) are still waiting when the cook only
 * starts their order. They leave in tick 8, five ticks after ordering, and rate negatively.
 */
class WaitingForFoodSystemTest : ExampleSystemTestExtension() {
    override val name = "WaitingForFoodSystemTest"
    override val description = "Checks that customers are served in time, or leave five ticks after ordering"

    override val restaurants = "waitingforfoodjson/restaurants.json"
    override val food = "waitingforfoodjson/food.json"
    override val scenario = "waitingforfoodjson/scenario.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 12

    override suspend fun run() {
        val dish = "Slow Roast"

        // group 1 orders in tick 2 and is served in tick 5, 3 ticks after ordering
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(dish to 2), 1))
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))

        // group 2 orders for both customers; only one customer of group 3 can be served a dish
        skipUntilString(FohArrivalTestLogs.ordering(1, 2, 2, mapOf(dish to 2), 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 3, 3, mapOf(dish to 1), 1))
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(dish to 2), 1, 3))

        // group 1 eats for two ticks, is escorted and rates positively
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 2, 1, 1))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 2, 1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 1, "POSITIVE", 11, 0))

        // nobody left before tick 8: the leave logs must only appear after the start of tick 8
        skipUntilString(TickStatusTestLogs.tickStart(8, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 2, 2, 2))
        skipUntilString(FohServiceTestLogs.noEating(1, 1, 3, 3))
        skipUntilString(FohServiceTestLogs.rating(1, 2, "NEGATIVE", 11, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 3, "NEGATIVE", 11, 2))
    }
}
