package de.unisaarland.cs.se.selab.systemtest.selab26.regulareatingescortingtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DISH = "Rice Bowl"

/**
 * Tests that two Regular groups finishing eating in the same tick are logged in ascending id order, not file order.
 */
class RegularEatingEscortingOrderedByIdNotFileOrderSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularEatingEscortingOrderedByIdNotFileOrderSystemTest"
    override val description =
        "Two Regular groups finishing eating in the same tick are logged in ascending id order, not file order"
    override val logLevel = "DEBUG"
    override val restaurants = "regulareatingescortingtests/restaurants.json"
    override val food = "regulareatingescortingtests/food.json"
    override val scenario = "regulareatingescortingtests/scenario.json"
    override val maxTicks = 3

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))

        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 4, 1, mapOf(DISH to 2), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 9))
        assertNextLine(FohArrivalTestLogs.seating(1, 9, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 9, 2, mapOf(DISH to 2), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 4, 2))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 4, 1))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "TOURNANT", 4, DISH, 1, listOf(1, 2)))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 4, DISH, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 4, 4, 4))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(DISH to 2), 1, 0))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(DISH to 2), 2, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 4))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 4, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 2, 4, 1))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 2, 9, 2))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 4))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 2, 4, 1))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 2, 9, 2))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 4))
        assertNextLine(FohServiceTestLogs.rating(1, 4, "POSITIVE", 1, 0))
        assertNextLine(FohServiceTestLogs.rating(1, 9, "POSITIVE", 2, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 2))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
