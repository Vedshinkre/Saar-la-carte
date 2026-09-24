package de.unisaarland.cs.se.selab.systemtest.selab26.regulareatingescortingtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DISH = "Rice Bowl"

/**
 * Tests that a Regular group eats for exactly 2 full ticks after being served, then is escorted and rates POSITIVE.
 */
class RegularEatingTakesTwoFullTicksThenEscortsSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularEatingTakesTwoFullTicksThenEscortsSystemTest"
    override val description =
        "A Regular group eats for exactly 2 full ticks after being served, then is escorted and rates POSITIVE"
    override val logLevel = "DEBUG"
    override val restaurants = "reservationdecisionjson/positiverating/restaurants.json"
    override val food = "reservationdecisionjson/positiverating/food.json"
    override val scenario = "reservationdecisionjson/positiverating/scenario.json"
    override val maxTicks = 3

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))

        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(DISH to 2), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 2, 1))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "TOURNANT", 2, DISH, 1, listOf(1)))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 2, DISH, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 2, 2, 2))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(DISH to 2), 1, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 2))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 2, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 2, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 2, 1, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 2))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 2, 1, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 2))
        assertNextLine(FohServiceTestLogs.rating(1, 1, "POSITIVE", 1, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
