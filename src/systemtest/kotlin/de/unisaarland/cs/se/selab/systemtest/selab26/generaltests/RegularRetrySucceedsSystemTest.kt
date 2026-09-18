package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A REGULAR group blocked at their visitingTick by a waiter already saturated by an
 * earlier REGULAR group retries the next tick (their tick load resets every tick) and
 * succeeds - as opposed to [WaitstaffExhaustionTest], which shows a group giving up after
 * a second failed attempt. This is the other outcome the same rejection path can lead to.
 *
 * Both groups' orders fail here (correctly): they are first-time visitors with no order
 * history, and every table is reserved by REGULARs, leaving no "other seats" for the
 * kitchen to estimate demand from, so nothing gets procured. That is F18/F10 territory;
 * this test only cares about the SEATING outcome, which does not depend on it.
 */
class RegularRetrySucceedsSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularRetrySucceedsSystemTest"
    override val description = "A REGULAR group blocked by a saturated waiter retries next tick and succeeds"
    override val restaurants = "regularretrysucceedsjson/restaurants.json"
    override val scenario = "regularretrysucceedsjson/scenario.json"
    override val food = "regularretrysucceedsjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // Group 1 (size 10) takes the only waiter's full SEAT capacity for this tick.
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 1, 10))

        // Group 2 arrives the same tick but no waiter has SEAT capacity left. They are not
        // turned away yet - it is still their first attempt.
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 2))

        skipUntilString(TickStatusTestLogs.restEnd(1))

        // Next tick, the waiter's SEAT tick load has reset, so group 2's retry succeeds -
        // no new "Restaurant Arrival" log, since that only fires on the visitingTick itself.
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 2, 3))
    }
}
