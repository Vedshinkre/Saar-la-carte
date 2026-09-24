package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** Event browsing checks opening at the visiting tick and sums seats per event evening. */
class EventBrowsingSystemTest : ExampleSystemTestExtension() {
    override val name = "EventBrowsingSystemTest"
    override val description = "Event browsing: opening at the visiting tick, seats summed per event evening"
    override val food = "deniztests/browsingevent/food.json"
    override val restaurants = "deniztests/browsingevent/restaurants.json"
    override val scenario = "deniztests/browsingevent/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 25

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 2))
        assertNextLine(TickStatusTestLogs.restDecision(2, 2))
        assertNextLine(TickStatusTestLogs.restDecision(3, 3))
        assertNextLine(TickStatusTestLogs.restStart(1))

        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        assertNextLine(TickStatusTestLogs.restDecision(4, 2))
        assertNextLine(TickStatusTestLogs.restStart(1))
    }
}
