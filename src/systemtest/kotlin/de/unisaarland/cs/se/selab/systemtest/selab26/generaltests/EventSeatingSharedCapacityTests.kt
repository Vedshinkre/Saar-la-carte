package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * P02 (FOH - Event Seating): every waiter with a SEATING tick load below the limit is considered,
 * so several EVENT groups arriving in the same tick share the capacity of the waitstaff. There
 * are 2 waiters, so 20 seats per tick, and all waiters have a current load of 0 (ties go to the
 * lowest id). The EVENT groups are handled in ascending id.
 *
 * - EVENT group 1 (12): waiter 1 seats 10, waiter 2 seats 2
 * - EVENT group 2 (8): waiter 1 is at the limit, waiter 2 (tick load 2) seats all 8 customers
 * - EVENT group 3 (4): no capacity left, the group is not seated
 *
 * Each test below checks one step, so a failure points to the step that went wrong.
 */
abstract class EventSeatingSharedCapacityTest : ExampleSystemTestExtension() {
    override val restaurants = "eventseating/sharedcapacity/restaurants.json"
    override val scenario = "eventseating/sharedcapacity/scenario.json"
    override val food = "eventseating/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100
}

/** The first EVENT group needs both waiters, the second one starts to fill the capacity of waiter 2. */
class EventSeatingSharedCapacityFirstEventTest : EventSeatingSharedCapacityTest() {
    override val name = "EventSeatingSharedCapacityFirstEventTest"
    override val description = "EVENT of 12 is seated by waiter 1 (10 customers) and waiter 2 (2 customers)"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1, 2)))
    }
}

/** Waiter 1 is at the limit already, waiter 2 still has 8 seats of capacity. */
class EventSeatingSharedCapacitySecondEventTest : EventSeatingSharedCapacityTest() {
    override val name = "EventSeatingSharedCapacitySecondEventTest"
    override val description = "EVENT of 8 is seated by waiter 2 alone, which has a remaining capacity of exactly 8"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(2)))
    }
}

/** Both waiters are at their limit after the first two EVENT groups. */
class EventSeatingSharedCapacityNoCapacityLeftTest : EventSeatingSharedCapacityTest() {
    override val name = "EventSeatingSharedCapacityNoCapacityLeftTest"
    override val description = "Third EVENT group of the tick finds no waiter below the seating limit"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 3))
    }
}

/** Only the two successful EVENT groups (12 + 8 customers on 2 tables) are counted. */
class EventSeatingSharedCapacityStatusTest : EventSeatingSharedCapacityTest() {
    override val name = "EventSeatingSharedCapacityStatusTest"
    override val description = "Seating status counts only the seated EVENT groups: 2 waitstaff, 20 customers, 2 tables"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 2, 20, 2))
    }
}
