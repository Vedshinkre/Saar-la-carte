package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * changes the availability for 0 day
 */
class IncidentUnavailabilityZeroDurationRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentUnavailabilityZeroDurationRejectedSystemTest"
    override val description = "Tests that an unavailability incident with 0 duration is rejected"
    override val food = "incidentunavailabilityzeroduration/food.json"
    override val restaurants = "incidentunavailabilityzeroduration/restaurants.json"
    override val scenario = "incidentunavailabilityzeroduration/scenario.json"
    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
