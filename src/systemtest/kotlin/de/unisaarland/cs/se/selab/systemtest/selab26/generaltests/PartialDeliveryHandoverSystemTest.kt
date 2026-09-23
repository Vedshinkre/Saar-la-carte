package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val MEAL_A = "mealA"

/**
 * A delivery order that is handed over to the driver in two parts, because the single waiter runs
 * out of SERVING capacity in the tick where the order finishes cooking.
 *
 * The restaurant has one waiter, one driver, one cook and one dish. In tick 5 the in-house group 1
 * (7 people) orders and the delivery group 2 (8 people, distance 5, visitingTick 9, so its order
 * tick is 9 - 3 - 1 = 5) orders as well. Both orders are cooked in one batch of 15 meals that
 * finishes in tick 7.
 *
 * In tick 7 the waiter first SERVES the 7 in-house meals, which leaves them 3 of their 10 SERVING
 * actions. Those 3 meals go to the driver, the other 5 stay in the kitchen: the hand-over has
 * begun, so the order no longer waits for anything and the remaining 5 meals are handed over in
 * tick 8. Only then does the driver hold a full order and prepare the trip, so the delivery
 * arrives in tick 9 - exactly the tick the group wanted it.
 */
class PartialDeliveryHandoverSystemTest : ExampleSystemTestExtension() {
    override val name = "PartialDeliveryHandoverSystemTest"
    override val description =
        "one waiter hands a delivery order over to a driver in two parts, 3 meals now and 5 next tick"
    override val restaurants = "partialdeliveryhandoverjson/restaurants.json"
    override val scenario = "partialdeliveryhandoverjson/scenario.json"
    override val food = "partialdeliveryhandoverjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    override suspend fun run() {
        assertOrdering()
        assertFirstPartialHandOver()
        assertRemainingHandOverAndPreparation()
        assertArrival()
    }

    /** TICK 5: the in-house group and the delivery group both place their order */
    private suspend fun assertOrdering() {
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohArrivalTestLogs.arrival(restId = 1, groupId = 1))
        assertNextLine(FohArrivalTestLogs.seating(restId = 1, groupId = 1, tableId = 1, waitstaffIds = listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf(MEAL_A to 7),
                waitstaffId = 1
            )
        )
        // the delivery group never arrives physically, it only orders
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 2,
                orderId = 2,
                dishes = mapOf(MEAL_A to 8),
                waitstaffId = null
            )
        )
    }

    /** TICK 7: all 15 meals are cooked, the waiter only gets 3 of the 8 delivery meals out */
    private suspend fun assertFirstPartialHandOver() {
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(
            KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 15, dishName = MEAL_A, ticks = 2)
        )
        // the in-house table is served first and takes 7 of the waiter's 10 SERVING actions
        skipUntilString(
            FohServiceTestLogs.serving(restId = 1, waitstaffId = 1, dishes = mapOf(MEAL_A to 7), tableId = 1, ticks = 2)
        )
        assertNextLine(
            FohServiceTestLogs.deliveryHandover(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(MEAL_A to 3),
                driverId = 1,
                orderId = 2
            )
        )
        // 7 served to the table plus 3 handed to the driver exhausts the action limit
        skipUntilString(FohServiceTestLogs.servingStatus(restId = 1, waitstaff = 1, meals = 10))
    }

    /** TICK 8: the rest of the order follows, only now is the driver sent off */
    private suspend fun assertRemainingHandOverAndPreparation() {
        skipUntilString(TickStatusTestLogs.tickStart(8, 1))
        skipUntilString(
            FohServiceTestLogs.deliveryHandover(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(MEAL_A to 5),
                driverId = 1,
                orderId = 2
            )
        )
        skipUntilString(FohServiceTestLogs.servingStatus(restId = 1, waitstaff = 1, meals = 5))
        skipUntilString(
            DeliveryTestLogs.deliveryPrep(restId = 1, driverId = 1, orderId = 2, groupId = 2, ticks = 1)
        )
    }

    /** TICK 9: the driver covers the 5 km and hands the complete order to the group */
    private suspend fun assertArrival() {
        skipUntilString(TickStatusTestLogs.tickStart(9, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(restId = 1, driverId = 1, distance = 5, ticks = 0))
        assertNextLine(DeliveryTestLogs.deliveryArrival(restId = 1, driverId = 1, groupId = 2, orderId = 2))
        assertNextLine(DeliveryTestLogs.deliveryFinished(restId = 1, driverId = 1, orderId = 2, groupId = 2))
    }
}
