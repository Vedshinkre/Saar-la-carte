package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A Regular group rates before a Casual group in the same tick even with a higher id.
 */
class RegularRatedBeforeCasualDespiteLowerIdSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularRatedBeforeCasualDespiteLowerIdSystemTest"
    override val description =
        "A Regular group rates before a Casual group in the same tick even with a higher id"
    override val logLevel = "DEBUG"
    override val restaurants = "reservationdecisionjson/ratingpriority/restaurants.json"
    override val food = "reservationdecisionjson/ratingpriority/food.json"
    override val scenario = "reservationdecisionjson/ratingpriority/scenario.json"
    override val maxTicks = 2

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 9))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.rating(1, 9, "NEGATIVE", 0, 1))
        assertNextLine(FohServiceTestLogs.rating(1, 1, "NEGATIVE", 0, 2))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 2))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
