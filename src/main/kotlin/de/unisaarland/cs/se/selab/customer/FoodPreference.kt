package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.food.Ingredient

class FoodPreference(
    val excludedIngredients: List<Ingredient>,
    val preferredIngredients: List<Ingredient>,
    val favoriteDishNames: List<String>
)
