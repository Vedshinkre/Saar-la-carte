package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.api.SystemTestAssertionError
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val BOWL = "Bowl"

/**
 * A/B probes (Sep 23) for handing a delivery order to its driver when the waiter cannot pass all of
 * it in one tick. Written for the failing DriveMeCrazy full test. The office hour had also noted
 * that our main branch did not continue a hand-over in the next tick. The specification says the
 * waiters "deliver meals to the drivers [...] until their limit is reached", and adjustment #8
 * splits a hand-over across ticks and waiters. The reference passes HandOverContinuesInTheNextTick
 * and HandOverCountsInTheServingStatus (runs 9-12). The other three fail there by design.
 *
 * One waiter, one driver. In tick 2 a dine-in group of eight and a delivery group of four order
 * the only dish, which is cooked at once. The table is served first, so the waiter has 2 of their
 * 10 serving actions left for the 4 delivery meals. Every probe pins its line to one tick.
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
 * Reading A (the reference's, adjustment #8): 2 meals are handed over in tick 2 and 2 in tick 3.
 * The driver prepares once the order is complete, before tick 4.
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
 * Reading B (rejected, fails by design): nothing is handed over until one waiter can pass the whole
 * order, so the first hand-over is all four meals in tick 3. This follows the specification's "the
 * meals queue until all meals of an order are ready". Adjustment #8 rules it out.
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
 * Reading C (rejected, fails by design): 2 meals are handed over in tick 2 and the rest never, so
 * the driver never leaves and the group gives up. This is not a reading of the specification. It
 * is the behaviour the office hour reported for our main branch, and it checked whether the
 * reference stalls the same way (it does not).
 */
class HandOverNeverCompletesSystemTest : SplitHandOverScenario() {
    override val name = "HandOverNeverCompletesSystemTest"
    override val description = "A delivery handed over only partly is never completed"

    override suspend fun run() {
        assertFirstInTick(2, HAND_OVER_PREFIX, handOver(2))
        skipUntilString(DeliveryTestLogs.deliveryGivenUp(1, 2, 2))
    }
}

/**
 * The reference's answer to an open question: meals handed to a driver count in the serving status
 * of tick 2, 8 at the table plus 2 to the driver.
 */
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

/** Rejected, fails by design: a hand-over is not serving, so the status of tick 2 counts only the 8 at the table. */
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
