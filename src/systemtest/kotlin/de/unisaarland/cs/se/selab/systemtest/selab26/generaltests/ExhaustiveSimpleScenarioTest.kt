package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val GRILLED_CHICKEN = "Grilled Chicken"

/**
 * System test that exhaustively verifies a complete restaurant simulation up to Tick 5.
 *
 * This test validates the initial configuration parsing and the
 * evening preparation phase, followed by a step-by-step verification of the serving phase:
 *
 * - **Tick 1:** Event group reservation and restaurant decision.
 * - **Tick 2:** Casual group arrival, waitstaff seating, order placement, and kitchen assignment.
 * - **Tick 3:** Kitchen cooking completion and waitstaff serving operations.
 * - **Tick 4:** Customer eating phase duration.
 * - **Tick 5:** Meal completion, escorting customers outside, and collecting experience ratings.
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
        assertTick1()
        assertTick2()
        assertTick3()
        assertTick4()
        assertTick5()
    }

    private suspend fun assertTick1() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        // decision and start logs
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // Empty Statuses
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
                dishes = mapOf(GRILLED_CHICKEN to 4),
                waitstaffId = 1
            )
        )

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 4, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 4, 1))

        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "ROAST",
                meals = 4,
                dishName = GRILLED_CHICKEN,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )

        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 4, 0, 0))
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

        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 4, GRILLED_CHICKEN, 1))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 4, 4, 4))

        assertNextLine(
            FohServiceTestLogs.serving(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf("Grilled Chicken" to 4),
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
}
