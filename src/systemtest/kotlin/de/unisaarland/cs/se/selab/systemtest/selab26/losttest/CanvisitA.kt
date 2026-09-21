package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * can visit even if not psosible
 */
class CanvisitA : ExampleSystemTestExtension() {
    override val name = "canVisitA"
    override val description = "Asserts that the group does selectrestaurant 1"
    override val food = "mayicome/food.json"
    override val restaurants = "mayicome/restaurants.json"
    override val scenario = "mayicome/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.servingStart(evening = 1))
        skipUntilString(TickStatusTestLogs.restDecision(groupId = 1, restId = 1))
    }
}
