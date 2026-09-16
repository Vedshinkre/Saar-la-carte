package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * two different incident logics cannot be together
 */
class IncidentUnavailabilityProhibitedPropertyRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentUnavailabilityProhibitedPropertyRejectedSystemTest"
    override val description = "Tests that an unavailability incident with a prohibited property is rejected"
    override val food = "incidentunavailabilityprohibitedproperty/food.json"
    override val restaurants = "incidentunavailabilityprohibitedproperty/restaurants.json"
    override val scenario = "incidentunavailabilityprohibitedproperty/scenario.json"
    override val logLevel = "INFO"
    override val maxTicks = 0

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
