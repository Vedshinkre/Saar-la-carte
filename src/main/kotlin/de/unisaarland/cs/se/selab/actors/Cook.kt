package de.unisaarland.cs.se.selab.actors

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Recipe
import kotlin.math.ceil

private const val MINUTES_PER_TICK = 10.0

/**
 * Represents the cook in a kitchen .
 *   @property id the unique numeric identifier of the cook, or `null` if unassigned.
 *   @property orderId the primary base order identifier associated with the active cooking task.
 *   @property type the culinary specialty and qualification rank ([CookType]) of this cook.
 *   @property currentRecipe the recipe currently being prepared, or `null` when idle.
 *   @param remainingTicks the initial remaining simulation ticks needed to finish cooking.
 *   @property isCooking indicates whether the cook is actively engaged in cooking dishes
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
     * @return the list of dishes being prepared.
     */
    fun getDishes(): List<Dish> {
        return assignedDishes
    }

    /**
     * How many of the dishes currently with the cook belong to each order, by ascending order `id`.
     * @return a sorted map mapping each customer order ID to its count of assigned dishes.
     */
    fun getAssignedCountsByOrder(): Map<Int, Int> {
        val result = sortedMapOf<Int, Int>()
        for ((orderId, dishes) in assignedDishesByOrder) {
            result[orderId] = dishes.size
        }
        return result
    }

    /**
     * Overrides the remaining ticks countdown for the cook's active task.
     * @param numm the new remaining duration in simulation ticks.
     */
    fun setRemainingTicks(numm: Int) {
        remainingTicks = numm
    }

    // functions with logic

    /**
     * Assigns a recipe and dish batch to the cook and begins the cooking countdown.
     *
     * Translates preparation duration into simulation ticks ,
     * registers all assigned dishes partitioned by order, and transitions each dish status
     * to [DishStatus.COOKING].
     *
     * @param recipe the recipe being prepared.
     * @param dishes the flattened list of all dishes to cook in this batch.
     * @param baseOrderId the primary initiating customer order ID.
     * @param dishesByOrder a mapping of order IDs to their respective subset of dishes in this batch.
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
     * Advances cooking progress by one simulation tick, transitioning dishes to cooked upon completion.
     *
     * When [remainingTicks] reaches 0, transitions all assigned dishes to [DishStatus.COOKED],
     * resets the cook's state back to idle, clears order mappings, and returns a [CookResult]
     * reflecting the number of finished meals.
     *
     * @return a [CookResult] summarizing active cooking state, assigned meals, and finished count.
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
