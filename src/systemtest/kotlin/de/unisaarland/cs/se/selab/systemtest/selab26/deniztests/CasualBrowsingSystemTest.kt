package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** Casual browsing ranks by rating difference and skips closed, full, driverless or unsuitable restaurants. */
class CasualBrowsingSystemTest : ExampleSystemTestExtension() {
    override val name = "CasualBrowsingSystemTest"
    override val description = "Casual browsing: rating difference, filters, seats, drivers and new ratings"
    override val food = "deniztests/browsing/food.json"
    override val restaurants = "deniztests/browsing/restaurants.json"
    override val scenario = "deniztests/browsing/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 10

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 2))
        assertNextLine(TickStatusTestLogs.restDecision(2, 3))
        assertNextLine(TickStatusTestLogs.restDecision(3, 3))
        assertNextLine(TickStatusTestLogs.restDecision(4, 3))
        assertNextLine(TickStatusTestLogs.restNoDecision(5))
        skipUntilString(FohServiceTestLogs.rating(2, 7, "NEGATIVE", 3, 1))

        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        assertNextLine(TickStatusTestLogs.restDecision(6, 3))
    }
}
