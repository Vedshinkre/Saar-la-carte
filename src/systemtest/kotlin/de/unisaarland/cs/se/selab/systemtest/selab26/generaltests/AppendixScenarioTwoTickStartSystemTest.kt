package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 2 -- tick start and restaurant decisions for tick 1 of evening 30.
 */
class AppendixScenarioTwoTickStartSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioTwoTickStartSystemTest"
    override val description = "Appendix scenario 2, phase 2: tick start and restaurant decisions."
    override val food = "appendixScenario2/food.json"
    override val restaurants = "appendixScenario2/restaurants.json"
    override val scenario = "appendixScenario2/scenario.json"
    override val logLevel = "DEBUG"

    // 29 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 1 of
    // evening 30, the first evening tonight's diagram takes place on.
    override val maxTicks = 29 * 24 + 1

    override suspend fun run() {
        // Skip past initialization and preparation for evening 30 -- covered by
        // AppendixScenarioTwoPreparationSystemTest -- straight to the tick.
        skipUntilString(InitialAndPrepTestLogs.prepStart(30))
        skipUntilString(InitialAndPrepTestLogs.pantryRestocked(1))

        assertNextLine(TickStatusTestLogs.servingStart(30))
        assertNextLine(TickStatusTestLogs.tickStart(1, 30))

        // cc1 (EUROPEAN/AFRICAN, dine-in) and cc2 (EUROPEAN/ASIAN, delivery) both match
        // restaurant 1's EUROPEAN menu; cc5 (AMERICAN only) has no eligible restaurant.
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertNextLine(TickStatusTestLogs.restNoDecision(5))

        assertNextLine(TickStatusTestLogs.restStart(1))
    }
}
