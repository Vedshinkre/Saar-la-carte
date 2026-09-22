package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Validates kitchen cook assignment rules across multiple orders:
 * 1. Basic dish priority during assignment within Order 1 (Potato Fries before Roast Beef).
 * 2. Lowest-ranking cook selection (ROAST over TOURNANT for Potato Fries).
 * 3. Scaling effect across orders (Cook 1 batches Potato Fries for Order 1 and Order 2).
 * 4. Fallback to higher-ranking cook when lower-ranking cook is occupied (TOURNANT for Roast Beef).
 * 5. Assignment to specialist when higher-ranking cooks are occupied (PASTRY for Apple Pie).
 */
class KitchenCookAssignmentSystemTest : ExampleSystemTestExtension() {
    override val name = "KitchenCookAssignmentSystemTest"
    override val description = "Verifies cooks are assigned to dishes matching rank, scaling, and availability"
    override val restaurants = "multicooksmultiorders/restaurants.json"
    override val scenario = "multicooksmultiorders/scenario.json"
    override val food = "multicooksmultiorders/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        // Batches both meals across Order 1 and Order 2 via scaling.
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "ROAST",
                meals = 2,
                dishName = "Potato Fries",
                baseOrderId = 1,
                allOrders = listOf(1, 2)
            )
        )

        // 2. Order 1 Non-Basic Dish (Roast Beef):
        // ROAST cook is occupied -> TOURNANT is the only free eligible cook -> Cook ID 2.
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 2,
                cookType = "TOURNANT",
                meals = 1,
                dishName = "Roast Beef",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )

        // TOURNANT cook is occupied -> PASTRY is the only free eligible cook -> Cook ID 3.
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 3,
                cookType = "PASTRY",
                meals = 1,
                dishName = "Apple Pie",
                baseOrderId = 2,
                allOrders = listOf(2)
            )
        )

        // Verify summary: 3 cooks active, 4 meals cooking, 0 finished, 0 servable
        assertNextLine(
            KitchenTestLogs.kitchenStatus(
                restId = 1,
                cooks = 3,
                cooking = 4,
                finished = 0,
                servable = 0
            )
        )
    }
}
