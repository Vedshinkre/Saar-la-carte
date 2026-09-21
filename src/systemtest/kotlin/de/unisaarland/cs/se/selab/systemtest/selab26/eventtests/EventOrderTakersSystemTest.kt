package de.unisaarland.cs.se.selab.systemtest.selab26.eventtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "Tomato Soup"

/**
 * P03 (spec 2.2 "Front of house", forum "Deterministic waitstaff order for EVENT group ordering"): the
 * customers of an EVENT group are seated in the fixed ordering sequence (most excluded ingredients,
 * then fewest favorite dishes, then JSON order), each waiter seats the next block of it, and takes the
 * orders of exactly the customers of his block. A customer who finds no dish leaves and his slot stays
 * empty, nobody moves to another waiter. A waiter whose customers all fail to order takes no orders, so
 * he is neither named in the ordering log nor counted in the ordering status.
 *
 * Restaurant 1 has three waiters and two tables (8 and 16 seats), in tick 2 of evening 4:
 * - REGULAR 2 (8 customers) is seated by waiter 1 on table 1. Only 3 customers find a dish (Tomato
 *   Soup), the other 5 leave. Waiter 1 has current load 3
 * - EVENT 1 (15 customers) is seated on table 2 by the waiters with the highest current load first:
 *   waiter 1 seats the first 2 customers of the sequence, waiter 2 the next 10, waiter 3 the last 3
 * - The 12 customers who each exclude four ingredients that no dish contains come first in the
 *   sequence and order the soup. The last 3 customers exclude the ingredients of every dish that can be
 *   cooked and belong to waiter 3: all of them leave, waiter 3 takes no order
 *
 * The scenario is checked in independent chunks, so that a failure points at a single rule.
 */
abstract class EventOrderTakersScenario : ExampleSystemTestExtension() {
    override val restaurants = "eventtests/orderingwaiters/restaurants.json"
    override val scenario = "eventtests/orderingwaiters/scenario.json"
    override val food = "eventtests/orderingwaiters/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 80
}

/** Setup: REGULAR 2 orders for 3 customers and 5 leave, the EVENT group is seated by all three waiters. */
class EventOrderTakersSetupSystemTest : EventOrderTakersScenario() {
    override val name = "EventOrderTakersSetupSystemTest"
    override val description = "Setup of the order-takers scenario: the EVENT group is seated by three waiters"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 1, mapOf(SOUP to 3), 1))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 2, 5))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1, 2, 3)))
    }
}

/** All customers of waiter 3 fail to order: only waiters 1 and 2 are named in the order, the 3 leave. */
class EventOrderTakersOrderingLogSystemTest : EventOrderTakersScenario() {
    override val name = "EventOrderTakersOrderingLogSystemTest"
    override val description = "A waiter whose EVENT customers all fail to order is not named in the ordering log"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 2, listOf(1, 2, 3)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 2, mapOf(SOUP to 12), listOf(1, 2)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 1, 3))
    }
}

/** The ordering status counts the waiters who took orders: waiters 1 and 2, not waiter 3. */
class EventOrderTakersOrderingStatusSystemTest : EventOrderTakersScenario() {
    override val name = "EventOrderTakersOrderingStatusSystemTest"
    override val description = "The ordering status counts the waitstaff who took orders, not those who only seated"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.noOrdering(1, 1, 3))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 3, 23, 2))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 15, 2))
    }
}
