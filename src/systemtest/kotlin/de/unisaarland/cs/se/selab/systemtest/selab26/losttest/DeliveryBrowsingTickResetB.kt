package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Validates the exact browsing decision behavior for deliveries based on strict specification logic:
 * - Intra-tick: The driver is locked, so a second group in the SAME tick is rejected.
 * - Inter-tick: The driver estimation remains locked across ticks, so a group in the NEXT tick is also rejected.
 */
class DeliveryBrowsingTickResetB : ExampleSystemTestExtension() {

    override val name = "DeliveryBrowsingTickResetTestB"
    override val description = "the driver remains locked in the next tick"
    override val restaurants = "driverlockedinnexttick/restaurants.json"
    override val scenario = "driverlockedinnexttick/scenario.json"
    override val food = "driverlockedinnexttick/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        assertTick1Decisions()
        assertTick2Decisions()
    }

    private suspend fun assertTick1Decisions() {
        //  Tick 1 start
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        // Group 1 claims the driver first.
        assertNextLine(TickStatusTestLogs.restDecision(groupId = 1, restId = 1))

        //  Group 3 is rejected because the driver was claimed by Group 1 in this exact tick.
        assertNextLine("[DEBUG] Restaurant No Decision: Group 3 could not decide for a restaurant.")

        assertNextLine(TickStatusTestLogs.restStart(1))
    }

    private suspend fun assertTick2Decisions() {
        // Tick 2 start
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))

        // Group 2 fails to decide on R1 because the driver is still locked from Tick 1.
        assertNextLine("[DEBUG] Restaurant No Decision: Group 2 could not decide for a restaurant.")

        assertNextLine(TickStatusTestLogs.restStart(1))
    }
}
