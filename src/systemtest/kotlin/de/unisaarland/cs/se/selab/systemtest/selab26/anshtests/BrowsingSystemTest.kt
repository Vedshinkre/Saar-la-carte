package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * for F28
 * 3 European restaurants, all tables of 4
 *
 * rest 1: rating 10, no events, common + bar
 * rest 2: rating 5, events, common
 * rest 3: rating 8, events, common, closes at tick 10
 *
 * event groups 1, 2 (4 each, tick 8 of evening 4) skip rest 1 (no events) and rest 3 (tick 8 is in its last 3)
 * -> group 1 gets rest 2, group 2 gets nothing since that event took the seats
 *
 * casual group 3 (4, common) fills rest 1 common seats
 * -> group 4 (2, common) goes to rest 3, group 5 (4, bar) still fits the bar seats of rest 1
 */
class BrowsingSystemTest : ExampleSystemTestExtension() {
    override val name = "BrowsingEventFiltersAndSeatsPerTableType"
    override val description = "EVENT browsing filters and seat estimates kept per table type"
    override val food = "anshtests/browsing/food.json"
    override val restaurants = "anshtests/browsing/restaurants.json"
    override val scenario = "anshtests/browsing/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 2))
        assertNextLine(TickStatusTestLogs.restNoDecision(2))
        assertNextLine(TickStatusTestLogs.restDecision(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(4, 3))
        assertNextLine(TickStatusTestLogs.restDecision(5, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
    }
}
