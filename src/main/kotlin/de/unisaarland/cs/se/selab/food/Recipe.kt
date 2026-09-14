package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.RestaurantType

/**
 * Represents the Recipe of a dish.
 */
data class Recipe(
    val id: Int,
    val name: String,
    val duration: Int,
    val cookType: List<CookType>,
    val ingredients: Map<Ingredient, Int>,
    val basicDishFor: RestaurantType?
)
