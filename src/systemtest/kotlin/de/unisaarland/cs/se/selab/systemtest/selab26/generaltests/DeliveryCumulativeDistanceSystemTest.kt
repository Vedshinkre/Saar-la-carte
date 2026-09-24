package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension

/**
 * A 13 km delivery takes three ticks, and each driving line reports the distance covered so far,
 * capped at the total: 5, 10, then 13 km, with 2, 1 and 0 ticks left (forum topic 126).
 *
 * 13 is not a multiple of 5, so the last line checks the cap. The ticks-left value in each line
 * ties it to its tick. The arrival has to follow the last driving line directly. Written as tester
 * of the delivery feature (Sep 18).
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
