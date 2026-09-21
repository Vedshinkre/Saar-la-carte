package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs
private const val APPLE = "apple snack"

/**
 * Tests that Waitstaff and Delivery Driver IDs reset to 1 every evening,
 * while Order IDs continue to increment globally across evenings.
 */
class SomeDeliveryTest : ExampleSystemTestExtension() {

    override val name = "DeliveryStaffIdentityResetTest"
    override val description = "Tests that Waitstaff and Delivery Driver IDs reset to 1 every evening."
    override val food = "identity_reset/food.json"
    override val restaurants = "identity_reset/restaurants.json"
    override val scenario = "identity_reset/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 48

    override suspend fun run() {
        // ================= EVENING 1 =================
        skipUntilString(TickStatusTestLogs.servingStart(evening = 1))

        // The first casual delivery group places an order
        // Order ID starts at 1
        skipUntilString(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf(APPLE to 1),
                waitstaffId = null
            )
        )

        // Waitstaff gets assigned ID 1. Driver gets assigned ID 1. Order is 1.
        skipUntilString(
            FohServiceTestLogs.deliveryHandover(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(APPLE to 1),
                driverId = 1,
                orderId = 1
            )
        )

        // Driver 1 drives order 1
        skipUntilString(
            DeliveryTestLogs.deliveryPrep(
                restId = 1,
                driverId = 1,
                orderId = 1,
                groupId = 1,
                ticks = 1
            )
        )

        // ================= EVENING 2 =================
        skipUntilString(TickStatusTestLogs.servingStart(evening = 2))

        // The second casual delivery group places an order
        // ORDER ID MUST NOT RESET! It should now be 2.
        skipUntilString(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 2,
                orderId = 2,
                dishes = mapOf(APPLE to 1),
                waitstaffId = null
            )
        )

        // WAITSTAFF AND DRIVER ID MUST RESET TO 1! ORDER MUST BE 2!
        skipUntilString(
            FohServiceTestLogs.deliveryHandover(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(APPLE to 1),
                driverId = 1,
                orderId = 2
            )
        )

        // Driver 1 drives order 2
        skipUntilString(
            DeliveryTestLogs.deliveryPrep(
                restId = 1,
                driverId = 1,
                orderId = 2,
                groupId = 2,
                ticks = 1
            )
        )
    }
}
