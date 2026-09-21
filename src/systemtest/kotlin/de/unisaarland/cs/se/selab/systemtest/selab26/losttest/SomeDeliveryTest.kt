package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension

/**
 * Tests that Waitstaff and Delivery Driver IDs reset to 1 every evening,
 * while Order IDs continue to increment globally across evenings.
 */
class SomeDeliveryTest : ExampleSystemTestExtension() {

    override val name = "SomeDeliveryTest"
    override val description = "Tests that Waitstaff and Delivery Driver IDs reset to 1 every evening."
    override val food = "identity_reset/food.json"
    override val restaurants = "identity_reset/restaurants.json"
    override val scenario = "identity_reset/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 48

    override suspend fun run() {
        // ================= EVENING 1 =================
        skipUntilString("[IMPORTANT] Serving: Serving of evening 1 starts.")

        // The first casual delivery group places an order
        // Order ID starts at 1
        skipUntilString("[IMPORTANT] FOH Ordering (R 1): Group 1 placed order 1 of apple snack:1.")

        // Waitstaff gets assigned ID 1. Driver gets assigned ID 1. Order is 1.
        skipUntilString("[IMPORTANT] FOH Delivery (R 1): Waitstaff 1 serves apple snack:1 meals to driver 1 for order 1.")

        // Driver 1 drives order 1
        skipUntilString("[INFO] Delivery Preparation (R 1): Driver 1 prepares driving order 1 to group 1, which will take 1 ticks.")


        // ================= EVENING 2 =================
        skipUntilString("[IMPORTANT] Serving: Serving of evening 2 starts.")

        // The second casual delivery group places an order
        // ORDER ID MUST NOT RESET! It should now be 2.
        skipUntilString("[IMPORTANT] FOH Ordering (R 1): Group 2 placed order 2 of apple snack:1.")

        // WAITSTAFF AND DRIVER ID MUST RESET TO 1! ORDER MUST BE 2!
        // This single line proves everything worked perfectly:
        skipUntilString("[IMPORTANT] FOH Delivery (R 1): Waitstaff 1 serves apple snack:1 meals to driver 1 for order 2.")

        // Driver 1 drives order 2
        skipUntilString("[INFO] Delivery Preparation (R 1): Driver 1 prepares driving order 2 to group 2, which will take 1 ticks.")
    }
}