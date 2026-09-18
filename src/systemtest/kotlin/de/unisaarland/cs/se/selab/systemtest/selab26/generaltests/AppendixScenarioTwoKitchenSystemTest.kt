package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 4 -- kitchen assignment and cooking for tick 1 of evening 30.
 */
class AppendixScenarioTwoKitchenSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioTwoKitchenSystemTest"
    override val description = "Appendix scenario 2, phase 4: kitchen assignment and cooking."
    override val food = "appendixScenario2/food.json"
    override val restaurants = "appendixScenario2/restaurants.json"
    override val scenario = "appendixScenario2/scenario.json"
    override val logLevel = "DEBUG"

    // 29 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 1 of
    // evening 30, the first evening tonight's diagram takes place on.
    override val maxTicks = 29 * 24 + 1

    private val potatoSoup = "potato soup"

    override suspend fun run() {
        // Skip past initialization, preparation, tick start, and FOH arrival/ordering --
        // covered by earlier phases -- straight to the kitchen's tick processing.
        skipUntilString(FohArrivalTestLogs.orderingStatus(1, 0, 1))

        // Only one TOURNANT cook is on staff, so the potato soup (basic dish for EUROPEAN)
        // is picked and cooked first, ahead of the beef pasta and chicken rice in the order.
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "TOURNANT", 3, potatoSoup, 2, listOf(2)))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 3, potatoSoup, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 3, 3, 3))
    }
}
