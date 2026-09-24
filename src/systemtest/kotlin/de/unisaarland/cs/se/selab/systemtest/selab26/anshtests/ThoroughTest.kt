package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SPICY = "spicy noodles"
private const val NOODLE_SOUP = "noodle soup"
private const val CHICKEN_RICE = "chicken rice"
private const val TOMATO_SOUP = "tomato soup"
private const val BEEF_STEW = "beef stew"
private const val CAKE = "cream cake"
private const val TOURNANT = "TOURNANT"
private const val ROAST = "ROAST"
private const val POSITIVE = "POSITIVE"
private const val NEGATIVE = "NEGATIVE"
private const val GRAMS = "g"
private const val CHICKEN = "chicken"
private const val STAFF = "STAFF"

/**
 * whole run over 4 evenings (stops in tick 18 of evening 4)
 * R1 Lotus (ASIAN, 2 drivers), R2 Bistro (EUROPEAN, open 3-20, 1 driver), R3 Savanna (AFRICAN, no drivers)
 *
 * evening 1: events pick their restaurant in tick 1, stock runs out of noodles,
 * a SOUS cook can't take a TOURNANT only dish, delivery arrives early
 * evening 2: new waiter and smaller noodle packages, too big casual group can't decide
 * evening 3: cream unavailable and expired chicken, so groups can only partly order and one walks out
 * evening 4: event split over 3 waiters, the removed VEGETABLE cook takes lentil curry off the menu
 */
class ThoroughTest : ExampleSystemTestExtension() {
    override val name = "ThoroughTest"
    override val description = "Three restaurants over four evenings with every group type and every incident type"
    override val food = "anshtests/thorough/food.json"
    override val restaurants = "anshtests/thorough/restaurants.json"
    override val scenario = "anshtests/thorough/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 90

    override suspend fun run() {
        eveningOne()
        eveningTwo()
        eveningThree()
        eveningFour()
        statistics()
    }

