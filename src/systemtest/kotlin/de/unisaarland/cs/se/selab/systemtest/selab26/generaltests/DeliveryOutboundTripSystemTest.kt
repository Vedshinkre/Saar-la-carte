package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * The outbound trip of a 5 km delivery ordered in tick 6: the driver prepares in tick 6, drives in
 * tick 7 (one tick for 5 km), arrives and hands the food over directly after the driving line.
 *
 * Reuses the fixture of [CasualDeliveryEarlyDecisionTest]. The group never rates, so its
 * experience is not checked here. Written as tester of the delivery feature (Sep 18).
 */
class DeliveryOutboundTripSystemTest : ExampleSystemTestExtension() {
    override val name = "DeliveryOutboundTripSystemTest"
    override val description = "A delivery is prepared, driven and handed over to the customer"
    override val restaurants = "casualdeliveryearlydecisionjson/restaurants.json"
    override val scenario = "casualdeliveryearlydecisionjson/scenario.json"
    override val food = "casualdeliveryearlydecisionjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(DeliveryTestLogs.deliveryPrep(1, 1, 1, 1, 1))

        // The driving starts in the tick after the preparation.
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(1, 1, 5, 0))
        assertNextLine(DeliveryTestLogs.deliveryArrival(1, 1, 1, 1))
        assertNextLine(DeliveryTestLogs.deliveryFinished(1, 1, 1, 1))
    }
}
