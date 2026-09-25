package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A CASUAL delivery group (5 km, visitingTick 10) decides on a restaurant at
 * visitingTick - ceil(distance / 5) - 3 = tick 6 (specification adjustment #10), and not again
 * at tick 10.
 *
 * The decision must be the first line of tick 6, so a decision in any other tick fails, and the
 * restaurant start must be the first line of tick 10, so a second decision there fails too.
 * Written as tester of the browsing feature (Sep 18).
 */
class CasualDeliveryEarlyDecisionTest : ExampleSystemTestExtension() {
    override val name = "CasualDeliveryEarlyDecisionTest"
    override val description = "A delivery CASUAL group decides on a restaurant well before its visitingTick"
    override val restaurants = "casualdeliveryearlydecisionjson/restaurants.json"
    override val scenario = "casualdeliveryearlydecisionjson/scenario.json"
    override val food = "casualdeliveryearlydecisionjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 10

    override suspend fun run() {
        // Tick 6: the early delivery-order tick - the group decides now.
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))

        // Tick 10, the group's actual visitingTick: a second decision would come before the
        // restaurant start line.
        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
    }
}
