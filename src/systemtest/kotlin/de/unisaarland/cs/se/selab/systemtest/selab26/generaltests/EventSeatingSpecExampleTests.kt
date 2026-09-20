package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * P02 (FOH - Event Seating): the example of the spec, an EVENT group of 15 customers is seated by
 * three waiters. The waiters are recruited in descending order of their current load and each of
 * them seats until its SEATING tick load is at the limit of 10.
 *
 * Build-up on evening 4 (3 waiters, the cooking takes 3 ticks so nobody leaves before tick 4):
 * - tick 1: CASUAL group 3 (8) is seated by the first waiter (id 1), CASUAL group 4 (5) can not
 *   be seated by it any more (8 + 5 > 10) so a second waiter (id 2) takes it
 * - tick 2: CASUAL group 5 (7) goes to the busiest waiter with a current load below 10, waiter 1,
 *   which now has a current load of 15 and a tick load of 7
 * - tick 3: REGULAR group 2 (6) is seated first, by waiter 2 (busiest one below 10, load 5), which
 *   ends up with a current load of 11 and a tick load of 6. Waiter 3 has not done anything yet.
 *   Then EVENT group 1 (15) arrives: waiter 1 (15/0) seats 10, waiter 2 (11/6) seats 4 and the
 *   last waiter (0/0), which gets the id 3, seats the final customer.
 *
 * Each test below checks one step, so a failure points to the step that went wrong.
 */
abstract class EventSeatingSpecExampleTest : ExampleSystemTestExtension() {
    override val restaurants = "eventseating/specexample/restaurants.json"
    override val scenario = "eventseating/specexample/scenario.json"
    override val food = "eventseating/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100
}

/** Tick 1: the first two CASUAL groups are seated by two different waiters. */
class EventSeatingSpecExampleTickOneCasualTest : EventSeatingSpecExampleTest() {
    override val name = "EventSeatingSpecExampleTickOneCasualTest"
    override val description = "Casual 8 goes to waiter 1, casual 5 to waiter 2 as 8 + 5 exceeds the tick limit"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(1)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 4, listOf(2)))
    }
}

/** Tick 2: waiter 1 is the busiest waiter below a current load of 10 and takes the 7 customers. */
class EventSeatingSpecExampleTickTwoCasualTest : EventSeatingSpecExampleTest() {
    override val name = "EventSeatingSpecExampleTickTwoCasualTest"
    override val description = "Casual 7 goes to the busiest waiter below load 10, waiter 1 (8 to 15 customers)"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 5))
        assertNextLine(FohArrivalTestLogs.seating(1, 5, 5, listOf(1)))
    }
}

/** Tick 3: the REGULAR group is seated before the EVENT group and gives waiter 2 a tick load of 6. */
class EventSeatingSpecExampleRegularBeforeEventTest : EventSeatingSpecExampleTest() {
    override val name = "EventSeatingSpecExampleRegularBeforeEventTest"
    override val description = "Regular 6 is seated by waiter 2 before the EVENT arrives in the same tick"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(2)))
    }
}

/** Tick 3: waiters 1 (15/0), 2 (11/6) and 3 (0/0) seat 10, 4 and 1 customers of the EVENT group. */
class EventSeatingSpecExampleThreeWaitersTest : EventSeatingSpecExampleTest() {
    override val name = "EventSeatingSpecExampleThreeWaitersTest"
    override val description = "EVENT of 15 is seated by waitstaff 1, 2 and 3 (10, 4 and 1 customers)"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1, 2, 3)))
    }
}

/** Tick 3: the seating status counts the REGULAR customers and the EVENT customers. */
class EventSeatingSpecExampleSeatingStatusTest : EventSeatingSpecExampleTest() {
    override val name = "EventSeatingSpecExampleSeatingStatusTest"
    override val description = "Seating status of tick 3 is 3 waitstaff, 21 customers on 2 tables"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 3, 21, 2))
    }
}
