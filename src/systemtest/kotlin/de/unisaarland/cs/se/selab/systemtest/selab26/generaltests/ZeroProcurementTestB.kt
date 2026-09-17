package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * we skip case of procuring 0 ingredient
 */
class ZeroProcurementTestB : ExampleSystemTestExtension() {
    override val name = "ZeroProcurementTestB"
    override val description = "Tests if unavailable ingredients skip the procurement log entirely"
    override val restaurants = "zeroprocurement/restaurants.json"
    override val scenario = "zeroprocurement/scenario.json"
    override val food = "zeroprocurement/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        // Skip initialization until the preparation phase starts
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))

        // we do not procure chicken because we already have it
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }
}
