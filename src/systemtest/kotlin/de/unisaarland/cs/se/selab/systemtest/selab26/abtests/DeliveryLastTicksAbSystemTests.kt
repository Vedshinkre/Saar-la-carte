package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** A/B probe for whether "a restaurant does not accept new customers in the last 3 ticks of their opening time" */
abstract class DeliveryLastTicksScenario : ExampleSystemTestExtension() {
    override val restaurants = "abtests/deliverylastticks/restaurants.json"
    override val scenario = "abtests/deliverylastticks/scenario.json"
    override val food = "abtests/deliverylastticks/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 24

    /** the decision is logged right after the tick line, before the restaurant simulates */
    protected suspend fun skipToDecisionOfTickEight() {
        skipUntilString(TickStatusTestLogs.tickStart(DECISION_TICK, 1))
    }

    protected companion object {
        const val DECISION_TICK = 8
    }
}

/**
 * Guards the two readings below: if the files themselves are rejected, neither probe says anything
 * about the last-three-ticks rule.
 */
class DeliveryLastTicksFilesAcceptedSystemTest : DeliveryLastTicksScenario() {
    override val name = "DeliveryLastTicksFilesAcceptedSystemTest"
    override val description = "A delivery whose visitingTick lies after the opening time is accepted"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.servingStart(1))
    }
}

/**
 * Reading A: the last-three-ticks rule applies to deliveries too, so the restaurant is not eligible
 * and the group cannot decide at all. This is what we implement.
 */
class DeliveryInLastThreeTicksIsRefusedSystemTest : DeliveryLastTicksScenario() {
    override val name = "DeliveryInLastThreeTicksIsRefusedSystemTest"
    override val description = "Reading A: a delivery ordering in the last 3 opening ticks finds no restaurant"

    override suspend fun run() {
        skipToDecisionOfTickEight()
        assertNextLine(TickStatusTestLogs.restNoDecision(groupId = 1))
    }
}

/**
 * Reading B: the rule is about customers entering the restaurant, so a delivery order is still
 * taken and the group decides on restaurant 1.
 */
class DeliveryInLastThreeTicksIsAcceptedSystemTest : DeliveryLastTicksScenario() {
    override val name = "DeliveryInLastThreeTicksIsAcceptedSystemTest"
    override val description = "Reading B: a delivery ordering in the last 3 opening ticks still picks the restaurant"

    override suspend fun run() {
        skipToDecisionOfTickEight()
        assertNextLine(TickStatusTestLogs.restDecision(groupId = 1, restId = 1))
    }
}
