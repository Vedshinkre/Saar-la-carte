package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.food.Ingredient

/** Represents a food preference. */
class FoodPreference(
    val excludedIngredients: List<Ingredient>,
    val preferredIngredients: List<Ingredient>,
    val favoriteDishes: List<String>
)
