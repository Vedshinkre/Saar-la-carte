package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Validates the exact browsing decision behavior for deliveries based on the system's current logic:
 * - Intra-tick: The driver is locked, so a second group in the SAME tick is rejected.
 * - Inter-tick: The driver estimation resets, allowing a group in the NEXT tick to successfully order
 *   and queue up for when the driver physically returns.
 */
class DeliveryBrowsingTickResetA : ExampleSystemTestExtension() {

    override val name = "DeliveryBrowsingTickResetTestA"
    override val description = "the driver is made free in next tick"
    override val restaurants = "driveravailableinnexttick/restaurants.json"
    override val scenario = "driveravailableinnexttick/scenario.json"
    override val food = "driveravailableinnexttick/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        assertTick1Decisions()
        assertTick2Decisions()
    }

    private suspend fun assertTick1Decisions() {
        // Fast-forward to Tick 1 start
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        // VERIFY: Group 1 claims the driver first.
        assertNextLine(TickStatusTestLogs.restDecision(groupId = 1, restId = 1))

        // VERIFY: Group 3 is rejected because the driver was claimed by Group 1 in this exact tick.
        assertNextLine("[DEBUG] Restaurant No Decision: Group 3 could not decide for a restaurant.")

        assertNextLine(TickStatusTestLogs.restStart(1))
    }

    private suspend fun assertTick2Decisions() {
        // Tick 2 start
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))

        // VERIFY: Group 2 decides on R1 successfully because the estimation reset for the new tick.
        assertNextLine(TickStatusTestLogs.restDecision(groupId = 2, restId = 1))

        assertNextLine(TickStatusTestLogs.restStart(1))
    }
}
