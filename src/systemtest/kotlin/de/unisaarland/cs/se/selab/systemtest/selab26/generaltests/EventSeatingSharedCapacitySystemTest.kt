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
 */
class EventSeatingSharedCapacitySystemTest : ExampleSystemTestExtension() {
    override val name = "EventSeatingSharedCapacitySystemTest"
    override val description = "Several EVENT groups of one tick share the remaining tick load of the waiters"
    override val restaurants = "eventseating/sharedcapacity/restaurants.json"
    override val scenario = "eventseating/sharedcapacity/scenario.json"
    override val food = "eventseating/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))

        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1, 2)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(2)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 3))

        // only the two successful EVENT groups (12 + 8 customers on 2 tables) are counted
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 2, 20, 2))
    }
}
