package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 2 -- kitchen processing for tick 6 of evening 35.
 */
class AppendixScenarioThreeKitchenSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioThreeKitchenSystemTest"
    override val description = "Appendix scenario 3, phase 2: kitchen processing with no dish finishing."
    override val food = "appendixScenario3/food.json"
    override val restaurants = "appendixScenario3/restaurants.json"
    override val scenario = "appendixScenario3/scenario.json"
    override val logLevel = "DEBUG"

    // 34 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 6 of
    // evening 35, the evening tonight's diagram takes place on.
    override val maxTicks = 34 * 24 + 6

    override suspend fun run() {
        // Skip past initialization, preparation, and tick start -- covered by
        // AppendixScenarioThreeTickStartSystemTest -- straight to the tick.
        skipUntilString(TickStatusTestLogs.tickStart(6, 35))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // The only order in the queue is ec6's 16-dish potato soup order, already assigned to
        // cook c1 (the sole TOURNANT). Its remaining-ticks counter reaches 0 this tick, but the
        // batch itself hasn't finished cooking yet: still 16 meals cooking, 0 finished, 0
        // servable, so mealsCooked stays at its prior total of 8 and no new cook is assigned.
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 16, 0, 0))
    }
}
