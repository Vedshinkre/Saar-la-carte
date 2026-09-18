package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A CASUAL delivery group (distance 5, visitingTick 10) must decide on a restaurant early,
 * at visitingTick - ceil(distance/5) - 3 = 10 - 1 - 3 = tick 6 - not at tick 10 itself.
 * F25: the "Restaurant Decision" log fires at tick 6, and the group does not reconsider or
 * decide again at its actual visitingTick.
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
        // Ticks 1-5: the group is not being considered for a decision yet.
        for (tick in 1..5) {
            skipUntilString(TickStatusTestLogs.tickStart(tick, 1))
        }

        // Tick 6: the early delivery-order tick - the group decides now.
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))

        // Tick 10, the group's actual visitingTick: no further restaurant decision log for
        // this group - they already decided and do not reconsider. If one fired here,
        // this assertion would fail with a "Restaurant Decision" line instead.
        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
    }
}
