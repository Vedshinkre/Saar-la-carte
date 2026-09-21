package de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a packaging change with negative packagingVolume is rejected.
 */
class IncidentNegativePackagingVolumeRejectedSystemTest : ExampleSystemTestExtension() {
    override val name = "IncidentNegativePackagingVolumeRejectedSystemTest"
    override val description: String = "Tests that a packaging change with negative packagingVolume is rejected."

    override val restaurants: String = "incidentnegativepackagingvolume/restaurants.json"
    override val scenario: String = "incidentnegativepackagingvolume/scenario.json"
    override val food: String = "incidentnegativepackagingvolume/food.json"

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
