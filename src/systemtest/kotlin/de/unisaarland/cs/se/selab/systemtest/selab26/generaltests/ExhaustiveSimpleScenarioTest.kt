package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val GRILLED_CHICKEN = "Grilled Chicken"
private const val TOMATO_SOUP = "Tomato Soup"

/**
 * System test that exhaustively verifies a complete restaurant simulation up to Tick 5.
 *
 * This test validates the initial configuration parsing, the evening preparation phase,
 * a step-by-step verification of the serving phase, and the final simulation statistics.
 */
class ExhaustiveSimpleScenarioTest : ExampleSystemTestExtension() {
    override val name = "ExhaustiveScenarioTest"
    override val description = "Tests the full scenario from initialization through Tick 5 exhaustively"
    override val restaurants = "simplejson/restaurants.json"
    override val scenario = "simplejson/scenario.json"
    override val food = "simplejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        assertInitializationAndPrep()
        assertTick1()
        assertTick2()
        assertTick3()
        assertTick4()
        assertTick5()
        assertFinalStatistics()
    }

    private suspend fun assertInitializationAndPrep() {
        //  Preparation Phase
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))

        // Note: Ingredients are procured in alphabetical order! (Chicken before Tomato)
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 500, "g", "Chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, "g", "Tomato"))

        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        // 3. Serving Phase Start
        assertNextLine(TickStatusTestLogs.servingStart(1))
    }

    private suspend fun assertTick1() {
        assertNextLine(TickStatusTestLogs.tickStart(1, 1))

        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
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

    private suspend fun assertTick2() {
        assertNextLine(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 1, listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 2,
                orderId = 1,
                dishes = mapOf(GRILLED_CHICKEN to 2, TOMATO_SOUP to 2),
                waitstaffId = 1
            )
        )

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 4, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 4, 1))

        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "TOURNANT",
                meals = 2,
                dishName = TOMATO_SOUP,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 2,
                cookType = "ROAST",
                meals = 2,
                dishName = GRILLED_CHICKEN,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )

        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 2, TOMATO_SOUP, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 4, 2, 2))

        assertNextLine(FohServiceTestLogs.noServing(1, 1, 2, 1))
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

        assertNextLine(KitchenTestLogs.kitchenCooked(1, 2, 2, GRILLED_CHICKEN, 1))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 2, 2, 4))

        assertNextLine(
            FohServiceTestLogs.serving(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(GRILLED_CHICKEN to 2, TOMATO_SOUP to 2),
                tableId = 1,
                ticks = 1
            )
        )

        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 4))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 4, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick4() {
        assertNextLine(TickStatusTestLogs.tickStart(4, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 4, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTick5() {
        assertNextLine(TickStatusTestLogs.tickStart(5, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))

        assertNextLine(FohServiceTestLogs.finishedEating(1, 4, 2, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 4))

        assertNextLine(FohServiceTestLogs.escorting(1, 1, 4, 2, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 4))

        assertNextLine(
            FohServiceTestLogs.rating(
                restId = 1,
                groupId = 2,
                rating = "POSITIVE",
                pos = 11,
                neg = 3
            )
        )

        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertFinalStatistics() {
        // Because maxTicks = 5, the serving phase ends immediately after Tick 5 completes.
        assertNextLine(TickStatusTestLogs.servingEnd(1)) // DOTO: wrong ~Ciprian

        // Final Global Statistics
        assertNextLine(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, 4))
        assertNextLine(StatisticsTestLogs.statsServed(1, 4))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 1))
    }
}
