package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * checks the ceil logic for procurement
 */
class ProcurementLogicTestB : ExampleSystemTestExtension() {
    override val name = "ProcurementLogicTestB"
    override val description = "Tests procurement using ceil(seats/10) logic (2 seats = 1 meal)"

    override val restaurants = "nofoodforyoujson/restaurants.json"
    override val scenario = "nofoodforyoujson/scenario.json"
    override val food = "nofoodforyoujson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))

        // Expects exactly 1x the recipe amounts
        // 1 meal * 250g Chicken = 250g Chicken
        // 1 meal * 100g Tomato = 100g Tomato
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 250, "g", "Chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, "g", "Tomato"))

        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(TickStatusTestLogs.servingStart(1))
    }
}
