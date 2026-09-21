package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A CASUAL group of 10 wants a BAR table. Restaurants 1 and 2 have identical BAR tables (10, 5, 5)
 * and identical ratings, so the tie is broken by the lowest restaurant id: the group must decide
 * on restaurant 1.
 */
class CasualBarRestaurantChoiceSystemTest : TableMergingSystemTest() {
    override val name = "CasualBarRestaurantChoiceSystemTest"
    override val description = "A CASUAL BAR group decides on restaurant 1 over the identical restaurant 2"
    override val restaurants = "tablemerging/casualbarrestaurantchoice/restaurants.json"
    override val scenario = "tablemerging/casualbarrestaurantchoice/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 3

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(TickStatusTestLogs.restDecision(1, 1))
    }
}
