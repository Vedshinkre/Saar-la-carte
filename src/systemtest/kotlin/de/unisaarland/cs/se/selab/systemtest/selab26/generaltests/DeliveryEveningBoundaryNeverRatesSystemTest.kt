package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * F20/F27/P05: once an evening ends, there are no more rating consequences for what happened in it --
 * a delivery unresolved by then is simply abandoned, no rating ever.
 *
 * A blocking dine-in order delays the kitchen queue so group 2's delivery is only handed over in tick
 * 24, the evening's last tick and 3 ticks after its `visitingTick` of 21. `Dish.updateEating()` needs 2
 * more full ticks to reach EATEN, which don't exist in this evening. Matches current behavior:
 * `FrontOfHouse.resetDrivers()` clears `deliveryGroups` at every evening's end, so group 2 is gone
 * before `RatingProcessor` ever sees it.
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

        // hand-over on the evening's last tick, three ticks after visitingTick
        skipUntilString(TickStatusTestLogs.tickStart(24, 1))
        skipUntilString(DeliveryTestLogs.deliveryFinished(restId = 1, driverId = 1, orderId = 2, groupId = 2))

        // group is never resolved and never rates, even into evening 2
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        skipUntilString(StatisticsTestLogs.statsReceived(1, 0))
    }
}
