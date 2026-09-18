package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 1 -- tick start for tick 6 of evening 35, deep into the serving phase.
 */
class AppendixScenarioThreeTickStartSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioThreeTickStartSystemTest"
    override val description = "Appendix scenario 3, phase 1: tick start for tick 6 of evening 35."
    override val food = "appendixScenario3/food.json"
    override val restaurants = "appendixScenario3/restaurants.json"
    override val scenario = "appendixScenario3/scenario.json"
    override val logLevel = "DEBUG"

    // 34 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 6 of
    // evening 35, the evening tonight's diagram takes place on.
    override val maxTicks = 34 * 24 + 6

    override suspend fun run() {
        // "Tick 6 (35)" is logged exactly once in the whole run, so this alone locates the
        // diagram's starting point -- no earlier phase test to chain from since the diagram
        // begins mid-evening, well after preparation and arrival for evening 35.
        skipUntilString(TickStatusTestLogs.tickStart(6, 35))

        // Neither cc2 nor cc5 are visiting on evening 35 at all (their visitingEvenings are
        // [10, 30] and [30, 34] respectively) and cc1's visitingTick is 1, not 6, so no group
        // arrives, is seated, or orders this tick -- straight to restaurant 1's tick processing.
        assertNextLine(TickStatusTestLogs.restStart(1))
    }
}
