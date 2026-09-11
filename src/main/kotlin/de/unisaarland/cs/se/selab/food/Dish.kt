package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.DishStatus
/**
 * Represents the Dish made of a certain recipe.
 */
class Dish(
    private val recipe: Recipe,
    private val isBasic: Boolean,
    private var eatingProgress: Int,
    private var status: DishStatus
) {
    // explicit constructor with only recipe
    constructor(recipe: Recipe) : this(
        recipe = recipe,
        isBasic = recipe.getBasicDishFor() != null,
        eatingProgress = 2,
        status = DishStatus.UNCOOKED
    )
    // explicit getters for relevant functions
    /**
     * explicit getter to get the Recipe of the Dish.
     */
    fun getRecipe(): Recipe {
        return recipe
    }
    /**
     * explicit getter to get if the recipe is basic or not.
     */
    fun getIsBasic(): Boolean {
        return isBasic
    }
    /**
     * explicit getter to get the dish status.
     */
    fun getStatus(): DishStatus {
        return status
    }
    /**
     * explicit setter to change the status of the dish.
     */
    fun setStatus(newStatus: DishStatus): Unit {
        status = newStatus
    }

    // functions with logic
    /**
     * to update eating action per tick.
     */
    fun updateEating(): Unit {
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
