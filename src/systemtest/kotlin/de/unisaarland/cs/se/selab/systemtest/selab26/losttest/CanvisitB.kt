package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * we do not allow to visit
 */
class CanvisitB : ExampleSystemTestExtension() {
    override val name = "canVisitB"
    override val description = "Asserts that the group does not select a restaurant"
    override val food = "mayicome/food.json"
    override val restaurants = "mayicome/restaurants.json"
    override val scenario = "mayicome/scenario_selects.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.servingStart(evening = 1))
        skipUntilString(TickStatusTestLogs.restNoDecision(groupId = 1))
    }
}
