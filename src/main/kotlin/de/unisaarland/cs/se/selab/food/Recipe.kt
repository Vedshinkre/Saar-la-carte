package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.RestaurantType

/**
 * Represents the Recipe of a dish.
 */
class Recipe(
    private val id: Int,
    private val name: String,
    private val duration: Int,
    private val cookType: List<CookType>,
    private val ingredients: Map<Ingredient, Int>,
    private val basicDishFor: RestaurantType?
){
    // explicit getters for relevant functions
    /**
     *explicit getter for ID.
     */
    fun getId(): Int {
        return id
    }
    /**
     *explicit getter for Ingredients in a recipe.
     */
    fun getIngredients(): Map<Ingredient, Int> {
        return ingredients
    }
    /**
     *explicit getter for Cook Duration of a recipe.
     */
    fun getDuration(): Int {
        return duration
    }
    /**
     *explicit getter for allowed Cooks to cook the recipe.
     */
    fun getCookTypes(): List<CookType> {
        return cookType
    }
    /**
     *explicit getter to get the recipe name.
     */
    fun getName(): String {
        return name
    }
    /**
     *explicit getter to get which Restaurant type the recipe is Basic For.
     */
    fun getBasicDishFor(): RestaurantType? {
        return basicDishFor
    }
}

