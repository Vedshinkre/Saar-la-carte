package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.RestaurantType

/**
 * Represents the Recipe of a dish.
 * @property id the unique numeric identifier of the recipe, used for menu ordering and tie-breaking.
 *  @property name the name of the recipe.
 *  @property duration the cooking and preparation time required in simulation ticks.
 *  @property cookType the list of [CookType] roles qualified to cook this recipe.
 *  @property ingredients a mutable mapping of required [Ingredient] types to their required quantities.
 *  @property basicDishFor the [RestaurantType] for which this recipe serves as a default basic dish,
 *  or `null` if it is not a basic dish
 */
data class Recipe(
    val id: Int,
    val name: String,
    val duration: Int,
    val cookType: List<CookType>,
    val ingredients: MutableMap<Ingredient, Int>,
    val basicDishFor: RestaurantType?
)
