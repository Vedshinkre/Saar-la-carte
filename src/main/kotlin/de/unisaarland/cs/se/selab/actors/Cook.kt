package de.unisaarland.cs.se.selab.actors

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Recipe
import kotlin.math.ceil

private const val MINUTES_PER_TICK = 10.0

/**
 * Represents the cook in a kitchen .
 */

class Cook(
    var id: Int?,
    var orderId: Int?,
    val type: CookType,
    var currentRecipe: Recipe?,
    private var remainingTicks: Int,
    var isCooking: Boolean
) {
    // Internal attribute to store the list of dishes to be worked on
    private val assignedDishes = mutableListOf<Dish>()

    // until cooking is done instead of being flattened away at assignment time.
    private val assignedDishesByOrder = mutableMapOf<Int, MutableList<Dish>>()

    // explicit constructor with only Cook type
    constructor(type: CookType) : this(
        id = null,
        orderId = null,
        type = type,
        currentRecipe = null,
        remainingTicks = 0,
        isCooking = false
    )

    // change during the simulation (safety check)
    init {
        require(remainingTicks >= 0) { "Remaining ticks must be >= 0." }
    }

    /**
     *explicit setter to set id to the cook .
     */
    fun setId(givenId: Int) {
        if (id == null) {
            id = givenId
        }
    }

    /**
     * Read-only exposure of assigned dishes.
     */
    fun getDishes(): List<Dish> {
        return assignedDishes
    }

    /**
     * How many of the dishes currently with the cook belong to each order, by ascending order `id`.
     */
    fun getAssignedCountsByOrder(): Map<Int, Int> {
        val result = sortedMapOf<Int, Int>()
        for ((orderId, dishes) in assignedDishesByOrder) {
            result[orderId] = dishes.size
        }
        return result
    }

    /**
     * Read-only exposure of assigned dishes.
     */
    fun setRemainingTicks(numm: Int) {
        remainingTicks = numm
    }

    // functions with logic
    /**
     * function to start cooking with .
     */
    fun startCooking(
        recipe: Recipe,
        dishes: List<Dish>,
        baseOrderId: Int,
        dishesByOrder: Map<Int, List<Dish>> = mapOf(baseOrderId to dishes)
    ) {
        currentRecipe = recipe
        orderId = baseOrderId
        isCooking = true
        // calculate the exact ticks needed to cook the given recipe
        val durationDouble = recipe.duration.toDouble()

        val calculatedTicks = ceil(durationDouble / MINUTES_PER_TICK).toInt() - 1
        // safety check for the math
        if (calculatedTicks > 0) {
            remainingTicks = calculatedTicks
        } else {
            remainingTicks = 0
        }
        assignedDishes.clear()
        assignedDishes.addAll(dishes)

        assignedDishesByOrder.clear()
        for ((orderId, orderDishes) in dishesByOrder) {
            assignedDishesByOrder[orderId] = orderDishes.toMutableList()
        }

        for (dish in assignedDishes) {
            dish.status = DishStatus.COOKING
        }
    }

    /**
     * logs the cook status and sets dishes to cooked if finished cooking .
     */
    fun cookDishes(): CookResult {
        if (!isCooking) {
            return CookResult(false, 0, 0)
        }

        val totalAssignedMeals = assignedDishes.size
        if (remainingTicks == 0) {
            isCooking = false
        }
        if (remainingTicks > 0) {
            remainingTicks -= 1
        }

        if (!isCooking) {
            val finishedThisTick = assignedDishes.size

            //  Set all dishes to COOKED
            for (dish in assignedDishes) {
                dish.status = DishStatus.COOKED
            }
            currentRecipe = null
            orderId = null
            assignedDishes.clear()
            assignedDishesByOrder.clear()
            return CookResult(true, totalAssignedMeals, finishedThisTick)
        }

        return CookResult(true, totalAssignedMeals, 0)
    }
}
