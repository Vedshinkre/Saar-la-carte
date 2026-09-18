package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 3 -- FOH serving, delivery, and eating for tick 6 of evening 35.
 */
class AppendixScenarioThreeServiceSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioThreeServiceSystemTest"
    override val description = "Appendix scenario 3, phase 3: FOH serving, delivery, and eating."
    override val food = "appendixScenario3/food.json"
    override val restaurants = "appendixScenario3/restaurants.json"
    override val scenario = "appendixScenario3/scenario.json"
    override val logLevel = "DEBUG"

    // 34 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 6 of
    // evening 35, the evening tonight's diagram takes place on.
    override val maxTicks = 34 * 24 + 6

    override suspend fun run() {
        // Skip past initialization, preparation, tick start, and kitchen processing -- covered
        // by earlier phases -- straight to FOH serving.
        skipUntilString(TickStatusTestLogs.tickStart(6, 35))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 16, 0, 0))

        // ec6's order has 0 servable dishes (all 16 still cooking), so no waiter is recruited
        // and nothing is served this tick -- reflected only in the end-of-phase summary.
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))

        // All 3 delivery drivers are idle (no order handed to them yet), so processDelivery
        // produces no log lines at all this tick.

        // cc1's last two dishes -- the chicken rice ordered by the 2 of its 8 diners who
        // exclude beef and potato -- finish eating this tick (their eatingProgress reaches 0),
        // completing the group's order; the other 6 (potato soup) had already finished eating
        // in an earlier tick.
        assertNextLine(FohServiceTestLogs.finishedEating(1, 2, 1, 3))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 2))
    }
}
