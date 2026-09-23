package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Browsing probe (spec adjustment #15): a CASUAL group only considers a restaurant that is open and
 * not within the last 3 ticks of its opening time. The existing last-three-ticks tests only cover
 * deliveries. Restaurant 1 closes at tick 12, so tick 9 is the last tick it can be chosen: group 1
 * (tick 9) decides and is seated, group 2 (tick 10) finds no restaurant.
 */
class BrowsingRefusesDineInInLastThreeTicksSystemTest : ExampleSystemTestExtension() {
    override val name = "BrowsingRefusesDineInInLastThreeTicksSystemTest"
    override val description = "A dine-in CASUAL group cannot choose a restaurant in its last 3 opening ticks"
    override val restaurants = "browsinglastthreeticksjson/restaurants.json"
    override val food = "browsinglastthreeticksjson/food.json"
    override val scenario = "browsinglastthreeticksjson/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(9, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))

        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        assertNextLine(TickStatusTestLogs.restNoDecision(2))
    }
}
