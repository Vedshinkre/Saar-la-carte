package customer

import food.Ingredient

class FoodPreference(
    val excludedIngredients: List<Ingredient>,
    val preferredIngredients: List<Ingredient>,
    val favoriteDishNames: List<String>
)