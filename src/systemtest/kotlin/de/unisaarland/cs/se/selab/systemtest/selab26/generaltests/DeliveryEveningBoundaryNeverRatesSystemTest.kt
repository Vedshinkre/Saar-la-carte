package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * F20/F27/P05, confirmed: once an evening ends, there are no more rating consequences for what
 * happened in it, so a delivery that can't be fully resolved before the evening ends must simply be
 * abandoned -- no rating, in this evening or any later one.
 *
 * A blocking dine-in order (group 1) delays the kitchen queue so the delivery group's (group 2) dish is
 * only handed to the driver in tick 23 and delivered in tick 24 -- the evening's last tick, and three
 * ticks after group 2's `visitingTick` of 21 (the latest a delivery's desired arrival may validly be).
 * At the moment of hand-over, [de.unisaarland.cs.se.selab.actors.Driver.handOverToCustomer] scores this
 * as a NEGATIVE experience (it arrived after `visitingTick`), but `Dish.updateEating()` needs 3 calls (2
 * full ticks after hand-over) to reach EATEN, so it cannot finish within evening 1.
 *
 * This matches what our own jar currently does: `FrontOfHouse.resetDrivers()` clears every delivery
 * group (`deliveryGroups.clear()`) at every evening's end, so group 2 is removed before
 * `RatingProcessor` ever gets a chance to rate it off a reset or otherwise stale experience.
 */
class DeliveryEveningBoundaryNeverRatesSystemTest : ExampleSystemTestExtension() {
    override val name = "DeliveryEveningBoundaryNeverRatesSystemTest"
    override val description = "A delivery handed over on the evening's last tick is abandoned, never rating"

    override val restaurants = "deliveryeveningboundaryjson/restaurants.json"
    override val food = "deliveryeveningboundaryjson/food.json"
    override val scenario = "deliveryeveningboundaryjson/scenario.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 30

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(23, 1))
        skipUntilString(FohServiceTestLogs.deliveryHandover(1, 1, mapOf("Delivered Dish" to 1), 1, 2))

        // hand-over happens on the evening's last tick, three ticks after group 2's visitingTick of 21
        skipUntilString(TickStatusTestLogs.tickStart(24, 1))
        skipUntilString(DeliveryTestLogs.deliveryFinished(restId = 1, driverId = 1, orderId = 2, groupId = 2))

        // evening 2 starts and runs to completion: the group is never resolved, never rates
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        skipUntilString(StatisticsTestLogs.statsReceived(1, 0))
    }
}
