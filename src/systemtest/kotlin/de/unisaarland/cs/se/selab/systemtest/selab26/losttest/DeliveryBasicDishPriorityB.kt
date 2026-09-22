package de.unisaarland.cs.se.selab.systemtest.selab26.losttest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Validates that basic dishes are strictly prioritized over non-basic dishes
 * during delivery handoff to drivers, even when the non-basic dish has a lower recipe ID.
 */
class DeliveryBasicDishPriorityB : ExampleSystemTestExtension() {
    override val name = "DeliveryBasicDishPriorityB"
    override val description = "Non Basic dish (ID 1) is handed to driver before basic dish (ID 2)"
    override val restaurants = "recipeprioritytest/restaurants.json"
    override val scenario = "recipeprioritytest/scenario.json"
    override val food = "recipeprioritytest/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        // Skip setup and Tick 1 ordering to Tick 2
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))

        // Wait until Grilled Chicken finishes cooking in Tick 2
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 2, 1, "Grilled Chicken", 1))
        skipUntilString(KitchenTestLogs.kitchenStatus(1, 1, 1, 1, 2))
        // STRICT VERIFICATION:
        // Basic dish (Tomato Soup: ID 2) MUST precede non-basic dish (Grilled Chicken: ID 1) in the map.
        assertNextLine(
            "[IMPORTANT] FOH Delivery (R 1): Waitstaff 1 serves " +
                    "Grilled Chicken:1,Tomato Soup:1 meals to driver 1 for order 1."
        )
    }
}
