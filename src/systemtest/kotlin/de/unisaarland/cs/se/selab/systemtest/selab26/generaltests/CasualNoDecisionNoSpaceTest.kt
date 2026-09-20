package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A CASUAL group of 9 wants a BAR table, but the three matching restaurants only have 8, 2 and 5
 * BAR seats (their COMMON seats are a different table type and do not count). F25: the browsing
 * service offers no eligible restaurant, so the group logs "Restaurant No Decision" and the attempt
 * is aborted - the restaurants simulate their tick without the group ever arriving.
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
        assertNextLine(TickStatusTestLogs.restNoDecision(1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
