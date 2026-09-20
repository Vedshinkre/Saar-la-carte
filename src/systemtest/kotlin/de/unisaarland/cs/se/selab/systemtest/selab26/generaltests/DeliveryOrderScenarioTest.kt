package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val GRILLED_CHICKEN = "Grilled Chicken"

/**
 * Validates the complete delivery order lifecycle from early ordering,
 * kitchen cooking, driver handover, delivery travel, eating, up to final statistics.
 */
class DeliveryOrderScenarioTest : ExampleSystemTestExtension() {
    override val name = "DeliveryOrderScenarioTest"
    override val description = "Checks complete delivery lifecycle and final statistics for a scenario"

    override val restaurants = "deliverytestjson/restaurants.json"
    override val food = "deliverytestjson/food.json"
    override val scenario = "deliverytestjson/scenario.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        // Tick 1
        // visitingTick = 5, distance = 5 -> Order tick = 5 - ceil(5/5) - 3 = 1.
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(TickStatusTestLogs.restDecision(1, 1))
        skipUntilString(TickStatusTestLogs.restStart(1))

        // Delivery group bina waitstaff ke order place karta hai[cite: 3, 5]
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf(GRILLED_CHICKEN to 2),
                waitstaffId = null
            )
        )

        // 2. Kitchen assignment aur cooking process
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(TickStatusTestLogs.restStart(1))

        // Cook order uthata hai
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "TOURNANT",
                meals = 2,
                dishName = GRILLED_CHICKEN,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )

        // 3. Khana pakne ke baad waiter driver ko hand-over karta hai
        // Maan lijiye khana tick 3 par pak kar taiyar ho jata hai
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 1, 2, GRILLED_CHICKEN, 2))

        assertNextLine(
            FohServiceTestLogs.deliveryHandover(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(GRILLED_CHICKEN to 2),
                driverId = 1,
                orderId = 1
            )
        )

        // driver delivery
        skipUntilString(DeliveryTestLogs.deliveryPrep(restId = 1, driverId = 1, orderId = 1, groupId = 1, ticks = 1))
        skipUntilString(DeliveryTestLogs.deliveryArrival(restId = 1, driverId = 1, groupId = 1, orderId = 1))
        skipUntilString(DeliveryTestLogs.deliveryFinished(restId = 1, driverId = 1, orderId = 1, groupId = 1))

        // final stats
        skipUntilString(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, 2))
        assertNextLine(StatisticsTestLogs.statsServed(1, 0))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 2))
    }
}
