package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val FAST = "fast"
private const val SLOW = "slow"
private const val BLOCKER = "blocker"
private const val TOURNANT = "TOURNANT"

/**
 * Validates complex patience rules:
 * Group 2 receives delayed food on the exact last tick of their 2-tick extension.
 * Group 3 receives nothing and abandons the restaurant when their 5-tick base patience expires.
 */
class PartialServiceSuccessTest : ExampleSystemTestExtension() {
    override val name = "PartialServiceSuccessTest"
    override val description = "Customer receives food at exact end of extended patience while another group times out"
    override val restaurants = "partaildeliveryjson/restaurants.json"
    override val scenario = "partaildeliveryjson/scenario.json"
    override val food = "partaildeliveryjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    override suspend fun run() {
        assertTick1And2Arrivals()
        assertTick4And5FirstDishes()
        assertTick6And7Group3Timeout()
        assertTick9And11Group2Success()
    }

    private suspend fun assertTick1And2Arrivals() {
        // --- TICK 1 ---
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.arrival(restId = 1, groupId = 1))
        assertNextLine(FohArrivalTestLogs.seating(restId = 1, groupId = 1, tableId = 1, waitstaffIds = listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf(BLOCKER to 2),
                waitstaffId = 1
            )
        )

        // --- TICK 2 ---
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(FohArrivalTestLogs.arrival(restId = 1, groupId = 2))
        assertNextLine(FohArrivalTestLogs.seating(restId = 1, groupId = 2, tableId = 2, waitstaffIds = listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 2,
                orderId = 2,
                dishes = mapOf(FAST to 1, SLOW to 1),
                waitstaffId = 1
            )
        )

        skipUntilString(FohArrivalTestLogs.arrival(restId = 1, groupId = 3))
        assertNextLine(FohArrivalTestLogs.seating(restId = 1, groupId = 3, tableId = 3, waitstaffIds = listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 3,
                orderId = 3,
                dishes = mapOf(BLOCKER to 2),
                waitstaffId = 1
            )
        )
    }

    private suspend fun assertTick4And5FirstDishes() {
        // --- TICK 4 ---
        // Group 1's blocker finishes and is served
        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 2, dishName = BLOCKER, ticks = 3))
        assertNextLine(KitchenTestLogs.kitchenStatus(restId = 1, cooks = 1, cooking = 2, finished = 2, servable = 2))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(BLOCKER to 2), 1, 3))

        // --- TICK 5 ---
        // Cook instantly finishes Group 2's fast dish
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = TOURNANT,
                meals = 1,
                dishName = FAST,
                baseOrderId = 2,
                allOrders = listOf(2)
            )
        )
        assertNextLine(KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 1, dishName = FAST, ticks = 3))
    }

    private suspend fun assertTick6And7Group3Timeout() {
        // --- TICK 6 ---
        // Cook assigns Group 2's slow dish. Group 1 finishes eating and is escorted.
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = TOURNANT,
                meals = 1,
                dishName = SLOW,
                baseOrderId = 2,
                allOrders = listOf(2)
            )
        )
        skipUntilString(FohServiceTestLogs.finishedEating(restId = 1, customers = 2, groupId = 1, tableId = 1))
        skipUntilString(
            FohServiceTestLogs.escorting(restId = 1, waitstaffId = 1, customers = 2, groupId = 1, tableId = 1)
        )

        // --- TICK 7 ---
        // Waitstaff serves Group 2's fast dish.
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(
            FohServiceTestLogs.serving(restId = 1, waitstaffId = 1, dishes = mapOf(FAST to 1), tableId = 2, ticks = 5)
        )

        // Group 3 ordered at T2. T7 is exactly 5 ticks later. They abandon the restaurant!
        assertNextLine(FohServiceTestLogs.servingStatus(restId = 1, waitstaff = 1, meals = 1))
        assertNextLine(FohServiceTestLogs.noEating(restId = 1, customers = 2, groupId = 3, tableId = 3))
    }

    private suspend fun assertTick9And11Group2Success() {
        // TICK 9
        // Slow dish finishes. Because the batch window passed, waitstaff serves it immediately.
        skipUntilString(TickStatusTestLogs.tickStart(9, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 1, dishName = SLOW, ticks = 7))
        skipUntilString(
            FohServiceTestLogs.serving(restId = 1, waitstaffId = 1, dishes = mapOf(SLOW to 1), tableId = 2, ticks = 7)
        )

        // TICK 11
        // Group 2 finishes eating both meals and rates positively!
        skipUntilString(TickStatusTestLogs.tickStart(11, 1))
        skipUntilString(FohServiceTestLogs.finishedEating(restId = 1, customers = 1, groupId = 2, tableId = 2))
        skipUntilString(
            FohServiceTestLogs.escorting(restId = 1, waitstaffId = 1, customers = 2, groupId = 2, tableId = 2)
        )
        skipUntilString(FohServiceTestLogs.rating(restId = 1, groupId = 2, rating = "POSITIVE", pos = 6, neg = 0))
    }
}
