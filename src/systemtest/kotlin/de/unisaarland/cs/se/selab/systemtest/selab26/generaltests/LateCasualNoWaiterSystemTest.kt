package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A restaurant open from tick 1 to 24 accepts no new customers in its last 3 ticks, so it only
 * processes arrivals up to tick 21. Both CASUAL groups arrive in tick 21, but the only waiter
 * can seat at most 10 people per tick: group 1 (6 people) is seated, group 2 (another 6) finds
 * no free waiter and stays in the queue for a retry. The retry never happens because the
 * restaurant is closed to new customers from tick 22, so group 2 must be turned away then and
 * rate negatively, instead of staying in the customer queue unnoticed.
 */
class LateCasualNoWaiterSystemTest : ExampleSystemTestExtension() {
    override val name = "LateCasualNoWaiterSystemTest"
    override val description = "A group still waiting for a waiter after the last open tick is turned away"
    override val restaurants = "latecasualnowaiterjson/restaurants.json"
    override val scenario = "latecasualnowaiterjson/scenario.json"
    override val food = "simplejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = ONE_EVENING

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(LAST_OPEN_TICK, 1))
        skipUntilString(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 2))

        // the retry tick is already closed for new customers: group 2 leaves and rates
        skipUntilString(TickStatusTestLogs.tickStart(LAST_OPEN_TICK + 1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 2, "NEGATIVE", POSITIVE, NEGATIVE))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))
    }

    private companion object {
        const val ONE_EVENING = 24
        const val LAST_OPEN_TICK = 21
        const val POSITIVE = 10
        const val NEGATIVE = 4
    }
}
