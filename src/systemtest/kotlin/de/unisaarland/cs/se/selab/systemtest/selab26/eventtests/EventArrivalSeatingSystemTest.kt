package de.unisaarland.cs.se.selab.systemtest.selab26.eventtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "Tomato Soup"

/**
 * P02 (spec 2.2 "Front of house"): the EVENT group is greeted by the event manager, not by one
 * assigned waiter. Any waiter whose SEATING tick load is below the limit is recruited, in descending
 * order of the current load, and each seats customers until their SEATING tick load is at the limit.
 *
 * Restaurant 1 has three waiters. In tick 2 of evening 4 the groups arrive REGULAR, EVENT, CASUAL:
 * - REGULAR 2 (10 customers) fills the SEATING tick load of the first waiter (id 1, current load 10)
 * - REGULAR 3 (4 customers) cannot go to waiter 1 any more and is seated by waiter 2 (current load 4)
 * - EVENT 1 (15 customers) skips waiter 1 whose SEATING tick load is at the limit: waiter 2, the
 *   busier one of the rest, seats 6 (4 + 6 = 10) and the so far idle waiter 3 (it gets id 3 only now)
 *   seats the remaining 9
 * - CASUAL 4 (3 customers) arrives after the EVENT SEATING: nobody has capacity for 3 more, the
 *   group is turned away for now and tries again in tick 3
 * - in tick 3 the SEATING tick loads are reset. Waiter 1 is busy with its 10 customers (current
 *   load not below 10), EVENT customers do not count towards the current load, so waiter 3 has
 *   current load 0 while waiter 2 still has 4 and therefore seats the CASUAL group
 */
class EventArrivalSeatingSystemTest : TableMergingSystemTest() {
    override val name = "EventArrivalSeatingSystemTest"
    override val description = "EVENT seating recruits waiters by descending current load up to the tick limit"
    override val restaurants = "eventtests/seating/restaurants.json"
    override val scenario = "eventtests/seating/scenario.json"
    override val food = "eventtests/seating/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 80

    override suspend fun run() {
        // the EVENT gets the 16 seat table (15 >= 3/4 of 16), then the REGULAR groups their exact fits
        assertNoReservationFailsInPreparation(4)

        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(TickStatusTestLogs.restStart(1))
        assertRegularGroupsSeatedFirst()
        assertEventSeatedByTwoWaiters()

        // CASUAL groups arrive after the EVENT SEATING has been completed: no waiter has capacity left
        assertNextLine(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 4))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 3, 29, 3))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 29, 3))

        assertEventCustomersDoNotCountTowardsCurrentLoad()
    }

    private suspend fun assertRegularGroupsSeatedFirst() {
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 1, mapOf(SOUP to 10), 1))

        assertNextLine(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 1, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 3, 2, mapOf(SOUP to 4), 2))
    }

    private suspend fun assertEventSeatedByTwoWaiters() {
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 3, listOf(2, 3)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 3, mapOf(SOUP to 15), listOf(2, 3)))
    }

    private suspend fun assertEventCustomersDoNotCountTowardsCurrentLoad() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 4, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 4, 4, mapOf(SOUP to 3), 2))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 3, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 3, 1))
    }
}
