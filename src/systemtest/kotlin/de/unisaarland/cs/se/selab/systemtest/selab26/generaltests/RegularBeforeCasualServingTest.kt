package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * REGULAR group 1 (table 1) and CASUAL group 2 (table 2) both have a complete order in tick 1 and
 * share one waiter. The serving lines must come REGULAR first, then CASUAL (specification page 35,
 * line 4), followed directly by the serving status.
 *
 * In this fixture the REGULAR group also has the lower group id and the lower table id, so the
 * expected order is the same as serving by id. Written as tester of the serving feature (Sep 18).
 */
class RegularBeforeCasualServingTest : ExampleSystemTestExtension() {
    override val name = "RegularBeforeCasualServingTest"
    override val description = "REGULAR groups are served before CASUAL groups in the same tick"
    override val restaurants = "regularbeforecasualservingjson/restaurants.json"
    override val scenario = "regularbeforecasualservingjson/scenario.json"
    override val food = "regularbeforecasualservingjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf("Rice Bowl" to 3), 1, 0))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf("Rice Bowl" to 2), 2, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 5))
    }
}