    private suspend fun eveningOne() {
        // nothing known yet, only the per seat guess for every dish
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, GRAMS, CHICKEN))
        skipUntilString(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 1000, GRAMS, "beef"))
        skipUntilString(InitialAndPrepTestLogs.pantryRestocked(3))
        assertNextLine(TickStatusTestLogs.servingStart(1))

        // both events reserve in tick 1, the ASIAN/EUROPEAN tie goes to the lower id
        assertNextLine(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(10, 1))
        assertNextLine(TickStatusTestLogs.restDecision(11, 2))

        // the SOUS cook may cook noodle soup but the TOURNANT ranks lower, spicy noodles has to wait
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(20, 1))
        skipUntilString(FohArrivalTestLogs.seating(1, 20, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 20, 2, mapOf(NOODLE_SOUP to 1, SPICY to 1), 1))
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, NOODLE_SOUP, 2, listOf(2)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))
        skipUntilString(FohServiceTestLogs.noServing(1, 1, 1, 3))
        skipUntilString(FohServiceTestLogs.rating(1, 1, POSITIVE, 4, 1))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))

        // tick 5: bistro only opens now, the AFRICAN delivery finds no restaurant with drivers
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        assertNextLine(TickStatusTestLogs.restDecision(21, 2))
        assertNextLine(TickStatusTestLogs.restNoDecision(27))
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, SPICY, 2, listOf(2)))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(NOODLE_SOUP to 1, SPICY to 1), 3, 2))
        skipUntilString(FohArrivalTestLogs.arrival(2, 21))
        assertNextLine(FohArrivalTestLogs.mergingTables(2, 21, listOf(1, 2), 1))
        assertNextLine(FohArrivalTestLogs.seating(2, 21, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 21, 3, mapOf(BEEF_STEW to 2, CAKE to 5), 1))

        // delivery orders 5 ticks early, the basic dish goes to the ROAST cook
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        assertNextLine(TickStatusTestLogs.restDecision(25, 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 25, 5, mapOf(CHICKEN_RICE to 1, SPICY to 2), null))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 3, 0))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 2, ROAST, 1, CHICKEN_RICE, 5, listOf(5)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 2, SPICY, 5, listOf(5)))
        skipUntilString(FohServiceTestLogs.deliveryHandover(1, 1, mapOf(CHICKEN_RICE to 1, SPICY to 2), 1, 5))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 3))
        assertNextLine(DeliveryTestLogs.deliveryPrep(1, 1, 5, 25, 2))

        // only 200 g noodles left, the third customer falls back to chicken rice
        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        skipUntilString(FohArrivalTestLogs.seating(1, 23, 4, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 23, 6, mapOf(CHICKEN_RICE to 1, NOODLE_SOUP to 2), 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(1, 1, 7, 0))
        assertNextLine(DeliveryTestLogs.deliveryArrival(1, 1, 25, 5))
        assertNextLine(DeliveryTestLogs.deliveryFinished(1, 1, 5, 25))
        skipUntilString(FohServiceTestLogs.rating(1, 25, POSITIVE, 6, 1))

        // group 23 never rates
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 3, 23, 4))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 3))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
    }

    private suspend fun eveningTwo() {
        // noodles now come in packages of 300 g
        skipUntilString(TickStatusTestLogs.servingEnd(1))
        assertNextLine(InitialAndPrepTestLogs.incident(1, STAFF, 2))
        assertNextLine(InitialAndPrepTestLogs.incident(2, "PACKAGING", 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, GRAMS, CHICKEN))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1200, GRAMS, "noodles"))
        skipUntilString(InitialAndPrepTestLogs.pantryRestocked(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(3))

        // regular group of 3 on the BAR table of 4
        skipUntilString(FohArrivalTestLogs.arrival(2, 2))
        assertNextLine(FohArrivalTestLogs.seating(2, 2, 4, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 2, 8, mapOf(CAKE to 3), 1))

        // 9 people but only 8 SEPARATED seats at the only ASIAN restaurant
        skipUntilString(TickStatusTestLogs.tickStart(8, 2))
        assertNextLine(TickStatusTestLogs.restNoDecision(22))

        skipUntilString(TickStatusTestLogs.tickStart(9, 2))
        assertNextLine(TickStatusTestLogs.restDecision(26, 2))
        skipUntilString(FohArrivalTestLogs.ordering(2, 26, 9, mapOf(CAKE to 2), null))
        skipUntilString(FohServiceTestLogs.deliveryHandover(2, 1, mapOf(CAKE to 2), 1, 9))
        skipUntilString(DeliveryTestLogs.deliveryPrep(2, 1, 9, 26, 3))

        // arrived a tick early, even a SOME group rates that
        skipUntilString(FohServiceTestLogs.rating(2, 26, POSITIVE, 5, 0))
    }

    private suspend fun eveningThree() {
        skipUntilString(InitialAndPrepTestLogs.incident(3, "UNAVAILABLE", 3))
        assertNextLine(InitialAndPrepTestLogs.incident(4, "RECIPE", 3))
        assertNextLine(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 300, GRAMS, CHICKEN))
        skipUntilString(InitialAndPrepTestLogs.pantryRestocked(1))
        // bistro needs cream but can't buy any
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(3, 350, GRAMS, CHICKEN))

        // without cream only beef stew is left, which the first customer of group 2 excludes
        skipUntilString(FohArrivalTestLogs.arrival(2, 2))
        assertNextLine(FohArrivalTestLogs.seating(2, 2, 4, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 2, 12, mapOf(BEEF_STEW to 2), 1))
        assertNextLine(FohArrivalTestLogs.noOrdering(2, 2, 1))

        // one beef stew left for group 21, the ROAST cook is still busy
        skipUntilString(FohArrivalTestLogs.mergingTables(2, 21, listOf(1, 2), 1))
        assertNextLine(FohArrivalTestLogs.seating(2, 21, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 21, 13, mapOf(BEEF_STEW to 1), 1))
        assertNextLine(FohArrivalTestLogs.noOrdering(2, 21, 6))
        assertNextLine(FohArrivalTestLogs.seatingStatus(2, 1, 7, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(2, 1, 1))
        assertNextLine(KitchenTestLogs.kitchenStatus(2, 1, 2, 0, 0))

        // tick 9: the last customer of group 21 gives up, both groups rate negative
        skipUntilString(TickStatusTestLogs.tickStart(9, 3))
        skipUntilString(FohServiceTestLogs.noEating(2, 1, 21, 1))
        assertNextLine(FohServiceTestLogs.finishedEating(2, 2, 2, 4))
        assertNextLine(FohServiceTestLogs.eatingStatus(2, 0, 2))
        assertNextLine(FohServiceTestLogs.escorting(2, 1, 2, 2, 4))
        assertNextLine(FohServiceTestLogs.escortingStatus(2, 1, 2))
        assertNextLine(FohServiceTestLogs.rating(2, 2, NEGATIVE, 5, 1))
        assertNextLine(FohServiceTestLogs.rating(2, 21, NEGATIVE, 5, 2))
        assertNextLine(FohServiceTestLogs.ratingStatus(2, 2))

        // the abandoned stew is still finished
        skipUntilString(KitchenTestLogs.kitchenCooked(2, 1, 1, BEEF_STEW, 6))

        // bistro is in its last 3 ticks
        skipUntilString(TickStatusTestLogs.tickStart(19, 3))
        assertNextLine(TickStatusTestLogs.restNoDecision(24))
    }

    private suspend fun eveningFour() {
        skipUntilString(InitialAndPrepTestLogs.incident(5, STAFF, 4))
        assertNextLine(InitialAndPrepTestLogs.prepStart(4))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 550, GRAMS, CHICKEN))
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(1, 4000, GRAMS, CHICKEN))

        // tick 5: event of 22 on three merged tables, seated by all three waiters
        skipUntilString(TickStatusTestLogs.tickStart(5, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 10))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 10, listOf(2, 6, 7), 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 10, 2, listOf(1, 2, 3)))
        assertNextLine(
            FohArrivalTestLogs.ordering(1, 10, 17, mapOf(CHICKEN_RICE to 19, NOODLE_SOUP to 3), listOf(1, 2, 3))
        )

        // tick 6: event table waits for the chicken rice, bistro serves two tables at once
        skipUntilString(TickStatusTestLogs.tickStart(6, 4))
        skipUntilString(FohServiceTestLogs.noServing(1, 1, 3, 2))
        skipUntilString(FohArrivalTestLogs.seating(2, 11, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 11, 18, mapOf(TOMATO_SOUP to 6), 1))
        skipUntilString(KitchenTestLogs.kitchenStatus(2, 2, 9, 9, 9))
        assertNextLine(FohServiceTestLogs.serving(2, 1, mapOf(CAKE to 3), 4, 2))
        assertNextLine(FohServiceTestLogs.serving(2, 1, mapOf(TOMATO_SOUP to 6), 3, 0))
        // no VEGETABLE cook anymore, so the lentil fans take spicy noodles
        skipUntilString(FohArrivalTestLogs.ordering(3, 3, 19, mapOf(SPICY to 5), 1))

        skipUntilString(KitchenTestLogs.kitchenStatus(1, 1, 19, 19, 22))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(CHICKEN_RICE to 10), 2, 2))
        assertNextLine(FohServiceTestLogs.serving(1, 2, mapOf(CHICKEN_RICE to 9, NOODLE_SOUP to 1), 2, 2))
        assertNextLine(FohServiceTestLogs.serving(1, 3, mapOf(NOODLE_SOUP to 2), 2, 2))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 3, 22))

        skipUntilString(FohServiceTestLogs.rating(2, 2, POSITIVE, 6, 2))
        assertNextLine(FohServiceTestLogs.rating(2, 11, POSITIVE, 7, 2))

        skipUntilString(FohServiceTestLogs.escorting(1, 1, 10, 10, 2))
        assertNextLine(FohServiceTestLogs.escorting(1, 2, 10, 10, 2))
        assertNextLine(FohServiceTestLogs.escorting(1, 3, 2, 10, 2))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 3, 22))
        assertNextLine(FohServiceTestLogs.rating(1, 10, POSITIVE, 10, 1))
    }

    private suspend fun statistics() {
        // 90 ticks is not a multiple of 24, the run stops in the middle of evening 4
        skipUntilString(TickStatusTestLogs.tickStart(18, 4))
        skipUntilString(TickStatusTestLogs.restEnd(3))
        assertNextLine(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, 49))
        assertNextLine(StatisticsTestLogs.statsServed(1, 43))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 6))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 7))
        assertNextLine(StatisticsTestLogs.statsCooked(2, 24))
        assertNextLine(StatisticsTestLogs.statsServed(2, 21))
        assertNextLine(StatisticsTestLogs.statsDelivered(2, 2))
        assertNextLine(StatisticsTestLogs.statsReceived(2, 7))
        assertNextLine(StatisticsTestLogs.statsCooked(3, 10))
        assertNextLine(StatisticsTestLogs.statsServed(3, 10))
        assertNextLine(StatisticsTestLogs.statsDelivered(3, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(3, 2))
    }
}
