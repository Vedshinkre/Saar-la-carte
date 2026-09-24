package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val BURGER = "burger"
private const val FISH = "fish and chips"
private const val FRIES = "fries"
private const val PIE = "apple pie"
private const val CHEESE_FRIES = "cheese fries"
private const val STEAK = "steak"
private const val POSITIVE = "POSITIVE"
private const val NEGATIVE = "NEGATIVE"
private const val GRAMS = "g"
private const val EXEC = "EXEC"
private const val STAFF = "STAFF"

/**
 * two restaurant run over 5 full evenings (120 ticks), both AMERICAN
 * R1 Harbor: open all evening, 3 waiters, 3 drivers, rating 5
 * R2 Grill House: only open 4-18, 2 waiters, 1 driver, rating 12, so it wins whenever it's open
 *
 * lots of groups at once to push the waiter rules: most loaded waiter under 10 first,
 * the per tick seating limit, the fallback to the least loaded one and groups with a waiter but no table
 * kitchen batches the same dish over orders and falls back to higher ranked cooks when the usual one is busy
 * R2 closes with a group still eating, events are split over the waiters
 */
class EnormousTest : ExampleSystemTestExtension() {
    override val name = "EnormousTest"
    override val description = "Two busy restaurants over five evenings stressing waiters, cooks, drivers and events"
    override val food = "anshtests/enormous/food.json"
    override val restaurants = "anshtests/enormous/restaurants.json"
    override val scenario = "anshtests/enormous/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 120

    override suspend fun run() {
        eveningOne()
        eveningTwo()
        eveningThree()
        eveningsFourAndFive()
        statistics()
    }

