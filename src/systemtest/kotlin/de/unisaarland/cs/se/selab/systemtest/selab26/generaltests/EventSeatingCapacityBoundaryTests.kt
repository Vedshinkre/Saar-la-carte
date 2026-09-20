package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * P02 (FOH - Event Seating): an EVENT seating only counts as fulfilled if all customers could be
 * seated within the tick, otherwise the group leaves. Each restaurant has 2 waiters, so 20 seats
 * per tick. CASUAL groups arrive after the EVENT seating has been completed.
 *
 * - R1: EVENT group 1 (20) exactly uses the capacity of both waiters and is seated by 1 and 2.
 *   CASUAL group 3 arriving in the same tick finds both waiters at their limit and retries in
 *   tick 3, where the tick loads are reset and the EVENT customers did not add to the current
 *   load, so waiter 1 (lowest id) takes it.
 * - R2: EVENT group 2 (21) is one customer above the capacity and fails. The two waiters did
 *   seat 10 customers each before the failure and keep that tick load, so CASUAL group 4 of the
 *   same tick has no free waitstaff either, and is seated by waiter 1 in tick 3. The failed EVENT
 *   group leaves a negative rating.
 *
 * Each test below checks one step, so a failure points to the step that went wrong.
 */
abstract class EventSeatingCapacityBoundaryTest : ExampleSystemTestExtension() {
    override val restaurants = "eventseating/capacity/restaurants.json"
    override val scenario = "eventseating/capacity/scenario.json"
    override val food = "eventseating/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100
}

/** R1: an EVENT group of exactly 20 is seated by the two waiters. */
class EventSeatingCapacityExactFitTest : EventSeatingCapacityBoundaryTest() {
    override val name = "EventSeatingCapacityExactFitTest"
    override val description = "EVENT of 20 is seated by waitstaff 1 and 2, using exactly their tick limit"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1, 2)))
    }
}

/** R1: the casual group of the same tick arrives after the EVENT seating and finds nobody free. */
class EventSeatingCapacityCasualAfterEventTest : EventSeatingCapacityBoundaryTest() {
    override val name = "EventSeatingCapacityCasualAfterEventTest"
    override val description = "Casual group of the EVENT tick finds both waiters at their seating limit"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 3))
    }
}

/** R1: tick loads are reset and EVENT customers add no current load, so waiter 1 takes the retry. */
class EventSeatingCapacityCasualRetryTest : EventSeatingCapacityBoundaryTest() {
    override val name = "EventSeatingCapacityCasualRetryTest"
    override val description = "Casual group retries in the next tick and is seated by waiter 1"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.seating(1, 3, 2, listOf(1)))
    }
}

/** R2: an EVENT group of 21 does not fit the 20 seats per tick of the two waiters. */
class EventSeatingCapacityOneTooManyTest : EventSeatingCapacityBoundaryTest() {
    override val name = "EventSeatingCapacityOneTooManyTest"
    override val description = "EVENT of 21 can not be seated by 2 waiters in one tick and is not seated"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(2, 2))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(2, 2))
    }
}

/** R2: the failed EVENT group leaves a negative rating at the end of the tick. */
class EventSeatingCapacityFailedEventRatingTest : EventSeatingCapacityBoundaryTest() {
    override val name = "EventSeatingCapacityFailedEventRatingTest"
    override val description = "The EVENT group that could not be fully seated leaves a negative rating"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohServiceTestLogs.rating(2, 2, "NEGATIVE", 0, 1))
    }
}

/** R2: the tick load of the failed partial seating is not rolled back, so nobody is free. */
class EventSeatingCapacityFailedKeepsTickLoadTest : EventSeatingCapacityBoundaryTest() {
    override val name = "EventSeatingCapacityFailedKeepsTickLoadTest"
    override val description = "After a failed EVENT seating both waiters keep their tick load, casual finds no one"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(2, 4))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(2, 4))
    }
}

/** R2: in the next tick the tick loads are reset and waiter 1 (id given by the failed seating) acts. */
class EventSeatingCapacityFailedThenRetryTest : EventSeatingCapacityBoundaryTest() {
    override val name = "EventSeatingCapacityFailedThenRetryTest"
    override val description = "Casual group of the failed EVENT tick is seated by waiter 1 in the next tick"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.seating(2, 4, 2, listOf(1)))
    }
}
