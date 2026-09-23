package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val HOG_DISH = "Hog Dish"
private const val TARGET_DISH = "Target Dish"

/**
 * Tutor-reported bug: "when we partially serve first, we never serve later the remaining dishes,
 * they seem to get lost." Targets `ServingProcessor.serveAssignedWaiterTable`'s least-covered branch:
 * an order that is fully COOKED, has never started serving, but whose group is too big for the
 * shared waiter's remaining capacity THAT tick must be served ALL AT ONCE or not at all (spec: a
 * waiter cannot serve part of a not-yet-started order) - meaning it gets nothing this tick, and has
 * to be picked up again later. No existing test forces that exact collision.
 *
 * Group 1 (Hog, size 9) is seated and ordered at tick 1, group 2 (Target, size 3) has to wait a tick
 * for the single waiter's SEATING capacity and orders at tick 2. Two separate cooks (TOURNANT for
 * Hog, ROAST for Target) let their durations be tuned so both dishes finish cooking in the very same
 * tick 2: Hog's 9 meals exhaust 9 of the waiter's 10 SERVING capacity that tick, leaving only 1 for
 * Target's complete, not-yet-started 3-dish order - too little to serve it at once, so it gets
 * nothing (logged as "FOH No Serving"). Tick 3: the waiter is free again, and Target's order - now
 * flagged as started - must be served in full. If the "must not lose it" invariant is broken, tick 3
 * never serves it and the group instead times out and leaves.
 */
class RegularCapacityBlockedOrderNotLostSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularCapacityBlockedOrderNotLostSystemTest"
    override val description = "A complete order blocked once by insufficient waiter capacity is still served next tick"
    override val restaurants = "regularcapacitypartialservejson/restaurants.json"
    override val food = "regularcapacitypartialservejson/food.json"
    override val scenario = "regularcapacitypartialservejson/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        // tick 1: group 1 (9) seated and ordered; group 2 arrives but finds no free waiter capacity
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.arrival(restId = 1, groupId = 1))
        assertNextLine(FohArrivalTestLogs.seating(restId = 1, groupId = 1, tableId = 1, waitstaffIds = listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf(HOG_DISH to 9),
                waitstaffId = 1
            )
        )
        skipUntilString(FohArrivalTestLogs.noSeatingNoWaitstaff(restId = 1, groupId = 2))

        // tick 2: group 2 finally seated and ordered; both dishes finish cooking this same tick
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(FohArrivalTestLogs.seating(restId = 1, groupId = 2, tableId = 2, waitstaffIds = listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 2,
                orderId = 2,
                dishes = mapOf(TARGET_DISH to 3),
                waitstaffId = 1
            )
        )
        skipUntilString(
            KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 9, dishName = HOG_DISH, ticks = 1)
        )
        assertNextLine(
            KitchenTestLogs.kitchenCooked(restId = 1, cookId = 2, meals = 3, dishName = TARGET_DISH, ticks = 0)
        )

        // group 1 exhausts 9 of the waiter's 10 capacity; group 2's complete, not-yet-started
        // 3-dish order needs all 3 at once and gets NOTHING this tick - not lost, just blocked
        skipUntilString(
            FohServiceTestLogs.serving(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(HOG_DISH to 9),
                tableId = 1,
                ticks = 1
            )
        )
        assertNextLine(FohServiceTestLogs.noServing(restId = 1, waitstaffId = 1, meals = 3, tableId = 2))

        // tick 3: the waiter is free again, and group 2's full order must be served now
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(
            FohServiceTestLogs.serving(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(TARGET_DISH to 3),
                tableId = 2,
                ticks = 1
            )
        )
    }
}
