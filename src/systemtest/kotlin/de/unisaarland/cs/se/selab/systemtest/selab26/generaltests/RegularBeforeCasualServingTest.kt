package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A REGULAR and a CASUAL group both have fully cooked orders ready in the same tick,
 * served by the same single waiter. F19: SERVING is performed for REGULAR groups before
 * CASUAL groups, regardless of arrival order or id - REGULAR's "FOH Serving" line must
 * appear before CASUAL's, even though group 2 (CASUAL) has the lower id.
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
