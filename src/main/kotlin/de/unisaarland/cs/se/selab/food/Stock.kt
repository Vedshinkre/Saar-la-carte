package de.unisaarland.cs.se.selab.food

/**
 * Represents what ingredients are available and what's not .
 */
class Stock(
    private val ingredients: MutableMap<Ingredient, Boolean>
    /* TODO
    We need a way to keep track for how long an ingredient is unavailable.
    Easiest way is to change the map to <Ingredient, Pair<Boolean, Int>>
    The default for an ingredient would be (true, 0) and then the incident would change it to (false, duration)
    We also need a way to decrement and change the availability of an ingredient once the duration has passed.
    An idea would be to have isIngredientAvailable() decrement the counter before it returns false
     */

) {
    // explicit constructor with only ingredients
    constructor(ingredientList: List<Ingredient>) : this(mutableMapOf()) {
        for (item in ingredientList) {
            this.ingredients[item] = true
        }
    }

    // functions with logic
    // functions with logic
    /**
     * Explicit setter to set Ingredients to Unavailable .
     */
    fun setIngredientToUnavailable(ingredient: Ingredient) {
        var found = false
        for (key in ingredients.keys) {
            if (key.getName() == ingredient.getName()) {
                ingredients[key] = false
                found = true
                break // stop searching once found
            }
        }
        // should never happen, but for safety reasons, as can change during simulation
        require(found) {
            "Cannot make ingredient '${ingredient.getName()}' unavailable because it does not exist in stock."
        }
    }

    /**
     * Explicit setter to set Ingredients to Available .
     */
    fun setIngredientToAvailable(ingredient: Ingredient) {
        var found = false
        for (key in ingredients.keys) {
            if (key.getName() == ingredient.getName()) {
                ingredients[key] = true
                found = true
                break // stop searching once found
            }
        }
        // should never happen , but for safety reasons , as can change during simulation
        require(found) {
            "Cannot make ingredient '${ingredient.getName()}' available because it does not exist in stock."
        }
    }

    /**
     * Explicit getter to get Ingredients if it's in the stock else null  .
     */
    fun getIngredient(ingredient: Ingredient): Ingredient? {
        for (key in ingredients.keys) {
            if (key.getName() == ingredient.getName()) {
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
            if (key.getName() == ingredient.getName()) {
                return value
            }
        }
        return false
    }
}
