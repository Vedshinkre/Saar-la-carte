package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val MEAL_A = "mealA"
private const val MEAL_B = "mealB"
private const val MEAL_C = "mealC"
private const val TOURNANT2 = "TOURNANT"

/**
 * Validates partial service rules with a 3-person group:
 * Group 1 arrives at Tick 1 and orders three different meals.
 * The first meal is served at Tick 5, granting a 2-tick patience extension.
 * The second meal is served at Tick 7.
 * At Tick 7, the third unserved customer times out and leaves.
 * The group finishes eating and leaves by Tick 9, rating the restaurant NEGATIVELY.
 */
class CorrectPartialServing2 : ExampleSystemTestExtension() {
    override val name = "CorrectPartialServing2"
    override val description = "3-person group receives partial food, extends patience,exhaustive"
    override val restaurants = "partaildeliveryjson/restaurants.json"
    override val scenario = "partaildeliveryjson/scenario.json"
    override val food = "partaildeliveryjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    override suspend fun run() {
        assertPrepPhase()
        assertTick1()
        assertTick2()
        assertTick3()
        assertTick4()
        assertTick5()
        assertTick6()
        assertTick7()
        assertTick8()
        assertTick9()
        assertTick10()
        assertTick11()
        assertTick12()
        assertFinalStatistics()
    }

    private suspend fun assertPrepPhase() {
        // Skip JSON parsing logs directly to the preparation phase
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))

        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, "g", "chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        assertNextLine(TickStatusTestLogs.servingStart(1))
    }

    private suspend fun assertTick1() {
        assertNextLine(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(MEAL_A to 1, MEAL_B to 1, MEAL_C to 1), 1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 3, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 3, 1))

        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT2, 1, MEAL_A, 1, listOf(1)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))

        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick2() {
        assertNextLine(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))

        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick3() {
        assertNextLine(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, MEAL_A, 2))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 1, 1))

        assertNextLine(FohServiceTestLogs.noServing(1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick4() {
        assertNextLine(TickStatusTestLogs.tickStart(4, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT2, 1, MEAL_B, 1, listOf(1)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 1))

        assertNextLine(FohServiceTestLogs.noServing(1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick5() {
        assertNextLine(TickStatusTestLogs.tickStart(5, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 1))

        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(MEAL_A to 1), 1, 4))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 1, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick6() {
        assertNextLine(TickStatusTestLogs.tickStart(6, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))

        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 1, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick7() {
        assertNextLine(TickStatusTestLogs.tickStart(7, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, MEAL_B, 6))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 1, 1))

        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(MEAL_B to 1), 1, 6))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 1))

        assertNextLine(FohServiceTestLogs.noEating(1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 1, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick8() {
        assertNextLine(TickStatusTestLogs.tickStart(8, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT2, 1, MEAL_C, 1, listOf(1)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))

        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 1, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick9() {
        assertNextLine(TickStatusTestLogs.tickStart(9, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))

        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))

        assertNextLine(FohServiceTestLogs.finishedEating(1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 1))

        assertNextLine(FohServiceTestLogs.escorting(1, 1, 2, 1, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 2))

        assertNextLine(FohServiceTestLogs.rating(1, 1, "NEGATIVE", 5, 1))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick10() {
        assertNextLine(TickStatusTestLogs.tickStart(10, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick11() {
        assertNextLine(TickStatusTestLogs.tickStart(11, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, MEAL_C, 10))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 1, 1))

        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick12() {
        assertNextLine(TickStatusTestLogs.tickStart(12, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 1))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertFinalStatistics() {
        assertNextLine(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, 3))
        assertNextLine(StatisticsTestLogs.statsServed(1, 2))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 1))
    }
}
