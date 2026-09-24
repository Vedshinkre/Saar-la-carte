package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val G = "g"
private const val POSITIVE = "POSITIVE"
private const val NEGATIVE = "NEGATIVE"
private const val SOUP = "soup"
private const val SALAD = "salad"

/**
 * A general four-evening run mixing all three customer types: two REGULAR groups compete for
 * one table (the loser is rated negative on tick 1 of serving), a CASUAL group browses by
 * restaurant rating while another CASUAL delivery group's order is limited by pantry stock
 * shared with a REGULAR order, and an EVENT group reserved three evenings ahead visits once
 * its evening arrives.
 */
class FullSystemTest1 : ExampleSystemTestExtension() {
    override val name = "FullSystemTest1"
    override val description =
        "General multi-evening run: reservation conflict, browsing by rating, shared pantry, event visit"
    override val food = "deniztests/fullsystemtest1/food.json"
    override val restaurants = "deniztests/fullsystemtest1/restaurants.json"
    override val scenario = "deniztests/fullsystemtest1/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        assertPreparationAndReservationConflict()
        assertEveningOneArrivalsAndOrders()
        assertEveningOneFinishesAndRatings()
        assertEveningTwoRegularReturns()
        assertEventVisitOnEveningFour()
    }

    private suspend fun assertPreparationAndReservationConflict() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 20, G, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 20, G, "lettuce"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 20, G, "tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        // Group 3 loses the Bistro's single table to the lower-id group 2
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(2, 3))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 20, G, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 20, G, "tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))
    }

    private suspend fun assertEveningOneArrivalsAndOrders() {
        skipUntilString(TickStatusTestLogs.servingStart(1))
        assertNextLine(TickStatusTestLogs.tickStart(1, 1))
        // The event group reserves its table three evenings before its actual visit
        assertNextLine(TickStatusTestLogs.restDecision(6, 1))
        skipUntilString(FohArrivalTestLogs.arrival(2, 2))
        assertNextLine(FohArrivalTestLogs.seating(2, 2, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 2, 1, mapOf(SOUP to 2), 1))
        // The failed reservation is rated on tick 1 of serving, not during preparation
        skipUntilString(FohServiceTestLogs.rating(2, 3, NEGATIVE, 1, 1))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restDecision(5, 2))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 2, mapOf(SOUP to 2), 1))
        skipUntilString(FohArrivalTestLogs.ordering(2, 5, 3, mapOf(SOUP to 2), null))
        // Only two of the three delivery customers get soup: the rest of the tomato went to group 2
        assertNextLine(FohArrivalTestLogs.noOrdering(2, 5, 1))

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(4, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 4, 4, mapOf(SALAD to 2), 1))
        skipUntilString(DeliveryTestLogs.deliveryArrival(2, 1, 5, 3))
        assertNextLine(DeliveryTestLogs.deliveryFinished(2, 1, 3, 5))
    }

    private suspend fun assertEveningOneFinishesAndRatings() {
        skipUntilString(FohServiceTestLogs.finishedEating(2, 2, 2, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(2, 0, 2))
        assertNextLine(FohServiceTestLogs.escorting(2, 1, 2, 2, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(2, 1, 2))
        assertNextLine(FohServiceTestLogs.rating(2, 2, POSITIVE, 2, 1))

        skipUntilString(FohServiceTestLogs.finishedEating(1, 2, 1, 2))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 2, 1, 2))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 2))
        assertNextLine(FohServiceTestLogs.rating(1, 1, POSITIVE, 4, 0))

        skipUntilString(FohServiceTestLogs.finishedEating(1, 2, 4, 3))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 2))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 2, 4, 3))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 2))
        assertNextLine(FohServiceTestLogs.rating(1, 4, POSITIVE, 5, 0))

        skipUntilString(DeliveryTestLogs.deliveryFinishedEating(2, 5))
        skipUntilString(FohServiceTestLogs.rating(2, 5, NEGATIVE, 2, 2))
    }

    private suspend fun assertEveningTwoRegularReturns() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        skipUntilString(FohArrivalTestLogs.arrival(2, 2))
        assertNextLine(FohArrivalTestLogs.seating(2, 2, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 2, 5, mapOf(SOUP to 2), 1))

        // The casual group's second visit lands on a different, now-free table
        skipUntilString(FohArrivalTestLogs.seating(1, 4, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 4, 6, mapOf(SALAD to 2), 1))
        skipUntilString(FohServiceTestLogs.rating(2, 2, POSITIVE, 3, 2))
        skipUntilString(FohServiceTestLogs.rating(1, 4, POSITIVE, 6, 0))
    }

    private suspend fun assertEventVisitOnEveningFour() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 6))
        assertNextLine(FohArrivalTestLogs.seating(1, 6, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 6, 10, mapOf(SOUP to 5), listOf(1)))

        skipUntilString(FohServiceTestLogs.finishedEating(1, 5, 6, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 5))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 5, 6, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 5))
        assertNextLine(FohServiceTestLogs.rating(1, 6, POSITIVE, 8, 0))
    }
}