    private suspend fun eveningOne() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(1, 5000, GRAMS, "potato"))

        // event of 18 COMMON: R2 has the better rating but only 16 COMMON seats
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(3, 1))

        // tick 2: R2 still closed, waiter 1 takes everyone until the seating limit is hit
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restDecision(5, 1))
        skipUntilString(FohArrivalTestLogs.seating(1, 5, 4, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 5, 1, mapOf(PIE to 2, CHEESE_FRIES to 2), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 6))
        assertNextLine(FohArrivalTestLogs.seating(1, 6, 5, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 6, 2, mapOf(CHEESE_FRIES to 4), 1))
        // a single person doesn't fill 3/4 of a table of 4
        assertNextLine(FohArrivalTestLogs.arrival(1, 7))
        assertNextLine(FohArrivalTestLogs.noSeatingNoTable(1, 1, 7))
        assertNextLine(FohArrivalTestLogs.arrival(1, 8))
        assertNextLine(FohArrivalTestLogs.seating(1, 8, 7, listOf(1)))
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 1, 10, 3))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 10, 1))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "PASTRY", 2, PIE, 1, listOf(1)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 2, EXEC, 8, CHEESE_FRIES, 1, listOf(1, 2, 3)))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 2, 8, CHEESE_FRIES, 0))
        skipUntilString(FohServiceTestLogs.noServing(1, 1, 2, 4))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(CHEESE_FRIES to 4), 5, 0))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(CHEESE_FRIES to 2), 7, 0))
        skipUntilString(FohServiceTestLogs.rating(1, 7, NEGATIVE, 5, 1))

        // tick 3: the BAR seats are used up by the earlier decisions, waiter 1 (load 10) only gets the fallback group
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(9, 1))
        assertNextLine(TickStatusTestLogs.restDecision(10, 1))
        assertNextLine(TickStatusTestLogs.restNoDecision(11))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 3, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 4, mapOf(FISH to 6), 2))
        assertNextLine(FohArrivalTestLogs.arrival(1, 9))
        assertNextLine(FohArrivalTestLogs.seating(1, 9, 6, listOf(3)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 9, 5, mapOf(CHEESE_FRIES to 6), 3))
        assertNextLine(FohArrivalTestLogs.arrival(1, 10))
        assertNextLine(FohArrivalTestLogs.seating(1, 10, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 10, 6, mapOf(PIE to 5, CHEESE_FRIES to 4), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 3, 21, 3))

        // tick 5: the SOUS cook gets its id last, two waiters hand the fries to the same driver
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        assertNextLine(TickStatusTestLogs.restDecision(20, 2))
        assertNextLine(TickStatusTestLogs.restDecision(21, 1))
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 4, "SOUS", 2, FRIES, 7, listOf(7)))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(PIE to 5, CHEESE_FRIES to 4), 1, 2))
        assertNextLine(FohServiceTestLogs.deliveryHandover(1, 1, mapOf(FRIES to 1), 1, 7))
        assertNextLine(FohServiceTestLogs.deliveryHandover(1, 2, mapOf(FRIES to 1), 1, 7))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 2, 17))
        assertNextLine(DeliveryTestLogs.deliveryPrep(1, 1, 7, 21, 1))
        skipUntilString(KitchenTestLogs.kitchenAssign(2, 1, "ROAST", 3, STEAK, 8, listOf(8)))

        // tick 6: ROAST is busy with the delivery steaks, so the EXEC takes the next ones
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        assertNextLine(TickStatusTestLogs.restDecision(14, 2))
        skipUntilString(FohArrivalTestLogs.ordering(2, 14, 9, mapOf(STEAK to 8), 1))
        skipUntilString(KitchenTestLogs.kitchenAssign(2, 2, EXEC, 8, STEAK, 9, listOf(9)))
        assertNextLine(KitchenTestLogs.kitchenStatus(2, 2, 11, 0, 0))

        skipUntilString(FohServiceTestLogs.escorting(1, 2, 6, 1, 3))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 9, 10, 1))
        skipUntilString(FohServiceTestLogs.deliveryHandover(2, 1, mapOf(STEAK to 3), 1, 8))
        skipUntilString(DeliveryTestLogs.deliveryFinished(2, 1, 8, 20))
        skipUntilString(FohServiceTestLogs.rating(2, 20, POSITIVE, 14, 0))
    }

    private suspend fun eveningTwo() {
        // fish only keeps for one evening and can't be bought now
        skipUntilString(InitialAndPrepTestLogs.incident(1, STAFF, 2))
        assertNextLine(InitialAndPrepTestLogs.incident(2, "UNAVAILABLE", 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 1100, GRAMS, "fish"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 5000, GRAMS, "potato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))
        assertNextLine(TickStatusTestLogs.servingStart(2))
        // the evening 5 event books three evenings ahead
        assertNextLine(TickStatusTestLogs.tickStart(1, 2))
        assertNextLine(TickStatusTestLogs.restDecision(4, 1))

        skipUntilString(FohArrivalTestLogs.noSeatingNoTable(1, 1, 7))

        // regular and delivery order steak in the same tick, one ROAST batch for both; the new TOURNANT does the fries
        skipUntilString(FohArrivalTestLogs.ordering(2, 2, 11, mapOf(FRIES to 3, STEAK to 5), 1))
        assertNextLine(FohArrivalTestLogs.ordering(2, 20, 12, mapOf(STEAK to 3), null))
        assertNextLine(FohArrivalTestLogs.seatingStatus(2, 1, 8, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(2, 11, 1))
        assertNextLine(KitchenTestLogs.kitchenAssign(2, 1, "TOURNANT", 3, FRIES, 11, listOf(11)))
        assertNextLine(KitchenTestLogs.kitchenAssign(2, 2, "ROAST", 8, STEAK, 11, listOf(11, 12)))

        // R2 has nothing for the two who avoid patty and potato, R1 has no fitting SEPARATED table
        skipUntilString(TickStatusTestLogs.tickStart(7, 2))
        assertNextLine(TickStatusTestLogs.restDecision(13, 1))
        skipUntilString(FohArrivalTestLogs.noSeatingNoTable(1, 1, 13))
        skipUntilString(FohServiceTestLogs.serving(2, 1, mapOf(FRIES to 3, STEAK to 5), 1, 2))
        assertNextLine(FohServiceTestLogs.deliveryHandover(2, 1, mapOf(STEAK to 2), 1, 12))
        assertNextLine(FohServiceTestLogs.deliveryHandover(2, 2, mapOf(STEAK to 1), 1, 12))
        assertNextLine(FohServiceTestLogs.servingStatus(2, 2, 11))

        // tick 15 is the last tick R2 takes guests, they are still eating when it closes at 18
        skipUntilString(TickStatusTestLogs.tickStart(15, 2))
        assertNextLine(TickStatusTestLogs.restDecision(12, 2))
        skipUntilString(FohArrivalTestLogs.seating(2, 12, 2, listOf(1)))
        skipUntilString(FohServiceTestLogs.serving(2, 1, mapOf(STEAK to 6), 2, 2))
        skipUntilString(TickStatusTestLogs.tickStart(18, 2))
        skipUntilString(FohServiceTestLogs.eatingStatus(2, 6, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(2, 0, 0))
        assertNextLine(FohServiceTestLogs.rating(2, 12, NEGATIVE, 16, 1))
    }

    private suspend fun eveningThree() {
        skipUntilString(InitialAndPrepTestLogs.incident(3, STAFF, 3))
        assertNextLine(InitialAndPrepTestLogs.incident(4, "PACKAGING", 3))
        skipUntilString(InitialAndPrepTestLogs.pantryRemoved(1, 50, "X", "patty"))

        // no fish again, the fish fans take the highest id
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, 15, mapOf(CHEESE_FRIES to 6), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 10))
        assertNextLine(FohArrivalTestLogs.seating(1, 10, 1, listOf(2)))
        skipUntilString(FohArrivalTestLogs.noSeatingNoTable(1, 1, 11))

        // R2 lost its only driver, so the delivery goes to R1 even with the worse rating
        skipUntilString(TickStatusTestLogs.tickStart(14, 3))
        assertNextLine(TickStatusTestLogs.restDecision(22, 1))
        skipUntilString(FohServiceTestLogs.deliveryHandover(1, 1, mapOf(CHEESE_FRIES to 5), 1, 18))
        skipUntilString(DeliveryTestLogs.deliveryPrep(1, 1, 18, 22, 3))
        skipUntilString(DeliveryTestLogs.deliveryDriving(1, 1, 15, 0))
        assertNextLine(DeliveryTestLogs.deliveryArrival(1, 1, 22, 18))
        skipUntilString(FohServiceTestLogs.rating(2, 12, NEGATIVE, 17, 2))
        skipUntilString(FohServiceTestLogs.rating(1, 22, POSITIVE, 15, 4))
    }

    private suspend fun eveningsFourAndFive() {
        skipUntilString(InitialAndPrepTestLogs.incident(5, "RECIPE", 4))
        assertNextLine(InitialAndPrepTestLogs.prepStart(4))
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(1, 2000, GRAMS, "fish"))

        // event 3: tables 1, 3 and 5 give 20 seats, cheese haters get the pie until the sugar runs out
        skipUntilString(TickStatusTestLogs.tickStart(6, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 3, listOf(1, 3, 5), 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 1, listOf(1, 2)))
        assertNextLine(
            FohArrivalTestLogs.ordering(1, 3, 22, mapOf(PIE to 2, BURGER to 14, FRIES to 2), listOf(1, 2))
        )
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 3, "SOUS", 14, BURGER, 22, listOf(22)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 2, EXEC, 2, FRIES, 22, listOf(22)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "PASTRY", 2, PIE, 22, listOf(22)))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(BURGER to 10), 1, 1))
        assertNextLine(FohServiceTestLogs.serving(1, 2, mapOf(PIE to 2, BURGER to 4, FRIES to 2), 1, 1))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 10, 3, 1))
        assertNextLine(FohServiceTestLogs.escorting(1, 2, 8, 3, 1))

        skipUntilString(InitialAndPrepTestLogs.incident(6, STAFF, 5))
        assertNextLine(InitialAndPrepTestLogs.prepStart(5))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 720, GRAMS, "cheese"))

        // SEPARATED is reserved for event 4 tonight, so group 13 finds nothing at all
        skipUntilString(TickStatusTestLogs.tickStart(7, 5))
        assertNextLine(TickStatusTestLogs.restNoDecision(13))
        skipUntilString(FohArrivalTestLogs.seating(1, 4, 8, listOf(1, 2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 4, 25, mapOf(BURGER to 12), listOf(1, 2)))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(BURGER to 10), 8, 1))
        assertNextLine(FohServiceTestLogs.serving(1, 2, mapOf(BURGER to 2), 8, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 4, POSITIVE, 19, 4))
    }

    private suspend fun statistics() {
        // 120 ticks end right with the serving phase, no preparation for evening 6
        skipUntilString(TickStatusTestLogs.tickStart(24, 5))
        skipUntilString(TickStatusTestLogs.servingEnd(5))
        assertNextLine(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, 97))
        assertNextLine(StatisticsTestLogs.statsServed(1, 90))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 7))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 18))
        assertNextLine(StatisticsTestLogs.statsCooked(2, 58))
        assertNextLine(StatisticsTestLogs.statsServed(2, 52))
        assertNextLine(StatisticsTestLogs.statsDelivered(2, 6))
        assertNextLine(StatisticsTestLogs.statsReceived(2, 9))
    }
}
