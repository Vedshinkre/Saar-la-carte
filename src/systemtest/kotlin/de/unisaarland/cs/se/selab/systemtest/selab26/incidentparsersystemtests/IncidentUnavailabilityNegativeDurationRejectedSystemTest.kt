package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * negative days change is not allowed
 */
class IncidentUnavailabilityNegativeDurationRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentUnavailabilityNegativeDurationRejectedSystemTest"
    override val description = "Tests that an unavailability incident with negative duration is rejected"
    override val food = "incidentunavailabilitynegativeduration/food.json"
    override val restaurants = "incidentunavailabilitynegativeduration/restaurants.json"
    override val scenario = "incidentunavailabilitynegativeduration/scenario.json"
    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
