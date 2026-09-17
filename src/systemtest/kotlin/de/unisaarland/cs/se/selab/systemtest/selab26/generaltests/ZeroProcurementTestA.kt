package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * we log 0 procure
 */
class ZeroProcurementTestA : ExampleSystemTestExtension() {
    override val name = "ZeroProcurementTestA"
    override val description = "Tests if unavailable ingredients log a 0 procurement"
    override val restaurants = "zeroprocurement/restaurants.json"
    override val scenario = "zeroprocurement/scenario.json"
    override val food = "zeroprocurement/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        // Skip initialization
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))

        // server wants a 0 procurement log
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 0, "g", "Chicken"))

        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }
}
