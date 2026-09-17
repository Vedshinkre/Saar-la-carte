package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * checks if procurement works with seat size = meal size
 */
class ProcurementLogicTestA : ExampleSystemTestExtension() {
    override val name = "ProcurementLogicTestA"
    override val description = "Tests procurement using strict #Seats multiplier (2 seats = 2 meals)"

    override val restaurants = "nofoodforyoujson/restaurants.json"
    override val scenario = "nofoodforyoujson/scenario.json"
    override val food = "nofoodforyoujson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))

        // Expects exactly 2x the recipe amounts
        // 2 meals * 250g Chicken = 500g Chicken
        // 2 meals * 100g Tomato = 200g Tomato
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 500, "g", "Chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 200, "g", "Tomato"))

        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }
}
