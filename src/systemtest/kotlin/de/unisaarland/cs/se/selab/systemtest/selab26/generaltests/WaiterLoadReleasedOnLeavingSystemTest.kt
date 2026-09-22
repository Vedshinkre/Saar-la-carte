package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec adjustment 5: a waiter's current load counts customers seated by them until they left or were
 * escorted. Waiter 1 seats a group of 2 (tick 1) and a group of 10 (tick 2); the cook is busy until tick
 * 5, so the group of 10 is never served and gives up in tick 6 (4 ticks after ordering). By tick 8 both
 * waiters are free, so waiter 1 (lowest id) seats the next group -- if the 10 who left still counted as
 * load, waiter 2 would be picked instead.
 */
class WaiterLoadReleasedOnLeavingSystemTest : ExampleSystemTestExtension() {
    override val name = "WaiterLoadReleasedOnLeavingSystemTest"
    override val description = "Customers that left without being escorted no longer count as load of their waiter"

    override val restaurants = "waiterloadreleasejson/restaurants.json"
    override val food = "waiterloadreleasejson/food.json"
    override val scenario = "waiterloadreleasejson/scenario.json"

    override val logLevel = "INFO"
    override val maxTicks = 9

    override suspend fun run() {
        // waiter 1 seats both groups; the cook is busy, so group 2 never gets served in time
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        skipUntilString(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))

        // tick 6: group of 2 finishes eating and is escorted; group of 10 gives up (leaving before escorting)
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 10, 2, 2))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 2, 1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 2, "NEGATIVE", 11, 1))

        // both waiters are free again: the lowest id gets the next group
        skipUntilString(TickStatusTestLogs.tickStart(8, 1))
        skipUntilString(FohArrivalTestLogs.seating(1, 3, 1, listOf(1)))
    }
}
