package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A CASUAL group of 5 wants a COMMON table, but the only restaurant has just 4 COMMON seats
 * (its 8 BAR seats are a different table type and do not count). F25: the browsing service
 * offers no eligible restaurant, so the group logs "Restaurant No Decision" and the attempt
 * is aborted - the restaurant simulates its tick without the group ever arriving.
 */
class CasualNoDecisionNoSpaceTest : ExampleSystemTestExtension() {
    override val name = "CasualNoDecisionNoSpaceTest"
    override val description = "A CASUAL group logs no decision when the restaurant lacks seats of its table type"
    override val restaurants = "casualnodecisionnospacejson/restaurants.json"
    override val scenario = "casualnodecisionnospacejson/scenario.json"
    override val food = "casualnodecisionnospacejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        // 5 > 4 COMMON seats, so no restaurant can be chosen
        assertNextLine(TickStatusTestLogs.restNoDecision(1))
        // the restaurant carries on with its tick; the group never arrives, so nobody is seated
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
    }
}
