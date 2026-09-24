package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val POSITIVE = "POSITIVE"
private const val NEGATIVE = "NEGATIVE"

/**
 * CASUAL browsing across three restaurants: the best-rated restaurant is chosen even though its
 * two BAR tables can never merge for a group too big for either alone, a dine-in group is seated
 * there fine, a delivery group is forced to the only restaurant with a driver regardless of
 * rating, and a never-rating group never shows up in the rating log. An EVENT visit rounds it out.
 */
class FullSystemTest3 : ExampleSystemTestExtension() {
    override val name = "FullSystemTest3"
    override val description =
        "Casual browsing by rating, non-mergeable BAR tables, driver-only restaurant, never-rates group"
    override val food = "deniztests/fullsystemtest3/food.json"
    override val restaurants = "deniztests/fullsystemtest3/restaurants.json"
    override val scenario = "deniztests/fullsystemtest3/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        assertBarGroupTurnedAwayFromTopRatedRestaurant()
        assertDineInAndDeliveryOrders()
        assertEveningOneFinishesAndRatings()
        assertEventVisitOnEveningFour()
    }

    private suspend fun assertBarGroupTurnedAwayFromTopRatedRestaurant() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(3, 2))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        // Restaurant 1 is rated highest, but its two BAR tables (size 4 each) never merge,
        // so the size-5 BAR group cannot be seated on either one alone
        assertNextLine(FohArrivalTestLogs.noSeatingNoTable(1, 1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 1, NEGATIVE, 10, 1))
    }

    private suspend fun assertDineInAndDeliveryOrders() {
        skipUntilString(FohArrivalTestLogs.ordering(2, 3, 2, mapOf("stew" to 3), null))

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 3, mapOf("salad" to 2), 1))

        skipUntilString(DeliveryTestLogs.deliveryArrival(2, 1, 3, 2))
        assertNextLine(DeliveryTestLogs.deliveryFinished(2, 1, 2, 3))
        skipUntilString(FohServiceTestLogs.rating(2, 4, POSITIVE, 3, 0))
    }

    private suspend fun assertEveningOneFinishesAndRatings() {
        skipUntilString(FohServiceTestLogs.finishedEating(1, 2, 2, 3))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 2))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 2, 2, 3))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 2))
        assertNextLine(FohServiceTestLogs.rating(1, 2, POSITIVE, 11, 1))

        // Group 3's rating likelihood is NEVER: it finishes eating but never rates
        skipUntilString(DeliveryTestLogs.deliveryFinishedEating(2, 3))
    }

    private suspend fun assertEventVisitOnEveningFour() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(3, 5))
        assertNextLine(FohArrivalTestLogs.seating(3, 5, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(3, 5, 7, mapOf("soup" to 6), listOf(1)))

        skipUntilString(FohServiceTestLogs.rating(2, 4, POSITIVE, 6, 0))
        skipUntilString(FohServiceTestLogs.finishedEating(3, 6, 5, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(3, 0, 6))
        assertNextLine(FohServiceTestLogs.escorting(3, 1, 6, 5, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(3, 1, 6))
        assertNextLine(FohServiceTestLogs.rating(3, 5, POSITIVE, 2, 0))
    }
}
