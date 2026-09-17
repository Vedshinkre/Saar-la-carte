package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Verifies that preparation procures the planned ingredients from the supplier.
 */
class SupplierProcurementSystemTest : ExampleSystemTestExtension() {
    override val name = "SupplierProcurementSystemTest"
    override val description = "Verifies supplier procurement during preparation"
    override val restaurants = "nofoodforyoujson/restaurants.json"
    override val scenario = "nofoodforyoujson/scenario.json"
    override val food = "nofoodforyoujson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 250, "g", "Chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, "g", "Tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }
}
