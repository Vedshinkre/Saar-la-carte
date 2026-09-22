package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec adjustment 5: the current load of a waiter counts the customers seated by them until those customers either
 * left or were escorted. Waiter 1 seats a group of 2 (tick 1) and a group of 10 (tick 2). The only cook is busy with
 * the first order until tick 4 and starts the second one in tick 5, so the group of 10 is not served. The ordering
 * tick counts as the first of the patience window (confirmed against the reference by
 * [de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.PatienceEndsFourTicksAfterOrderingSystemTest]),
 * so the group of 10 gives up four ticks after ordering and leaves in tick 6. In tick 8 both waiters have no
 * customers left, so waiter 1 (lowest id) seats the next group. If the ten customers that left still counted as
 * load, waiter 1 would be at load 10 and waiter 2 would be chosen instead.
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
        // waiter 1 seats both groups, the second one has to be served by the busy cook and nobody serves it in time
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        skipUntilString(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))

        // in tick 6 both happen: the group of 2 finishes eating and is escorted, and the group of 10
        // gives up four ticks after ordering, in that order (eating/leaving runs before escorting)
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 10, 2, 2))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 2, 1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 2, "NEGATIVE", 11, 1))

        // both waiters are free again: the lowest id gets the next group
        skipUntilString(TickStatusTestLogs.tickStart(8, 1))
        skipUntilString(FohArrivalTestLogs.seating(1, 3, 1, listOf(1)))
    }
}
