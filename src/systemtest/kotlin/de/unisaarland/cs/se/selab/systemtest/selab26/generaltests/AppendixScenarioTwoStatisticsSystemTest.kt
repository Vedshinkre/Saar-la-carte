package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 6 -- statistics calculated at the end of tick 1 of evening 30.
 */
class AppendixScenarioTwoStatisticsSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioTwoStatisticsSystemTest"
    override val description = "Appendix scenario 2, phase 6: end-of-tick statistics."
    override val food = "appendixScenario2/food.json"
    override val restaurants = "appendixScenario2/restaurants.json"
    override val scenario = "appendixScenario2/scenario.json"
    override val logLevel = "DEBUG"

    // 29 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 1 of
    // evening 30, the first evening tonight's diagram takes place on.
    override val maxTicks = 29 * 24 + 1

    override suspend fun run() {
        // Skip past initialization, preparation, tick start, arrival, kitchen, and service --
        // covered by earlier phases -- straight to end-of-tick statistics.
        skipUntilString(TickStatusTestLogs.tickStart(1, 30))
        skipUntilString(TickStatusTestLogs.restEnd(1))

        assertNextLine(StatisticsTestLogs.STATS_CALCULATED)
        // Cumulative over the whole run: rc3's 10 meals (evening 20), cc2's 5 delivered meals
        // (evening 10) and the 6 potato soups cooked tonight. Only rc3 left a rating (cc2: NEVER).
        assertNextLine(StatisticsTestLogs.statsCooked(1, 21))
        assertNextLine(StatisticsTestLogs.statsServed(1, 10))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 5))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 1))
    }
}
