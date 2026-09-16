package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val GRILLED_CHICKEN = "Grilled Chicken"

/**
 * tests the case where the lowest ranking chef needs to cook dishes of two orders
 */
class OneCookTwoOrdersTest : ExampleSystemTestExtension() {
    override val name = "OneCookTwoOrdersTest"
    override val description = "Tests that the kitchen correctly scales multiple orders simultaneously"
    override val restaurants = "onecooktwoorders/restaurants.json"
    override val scenario = "onecooktwoorders/scenario.json"
    override val food = "onecooktwoorders/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        assertEvening1Tick1()
        assertEvening1Tick2()
    }

    private suspend fun assertEvening1Tick1() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // Group 1 arrives, gets Waitstaff 1, and takes Table 2
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(GRILLED_CHICKEN to 2), 1))

        // Group 2 arrives. Waitstaff 1 has the most customers (2) and is under the 10 load limit
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 1, listOf(1))) // Changed to Waitstaff 1
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 2, mapOf(GRILLED_CHICKEN to 4), 1)) // Waitstaff 1

        // Status: 1 Waitstaff handled all 6 customers
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 6, 2))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 6, 1))

        // Even though there are 2 separate orders, the ROAST cook must group them
        // and cook all 6 meals in a single action based on the first order's ID.
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "ROAST",
                meals = 6,
                dishName = "Grilled Chicken",
                baseOrderId = 1,
                allOrders = listOf(1, 2)
            )
        )

        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 6, 0, 0))

        skipUntilString(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertEvening1Tick2() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        // The single ROAST cook finishes all 6 meals simultaneously 1 tick after ordering
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 6, GRILLED_CHICKEN, 1))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 6, 6, 6))

        // Waitstaff 1 serves BOTH tables!
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(GRILLED_CHICKEN to 2), 2, 1)) // Waitstaff 1
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(GRILLED_CHICKEN to 4), 1, 1)) // Waitstaff 1

        // Status: 1 Waitstaff served 6 meals
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 6))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 6, 0))

        skipUntilString(TickStatusTestLogs.restEnd(1))
    }
}
