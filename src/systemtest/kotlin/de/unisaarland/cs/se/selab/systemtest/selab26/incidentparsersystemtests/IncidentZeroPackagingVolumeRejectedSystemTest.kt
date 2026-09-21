package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a packaging incident with zero packagingVolume is rejected by the parser.
 */
class IncidentZeroPackagingVolumeRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentZeroPackagingVolumeRejectedSystemTest"
    override val description: String = "Tests that a packaging change with zero packagingVolume is rejected."

    override val restaurants: String = "incidentzeropackagingvolume/restaurants.json"
    override val scenario: String = "incidentzeropackagingvolume/scenario.json"
    override val food: String = "incidentzeropackagingvolume/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks: Int = 0
    override suspend fun run() {
        assertInitAndPrep()
    }

    private suspend fun assertInitAndPrep() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))

        assertNextLine(InitialAndPrepTestLogs.initFail("scenario.json"))
    }
}
