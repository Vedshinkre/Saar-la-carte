package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.RestaurantType
/**
 * Represents the Dish made of a certain recipe.
 * @property recipe the [Recipe] from which this dish is prepared.
 * @property isBasic indicates whether this dish qualifies as a basic dish for a restaurant type.
 * @property eatingProgress the remaining simulation ticks required for a guest to consume the dish.
 * @property status the current lifecycle state of the dish.
 * @property abandoned flags whether the customer group departed before serving, allowing the kitchen
 * to continue cooking without routing to a table.
 */
class Dish(
    val recipe: Recipe,
    val isBasic: Boolean,
    var eatingProgress: Int,
    var status: DishStatus,
    // set when the customer who ordered the dish left before it served, so kitchen can keep cooking it */
    var abandoned: Boolean = false
    // getServableDishes also updated accordingly
) {
    // explicit constructor with only recipe
    constructor(recipe: Recipe) : this(
        recipe = recipe,
        isBasic = recipe.basicDishFor != null,
        eatingProgress = 2,
        status = DishStatus.UNCOOKED
    )

    /**
     * explicit constructor 2
     */
    constructor(recipe: Recipe, restaurantType: RestaurantType) : this(
        recipe = recipe,
        isBasic = recipe.basicDishFor == restaurantType,
        eatingProgress = 2,
        status = DishStatus.UNCOOKED
    )

    // functions with logic
    /**
     * to update eating action per tick.
     *  When [eatingProgress] reaches 0, transitions [status] to [DishStatus.EATEN].
     *  If consumption ticks remain, decrements [eatingProgress] by 1.
     */
    fun updateEating() {
        // If the progress hits 0, the guest has finished eating the dish
        if (eatingProgress == 0) {
            status = DishStatus.EATEN
        }

        //  the progress goes down by 1 each tick
        if (eatingProgress > 0) {
            eatingProgress -= 1
        }
    }
}
