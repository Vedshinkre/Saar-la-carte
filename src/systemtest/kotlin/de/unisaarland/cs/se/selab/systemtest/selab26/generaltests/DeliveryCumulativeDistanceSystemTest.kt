package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension

/**
 * F29 (forum thread 126): a 13 km delivery takes 3 ticks and the driving log reports the
 * cumulative distance, capped at the total: 5, 10, then 13 km.
 */
class DeliveryCumulativeDistanceSystemTest : ExampleSystemTestExtension() {
    override val name = "DeliveryCumulativeDistanceSystemTest"
    override val description = "The driving log reports cumulative distance capped at the total"
    override val restaurants = "deliveryoutboundlongtripjson/restaurants.json"
    override val scenario = "deliveryoutboundlongtripjson/scenario.json"
    override val food = "deliveryoutboundlongtripjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 16

    override suspend fun run() {
        skipUntilString(DeliveryTestLogs.deliveryPrep(1, 1, 1, 1, 3))
        skipUntilString(DeliveryTestLogs.deliveryDriving(1, 1, 5, 2))
        skipUntilString(DeliveryTestLogs.deliveryDriving(1, 1, 10, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(1, 1, 13, 0))
        assertNextLine(DeliveryTestLogs.deliveryArrival(1, 1, 1, 1))
    }
}
