package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 5 -- FOH service and end of tick 1 of evening 30.
 */
class AppendixScenarioTwoServiceSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioTwoServiceSystemTest"
    override val description = "Appendix scenario 2, phase 5: FOH service and end of tick."
    override val food = "appendixScenario2/food.json"
    override val restaurants = "appendixScenario2/restaurants.json"
    override val scenario = "appendixScenario2/scenario.json"
    override val logLevel = "DEBUG"

    // 29 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 1 of
    // evening 30, the first evening tonight's diagram takes place on.
    override val maxTicks = 29 * 24 + 1

    override suspend fun run() {
        // Skip past initialization, preparation, tick start, arrival, and kitchen cooking --
        // covered by earlier phases -- straight to FOH service.
        skipUntilString(TickStatusTestLogs.tickStart(1, 30))
        skipUntilString(KitchenTestLogs.kitchenStatus(1, 1, 6, 6, 6))

        // The dishes just finished cooking this same tick, so the waiter hasn't served them yet.
        assertNextLine(FohServiceTestLogs.noServing(1, 1, 6, 2))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))

        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
