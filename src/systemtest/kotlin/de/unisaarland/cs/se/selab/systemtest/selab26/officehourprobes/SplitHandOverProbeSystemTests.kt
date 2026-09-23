package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.api.SystemTestAssertionError
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val BOWL = "Bowl"

/**
 * A/B probes for handing a delivery order to its driver when the waitstaff cannot do it in one tick -
 * the area of the DriveMeCrazy failure, and a known gap of the code the reference run was made from.
 *
 * The serving section of the specification: "For deliveries, the meals queue until all meals of an
 * order are ready and a driver is free. Then, all waiters with a SERVING tick load below the action
 * limit deliver meals to the drivers [...] until their limit is reached or the condition is negated."
 *
 * The scenario: one waiter, one driver. In tick 2 a dine-in group of eight and a delivery group of
 * four both order the only dish, which is cooked on the spot. Serving goes to the table first, so the
 * waiter spends 8 of their 10 serving actions there and has room for only 2 of the 4 delivery meals.
 *
 * Results 10: HandOverContinuesInTheNextTick and HandOverCountsInTheServingStatus pass on the
 * reference, the same as on our dev branch. The other three fail there by design.
 *
 * Every probe pins its line to one tick: the skip starts at that tick and the start of the next tick
 * must still be ahead of the line.
 */
abstract class SplitHandOverScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/splithandover/restaurants.json"
    override val scenario = "officehourjson/splithandover/scenario.json"
    override val food = "officehourjson/splithandover/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    protected fun handOver(meals: Int): String =
        FohServiceTestLogs.deliveryHandover(1, 1, mapOf(BOWL to meals), 1, 2)

    /** the first line of [tick] that starts with [prefix], which must equal [expected] */
    protected suspend fun assertFirstInTick(tick: Int, prefix: String, expected: String) {
        skipUntilString(TickStatusTestLogs.tickStart(tick, 1))
        val found = skipUntilString(prefix)
        if (found != expected) {
            throw SystemTestAssertionError(
                "Expected the first \"$prefix\" line of tick $tick to be\n    $expected\nbut got\n    $found"
            )
        }
        skipUntilString(TickStatusTestLogs.tickStart(tick + 1, 1))
    }

    protected companion object {
        const val HAND_OVER_PREFIX = "[IMPORTANT] FOH Delivery (R 1)"
        const val SERVING_STATUS_PREFIX = "[DEBUG] FOH Serving Status (R 1)"
    }
}

/**
 * Reading A, the specification's: the waiter hands over what fits in tick 2 and the rest in tick 3,
 * and the driver leaves once the order is complete. This is what our dev branch does.
 */
class HandOverContinuesInTheNextTickSystemTest : SplitHandOverScenario() {
    override val name = "HandOverContinuesInTheNextTickSystemTest"
    override val description = "A delivery that does not fit the waiter's capacity is handed over over two ticks"

    override suspend fun run() {
        // pins the first hand-over to tick 2 and leaves the log at the start of tick 3
        assertFirstInTick(2, HAND_OVER_PREFIX, handOver(2))
        skipUntilString(handOver(2))
        skipUntilString(DeliveryTestLogs.deliveryPrep(1, 1, 2, 2, 1))
        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
    }
}

/**
 * Reading B: nothing is handed over until a waiter can pass the whole order at once, so tick 2 has
 * no hand-over and all four meals go in one line in tick 3.
 */
class HandOverWaitsForTheWholeOrderSystemTest : SplitHandOverScenario() {
    override val name = "HandOverWaitsForTheWholeOrderSystemTest"
    override val description = "A delivery is only handed over once the whole order fits one waiter"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        val first = skipUntilString(HAND_OVER_PREFIX)
        if (first != handOver(4)) {
            throw SystemTestAssertionError(
                "Expected the first hand-over to be\n    ${handOver(4)}\nbut got\n    $first"
            )
        }
    }
}

/**
 * Reading C: the hand-over is never finished, so the driver never leaves - the behaviour the office
 * hour reported for the code the reference run was made from. Kept so a stall is named, not guessed.
 */
class HandOverNeverCompletesSystemTest : SplitHandOverScenario() {
    override val name = "HandOverNeverCompletesSystemTest"
    override val description = "A delivery handed over only partly is never completed"

    override suspend fun run() {
        assertFirstInTick(2, HAND_OVER_PREFIX, handOver(2))
        skipUntilString(DeliveryTestLogs.deliveryGivenUp(1, 2, 2))
    }
}

/** Handing meals to a driver counts towards the serving status: 8 at the table plus 2 to the driver. */
class HandOverCountsInTheServingStatusSystemTest : SplitHandOverScenario() {
    override val name = "HandOverCountsInTheServingStatusSystemTest"
    override val description = "Meals handed to a driver are counted in the serving status of the tick"

    override suspend fun run() {
        assertFirstInTick(2, SERVING_STATUS_PREFIX, FohServiceTestLogs.servingStatus(1, 1, TABLE_AND_DRIVER))
    }

    private companion object {
        const val TABLE_AND_DRIVER = 10
    }
}

/** Handing meals to a driver is not serving: the status of tick 2 only counts the 8 at the table. */
class HandOverIsNotCountedInTheServingStatusSystemTest : SplitHandOverScenario() {
    override val name = "HandOverIsNotCountedInTheServingStatusSystemTest"
    override val description = "Meals handed to a driver are not counted in the serving status"

    override suspend fun run() {
        assertFirstInTick(2, SERVING_STATUS_PREFIX, FohServiceTestLogs.servingStatus(1, 1, TABLE_ONLY))
    }

    private companion object {
        const val TABLE_ONLY = 8
    }
}
