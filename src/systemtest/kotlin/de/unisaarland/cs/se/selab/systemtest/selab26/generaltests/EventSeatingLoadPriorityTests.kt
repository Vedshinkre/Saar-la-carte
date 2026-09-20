package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * P02 (FOH - Event Seating): the manager recruits the waiters in descending order of their current
 * load, not by id, and EVENT customers never add to the current load of a waiter.
 *
 * Build-up on evening 4 (3 waiters): in tick 1 CASUAL group 3 (5) is seated by waiter 1 and
 * CASUAL group 4 (8) by waiter 2, as waiter 1 can not take another 8 customers in that tick.
 * The current loads are therefore 5 (id 1), 8 (id 2) and 0 (no id yet).
 * - tick 2: EVENT group 1 (10) is seated completely by the busiest waiter 2 alone
 * - tick 3: EVENT group 2 (12): waiter 2 seats 10, then waiter 1 (load 5) seats the last 2
 * - tick 4: CASUAL group 5 (2) goes to the busiest waiter below 10. As the EVENT customers do
 *   not count, that is still waiter 2 (load 8) and not waiter 1
 *
 * Each test below checks one step, so a failure points to the step that went wrong.
 */
abstract class EventSeatingLoadPriorityTest : ExampleSystemTestExtension() {
    override val restaurants = "eventseating/loadpriority/restaurants.json"
    override val scenario = "eventseating/loadpriority/scenario.json"
    override val food = "eventseating/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100
}

/** Tick 1: sets up the current loads 5 (waiter 1) and 8 (waiter 2). */
class EventSeatingLoadPriorityBuildUpTest : EventSeatingLoadPriorityTest() {
    override val name = "EventSeatingLoadPriorityBuildUpTest"
    override val description = "Casual 5 goes to waiter 1 and casual 8 to waiter 2 (5 + 8 exceeds the tick limit)"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(1)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 4, listOf(2)))
    }
}

/** Tick 2: the waiter with the highest current load (waiter 2, not the lowest id) seats all 10. */
class EventSeatingLoadPriorityBusiestWaiterFirstTest : EventSeatingLoadPriorityTest() {
    override val name = "EventSeatingLoadPriorityBusiestWaiterFirstTest"
    override val description = "EVENT of 10 is seated by waiter 2 alone, the waiter with the highest current load"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(2)))
    }
}

/** Tick 2: only one waiter seated the EVENT group. */
class EventSeatingLoadPriorityFirstEventStatusTest : EventSeatingLoadPriorityTest() {
    override val name = "EventSeatingLoadPriorityFirstEventStatusTest"
    override val description = "Seating status of tick 2 is 1 waitstaff, 10 customers on 1 tables"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 1, 10, 1))
    }
}

/** Tick 3: waiter 2 (load 8) reaches the limit, waiter 1 (load 5) seats the remaining 2. */
class EventSeatingLoadPrioritySecondWaiterTest : EventSeatingLoadPriorityTest() {
    override val name = "EventSeatingLoadPrioritySecondWaiterTest"
    override val description = "EVENT of 12 is seated by waiter 2 (10 customers) and then waiter 1 (2 customers)"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1, 2)))
    }
}

/** Tick 3: two waiters seated the EVENT group. */
class EventSeatingLoadPrioritySecondEventStatusTest : EventSeatingLoadPriorityTest() {
    override val name = "EventSeatingLoadPrioritySecondEventStatusTest"
    override val description = "Seating status of tick 3 is 2 waitstaff, 12 customers on 1 tables"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 2, 12, 1))
    }
}

/**
 * Tick 4: the EVENT customers did not add to any current load, so waiter 2 (load 8) is still the
 * busiest waiter below 10 and takes the casual group. If they had counted, waiter 2 would be out
 * of the pool and waiter 1 would be chosen.
 */
class EventSeatingLoadPriorityEventNotCountedTest : EventSeatingLoadPriorityTest() {
    override val name = "EventSeatingLoadPriorityEventNotCountedTest"
    override val description = "EVENT customers do not add to the current load of the waiters that seated them"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(4, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 5))
        assertNextLine(FohArrivalTestLogs.seating(1, 5, 5, listOf(2)))
    }
}
