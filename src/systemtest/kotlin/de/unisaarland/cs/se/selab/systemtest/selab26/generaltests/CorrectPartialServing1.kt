package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val MEAL_A = "mealA"
private const val MEAL_B = "mealB"
private const val MEAL_C = "mealC"

/**
 * Validates partial service rules with a 3-person group:
 * Group 1 arrives at Tick 1 and orders three different meals.
 * The first meal is served at Tick 5, granting a 2-tick patience extension.
 * The second meal is served at Tick 7.
 * At Tick 8, the third unserved customer times out and leaves.
 */
class CorrectPartialServing1 : ExampleSystemTestExtension() {
    override val name = "CorrectPartialServing1"
    override val description = "3-person group receives partial food, extends patience, but times out on late meals"
    override val restaurants = "partaildeliveryjson/restaurants.json"
    override val scenario = "partaildeliveryjson/scenario.json"
    override val food = "partaildeliveryjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 10

    override suspend fun run() {
        //  TICK 1: Arrival and Ordering
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.arrival(restId = 1, groupId = 1))
        assertNextLine(FohArrivalTestLogs.seating(restId = 1, groupId = 1, tableId = 1, waitstaffIds = listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf(MEAL_A to 1, MEAL_B to 1, MEAL_C to 1),
                waitstaffId = 1
            )
        )

        //  TICK 3: First Meal Cooked
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 1, dishName = MEAL_A, ticks = 2))

        //  TICK 5: Partial Service (mealA)
        // Served 4 ticks after ordering, extending patience to Tick 8
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(
            FohServiceTestLogs.serving(restId = 1, waitstaffId = 1, dishes = mapOf(MEAL_A to 1), tableId = 1, ticks = 4)
        )

        //  TICK 7: Second Meal Cooked & Served (mealB) + Timeout
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(restId = 1, cookId = 1, meals = 1, dishName = MEAL_B, ticks = 6))
        skipUntilString(
            FohServiceTestLogs.serving(restId = 1, waitstaffId = 1, dishes = mapOf(MEAL_B to 1), tableId = 1, ticks = 6)
        )
        // The third customer times out in Tick 7
        skipUntilString(FohServiceTestLogs.noEating(restId = 1, customers = 1, groupId = 1, tableId = 1))
    }
}
