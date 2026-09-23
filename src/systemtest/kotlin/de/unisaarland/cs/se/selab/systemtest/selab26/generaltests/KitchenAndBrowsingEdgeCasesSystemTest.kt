package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System tests designed to trigger subtle edge cases in Kitchen Scheduling
 * (basic dish prioritization, cook rank selection) and Browsing Service (BAR table non-mergeability).
 */

private const val DEBUG_LOG_LEVEL = "DEBUG"

// =========================================================================
// 1. CUSTOMIZED BASIC DISH PRIORITIZATION SYSTEM TEST
// =========================================================================
/**
 * Triggers the bug where customized basic dishes (where `basicDishFor` might be omitted in JSON)
 * are incorrectly treated as normal dishes and sorted by recipe ID instead of receiving basic dish priority.
 */
class CustomizedBasicDishPrioritySystemTest : ExampleSystemTestExtension() {

    override val name = "CustomizedBasicDishPrioritySystemTest"
    override val description = "A basic dish is assigned before a normal dish with a lower recipe id"
    override val food = "kitchenschedulingtests/customizedbasicdish/food.json"
    override val restaurants = "kitchenschedulingtests/customizedbasicdish/restaurants.json"
    override val scenario = "kitchenschedulingtests/customizedbasicdish/scenario.json"
    override val logLevel = DEBUG_LOG_LEVEL
    override val maxTicks = 2

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("scenario.json"))
        assertNextLine(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, "g", "chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, "g", "potato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(TickStatusTestLogs.servingStart(1))
        assertNextLine(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("chicken rice" to 1, POTATO_SOUP to 1), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 2, 1))

        // CRITICAL CHECK: "potato soup" (basic dish, higher recipe ID = 5) MUST be assigned BEFORE
        // "chicken rice" (normal dish, lower recipe ID = 1)
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "TOURNANT",
                meals = 1,
                dishName = POTATO_SOUP,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, POTATO_SOUP, 0))

        // The restaurant's only cook is busy with potato soup for the rest of this tick, so
        // chicken rice cannot be assigned until the cook is free again on the next tick - it was
        // never a candidate to go first despite its lower recipe id.
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "TOURNANT",
                meals = 1,
                dishName = "chicken rice",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
    }

    private companion object {
        const val POTATO_SOUP = "potato soup"
    }
}

// =========================================================================
// 2. LOWEST-RANKING COOK SELECTION SYSTEM TEST
// =========================================================================
/**
 * Triggers the cook hierarchy bug where assigning a cook picks the highest-ranking cook (EXEC)
 * instead of the lowest-ranking eligible cook (PASTRY = rank 8) as mandated by Section 2.2.
 */
class LowestRankingCookSelectionSystemTest : ExampleSystemTestExtension() {

    override val name = "LowestRankingCookSelectionSystemTest"
    override val description = "The lowest-ranking eligible cook (PASTRY) is chosen over the highest (EXEC)"
    override val food = "kitchenschedulingtests/cookrank/food.json"
    override val restaurants = "kitchenschedulingtests/cookrank/restaurants.json"
    override val scenario = "kitchenschedulingtests/cookrank/scenario.json"
    override val logLevel = DEBUG_LOG_LEVEL
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("apple pie" to 2), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 2, 1))

        // CRITICAL CHECK: PASTRY cook (rank 8, lowest rank) MUST be assigned instead of
        // EXEC cook (rank 1, highest rank)
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "PASTRY",
                meals = 2,
                dishName = "apple pie",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
    }
}

// =========================================================================
// 3. BAR TABLE NON-MERGEABILITY BROWSING SYSTEM TEST
// =========================================================================
/**
 * Triggers the browsing service bug where a Casual group requiring 8 BAR seats is incorrectly
 * marked eligible because total available BAR seats = 8 across two 4-seat BAR tables, ignoring that
 * BAR tables cannot be merged.
 */
class NonMergeableBarTableBrowsingSystemTest : ExampleSystemTestExtension() {

    override val name = "NonMergeableBarTableBrowsingSystemTest"
    override val description =
        "A CASUAL group is rejected when no single BAR table fits, even though two together would"
    override val food = "kitchenschedulingtests/bartable/food.json"
    override val restaurants = "kitchenschedulingtests/bartable/restaurants.json"
    override val scenario = "kitchenschedulingtests/bartable/scenario.json"
    override val logLevel = DEBUG_LOG_LEVEL
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        // CRITICAL CHECK: the Browsing Service sums BAR seats across the two non-mergeable 4-seat
        // BAR tables (4+4=8 >= group size 8) and wrongly decides restaurant 1 is eligible; FOH then
        // correctly finds no single BAR table that fits 8 and sends the group away unseated.
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.noSeatingNoTable(1, 1, 1))
    }
}
