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
 */
class EventSeatingSpecExampleSystemTest : ExampleSystemTestExtension() {
    override val name = "EventSeatingSpecExampleSystemTest"
    override val description = "EVENT of 15 is seated by 3 waiters (10, 4 and 1) as in the spec example"
    override val restaurants = "eventseating/specexample/restaurants.json"
    override val scenario = "eventseating/specexample/scenario.json"
    override val food = "eventseating/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(1)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 4, listOf(2)))

        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 5))
        assertNextLine(FohArrivalTestLogs.seating(1, 5, 5, listOf(1)))

        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(2)))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1, 2, 3)))

        // waiters 1, 2 and 3 seated the 6 customers of the REGULAR and the 15 of the EVENT group
        skipUntilString(FohArrivalTestLogs.seatingStatus(1, 3, 21, 2))
    }
}
