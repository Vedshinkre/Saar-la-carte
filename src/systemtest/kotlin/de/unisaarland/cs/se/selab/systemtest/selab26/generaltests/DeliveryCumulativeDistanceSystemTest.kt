package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * F29 (forum thread 126): a 13 km delivery takes 3 ticks and the driving log reports the
 * cumulative distance, capped at the total: 5, 10, then 13 km.
 *
 * NOT REGISTERED (fails) - caused by existing code not authored by Vlad Marciu. Nothing ever adds a
 * delivery CasualGroup to FrontOfHouse.deliveryGroups (declared in FrontOfHouse.kt, only read by
 * ServingProcessor/EatingProcessor/RatingProcessor; ArrivalProcessor's delivery path never
 * registers the group), so ServingProcessor.serveDeliveryGroups never sees the order, never assigns
 * a driver, and no "Delivery Preparation" is ever logged. Fix: after a delivery group's order was
 * placed successfully (ArrivalProcessor.processArrival, non in-house branch) add the group to the
 * shared deliveryGroups list (pass the list into ArrivalProcessor like turnedAwayGroups).
 * Once fixed, register this test again in SystemTestRegistration.
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
        skipUntilString(TickStatusTestLogs.tickStart(9, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(1, 1, 10, 1))
        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(1, 1, 13, 0))
    }
}
