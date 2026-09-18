package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.RestaurantType
/**
 * Represents the Dish made of a certain recipe.
 */
class Dish(
    val recipe: Recipe,
    val isBasic: Boolean,
    var eatingProgress: Int,
    var status: DishStatus
) {
    // explicit constructor with only recipe
    constructor(recipe: Recipe) : this(
        recipe = recipe,
        isBasic = recipe.basicDishFor != null,
        eatingProgress = 2,
        status = DishStatus.UNCOOKED
    )

    /**
     * explicit constructor 2 , lets see which works better
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
