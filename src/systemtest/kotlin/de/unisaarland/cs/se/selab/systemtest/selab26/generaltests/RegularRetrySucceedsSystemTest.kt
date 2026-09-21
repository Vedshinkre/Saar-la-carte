package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val BIG_GROUP = 10
private const val SMALL_GROUP = 3
private const val FIVE_TICKS = 5

private fun riceBowls(amount: Int) = mapOf("Rice Bowl" to amount)

/**
 * A REGULAR group blocked at their visitingTick by a waiter already saturated by an earlier REGULAR
 * group retries the next tick, when the waiter's tick load has reset, and succeeds. This is the
 * other outcome of the same rejection path that [WaitstaffExhaustionTest] follows to a group
 * giving up after a second failed attempt.
 *
 * Both groups are first-time visitors, so their reserved seats count towards the estimate and the
 * kitchen has stock for both orders.
 */
class RegularRetrySucceedsSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularRetrySucceedsSystemTest"
    override val description = "A REGULAR group blocked by a saturated waiter retries next tick and succeeds"
    override val restaurants = "regularretrysucceedsjson/restaurants.json"
    override val scenario = "regularretrysucceedsjson/scenario.json"
    override val food = "regularretrysucceedsjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = FIVE_TICKS

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        // Group 1 (size 10) takes the only waiter's full SEAT capacity for this tick.
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, riceBowls(BIG_GROUP), 1))

        // Group 2 arrives the same tick but no waiter has SEAT capacity left. They are not turned
        // away yet, it is still their first attempt.
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 2))

        // Next tick the waiter's SEAT tick load has reset, so group 2's retry succeeds. There is no
        // new arrival log, that only fires on the visitingTick itself.
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 2, riceBowls(SMALL_GROUP), 1))
    }
}
