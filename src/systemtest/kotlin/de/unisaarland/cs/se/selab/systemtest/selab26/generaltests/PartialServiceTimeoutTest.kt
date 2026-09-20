package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val FAST = "fast"
private const val SLOW = "slow"
private const val BLOCKER = "blocker"
private const val TOURNANT = "TOURNANT"

/**
 * Validates the partial serving patience rule:
 * Group 2 receives partial food, extending their patience from 5 to 7 ticks.
 * However, their second dish takes too long, causing them to abandon the restaurant.
 */
class PartialServiceTimeoutTest : ExampleSystemTestExtension() {
    override val name = "PartialServiceTimeoutTest"
    override val description = "Group receives partial food but abandons restaurant when remaining food takes too long"
    override val restaurants = "partaildeliveryjson/restaurants.json"
    override val scenario = "partaildeliveryjson/scenario.json"
    override val food = "partaildeliveryjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    override suspend fun run() {
        // TICK 1 & 2: Arrivals and Ordering
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(BLOCKER to 2), 1))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 2, 2, mapOf(FAST to 1, SLOW to 1), 1))

        //  TICK 5 & 6: Blocker finishes, Fast finishes
        // Group 1's blocker finishes and is served
        skipUntilString(KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 2, dishName = BLOCKER, ticks = 4))

        // Cook then immediately finishes the "fast" dish for Group 2
        skipUntilString(KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 1, dishName = FAST, ticks = 4))

        // Cook starts the "slow" dish for Group 2
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, SLOW, 2, listOf(2)))

        //  TICK 7: Partial Service
        // Waitstaff serves the "fast" dish to Group 2.
        // This extends their patience from 5 ticks to 7 ticks (expires at Tick 9).
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(FAST to 1), 2, 5))

        //  TICK 9: Timeout
        // It is now Tick 9. Group 2's extended patience has expired.
        // The "slow" dish is still cooking, so they abandon the restaurant.
        skipUntilString(TickStatusTestLogs.tickStart(9, 1))
        skipUntilString(FohServiceTestLogs.noEating(restId = 1, customers = 2, groupId = 2, tableId = 2))

        //  TICK 10: Late Food
        // The slow dish finally finishes, but it's too late.
        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 1, dishName = SLOW, ticks = 8))
    }
}
