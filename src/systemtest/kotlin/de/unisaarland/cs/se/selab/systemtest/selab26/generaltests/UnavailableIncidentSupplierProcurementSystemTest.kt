package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Verifies that unavailable stock cannot be procured from the supplier.
 */
class UnavailableIncidentSupplierProcurementSystemTest : ExampleSystemTestExtension() {
    override val name = "SimpleJsonSupplierUnavailableSystemTest"
    override val description = "Verifies supplier behavior when simplejson stock is unavailable"
    override val restaurants = "simplejson/restaurants.json"
    override val scenario = "simplejsonsupplierunavailable/scenario.json"
    override val food = "simplejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, "UNAVAILABLE", 1))
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        // Chicken is unavailable, so nothing is procured for it -- no log line at all,
        // per spec: the log only describes ingredients that *were* procured.
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, "g", "Tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }
}
