package de.unisaarland.cs.se.selab.systemtest.selab26.ratingsystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs

/**
 * Tests that casual groups properly give ratings.
 */
class CasualGroupRatingSystemTest : ExampleSystemTestExtension() {
    override val name = "CasualGroupPositiveRatingSystemTest"
    override val description: String = "Tests that casual groups properly give ratings."

    override val restaurants: String = "casualgrouprating/restaurants.json"
    override val scenario: String = "casualgrouprating/scenario.json"
    override val food: String = "casualgrouprating/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks: Int = 48

    override suspend fun run() {
        skipUntilString(FohServiceTestLogs.rating(1, 2, "NEGATIVE", 0, 1))
        skipUntilString(FohServiceTestLogs.ratingStatus(1, 1))

        skipUntilString(FohServiceTestLogs.rating(1, 1, "NEGATIVE", 0, 2))
        skipUntilString(FohServiceTestLogs.ratingStatus(1, 1))

        skipUntilString(FohServiceTestLogs.rating(2, 3, "POSITIVE", 1, 0))
        skipUntilString(FohServiceTestLogs.ratingStatus(2, 1))

        skipUntilString(StatisticsTestLogs.statsReceived(1, 2))
        skipUntilString(StatisticsTestLogs.statsReceived(2, 1))
    }
}
