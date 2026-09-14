package de.unisaarland.cs.se.selab.food

/**
 * Represents what ingredients are available and what's not .
 */
class Stock(
    // 0 means available, > 0 means the number of evenings it remains unavailable
    private val ingredients: MutableMap<Ingredient, Int>
) {
    // explicit constructor with only ingredients
    constructor(ingredientList: List<Ingredient>) : this(mutableMapOf()) {
        for (item in ingredientList) {
            //  default value  0 as ingredient is fully available
            this.ingredients[item] = 0
        }
    }

    // functions with logic
    /**
     * Explicit setter to set Ingredients to Unavailable .
     */
    fun setIngredientToUnavailable(ingredient: Ingredient, duration: Int) {
        var found = false
        for (key in ingredients.keys) {
            if (key.name == ingredient.name) {
                ingredients[key] = duration
                found = true
                break // stop searching once found
            }
        }
        // should never happen, but for safety reasons, as can change during simulation
        require(found) {
            "Cannot make ingredient '${ingredient.name}' unavailable because it does not exist in stock."
        }
    }

    /**
     * Explicit setter to set Ingredients to Available .
     */
    fun setIngredientToAvailable(ingredient: Ingredient) {
        var found = false
        for (key in ingredients.keys) {
            if (key.name == ingredient.name) {
                ingredients[key] = 0
                found = true
                break // stop searching once found
            }
        }
        // should never happen , but for safety reasons , as can change during simulation
        require(found) {
            "Cannot make ingredient '${ingredient.name}' available because it does not exist in stock."
        }
    }

    /**
     * Explicit getter to get Ingredients if it's in the stock else null  .
     */
    fun getIngredient(ingredient: Ingredient): Ingredient? {
        for (key in ingredients.keys) {
            if (key.name == ingredient.name) {
                return key
            }
        }
        return null
    }

    /**
     * checks if the ingredient is available or not   .
     */
    fun isIngredientAvailable(ingredient: Ingredient): Boolean {
        for ((key, value) in ingredients) {
            if (key.name == ingredient.name) {
                if (value == 0) {
                    return true
                } else {
                    return false
                }
            }
        }
        return false
    }

    /**
     * Decrements the duration of unavailability by 1 for all affected ingredients.
     * Should be called once during the preparation phase of each evening.
     */
    fun decrementUnavailableDurations() {
        for ((key, value) in ingredients) {
            if (value > 0) {
                val newDuration = value - 1
                ingredients[key] = newDuration
            }
        }
    }
}
