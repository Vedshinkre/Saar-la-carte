package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Casual delivery customer order placement math, kitchen queuing delays,
 * and eventual patience timeout resulting in an aborted order and negative rating.
 */
class CasualDeliveryTimeoutTest : ExampleSystemTestExtension() {
    override val name = "CasualDeliveryTimeoutTest"
    override val description = "Tests a casual delivery order timing out due to kitchen delays"
    override val restaurants = "deliverypatiencejson/restaurants.json"
    override val scenario = "deliverypatiencejson/scenario.json"
    override val food = "deliverypatiencejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        assertEvening1Tick10()
        assertEvening1Tick18()
    }

    private suspend fun assertEvening1Tick10() {
        // Group 2 math: visitingTick 15 - ceil(10/5) - 3 = 10. They order now!
        skipUntilString(TickStatusTestLogs.tickStart(10, 1))

        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // Group 1 (In-restaurant) arrives, sits, and orders
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("potato fries" to 1), 1))

        // Group 2 (Delivery) places their order early (no waitstaff ID for delivery orders)
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 2,
                orderId = 2,
                dishes = mapOf("baked potato" to 1),
                waitstaffId = null
            )
        )
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 1, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 2, 1))

        // The single TOURNANT cook takes Group 1's order first. Group 2 is blocked!
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "TOURNANT",
                meals = 1,
                dishName = "potato fries",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )

        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))
    }

    private suspend fun assertEvening1Tick18() {
        skipUntilString(TickStatusTestLogs.tickStart(18, 1))

        // Delivering. Group 2's patience expires at the end of tick 18 (15 + 3).
        skipUntilString(DeliveryTestLogs.deliveryGivenUp(restId = 1, groupId = 2, orderId = 2))

        //  The simulation must  output Eating and Escorting statuses before Rating
        assertNextLine(FohServiceTestLogs.eatingStatus(restId = 1, eating = 0, finished = 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(restId = 1, waitstaff = 0, customers = 0))

        //  Rating. They rate negatively because they never got their food.
        assertNextLine(
            FohServiceTestLogs.rating(
                restId = 1,
                groupId = 2,
                rating = "NEGATIVE",
                pos = 10,
                neg = 1
            )
        )

        // Verify the rating summary status
        assertNextLine(FohServiceTestLogs.ratingStatus(restId = 1, groups = 1))
    }
}
