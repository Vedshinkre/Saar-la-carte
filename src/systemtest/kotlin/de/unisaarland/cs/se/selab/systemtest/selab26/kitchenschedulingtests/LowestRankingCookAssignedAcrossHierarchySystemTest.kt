package de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec (p.13): "the cooks are ranked [EXEC, SOUS, TOURNANT, SAUCE, FISH, ROAST, VEGETABLE,
 * PASTRY]... including the ranking within the specialized cooks... If there are multiple
 * eligible cooks for a dish within an order, the dish is assigned to the lowest-ranking eligible
 * cook."
 *
 * One order carries two dishes: "Chef Special" is cookable by both EXEC (highest rank) and
 * VEGETABLE (a low-ranking specialist), and "Root Roast" is cookable by both ROAST and PASTRY
 * (the lowest-ranking specialist). Both cooks of each pair are free, so the *lowest*-ranking one
 * must be chosen each time: VEGETABLE over EXEC, and PASTRY over ROAST.
 */
class LowestRankingCookAssignedAcrossHierarchySystemTest : ExampleSystemTestExtension() {
    override val name = "LowestRankingCookAssignedAcrossHierarchySystemTest"
    override val description = "The lowest-ranking eligible cook is chosen, both across and within specialists"
    override val restaurants = "kitchenschedulingtests/hierarchy/restaurants.json"
    override val scenario = "kitchenschedulingtests/hierarchy/scenario.json"
    override val food = "kitchenschedulingtests/hierarchy/food.json"
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
                mapOf("Chef Special" to 1, "Root Roast" to 1),
                1
            )
        )
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 2, 1))

        // Chef Special is eligible for EXEC and VEGETABLE: VEGETABLE (the lower-ranking cook)
        // must be chosen, never the head cook.
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "VEGETABLE",
                meals = 1,
                dishName = "Chef Special",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        // Root Roast is eligible for ROAST and PASTRY: PASTRY ranks lower than ROAST among the
        // specialists (SAUCE > FISH > ROAST > VEGETABLE > PASTRY), so PASTRY must be chosen.
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 2,
                cookType = "PASTRY",
                meals = 1,
                dishName = "Root Roast",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 2, 0, 0))
    }
}
