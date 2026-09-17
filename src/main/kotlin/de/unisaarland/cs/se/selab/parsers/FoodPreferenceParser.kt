package de.unisaarland.cs.se.selab.parsers

import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Parses and validates food preferences. */
class FoodPreferenceParser {
    private fun <T> JsonArray.parseListOf(itemParser: (JsonElement) -> T): List<T> = this.map(itemParser)

    /** Call with jsonArray containing FoodPreferences, full list of Recipes and Ingredients.
     *  Returns list of valid FoodPreferences or throws exception */
    @Throws
    fun parseFoodPreferences(
        jsonArray: JsonArray,
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        groupSize: Int
    ): List<FoodPreference> {
        val foodPreferencePairs: List<Pair<FoodPreference, Int>> = jsonArray.parseListOf { foodPreference ->
            parseFoodPreference(foodPreference.jsonObject, recipes, ingredients)
        }

        val foodPreferenceTotalSize: Int = foodPreferencePairs.sumOf { it.second }
        validateFoodPreferencesTotalSize(foodPreferenceTotalSize, groupSize)

        val emptyFoodPreference: Pair<FoodPreference, Int> =
            Pair(FoodPreference(emptyList(), emptyList(), emptyList()), groupSize - foodPreferenceTotalSize)
        return foodPreferencePairs.plus(emptyFoodPreference).flatMap { pair -> List(pair.second) { pair.first } }
    }

    private fun validateFoodPreferencesTotalSize(foodPreferenceTotalSize: Int, groupSize: Int) {
        require(groupSize >= foodPreferenceTotalSize)
    }

    @Throws
    private fun parseFoodPreference(
        jsonObject: JsonObject,
        recipes: List<Recipe>,
        ingredients: List<Ingredient>
    ): Pair<FoodPreference, Int> {
        fun JsonArray.toListOfString() = this.parseListOf { it.jsonPrimitive.content }

        val size: Int = jsonObject.getValue("size").jsonPrimitive.int
        val excludedIngredients: List<String> =
            jsonObject["excludedIngredients"]?.jsonArray?.toListOfString()?.distinctBy { it } ?: emptyList()
        val preferredIngredients: List<String> =
            jsonObject["preferredIngredients"]?.jsonArray?.toListOfString()?.distinctBy { it } ?: emptyList()
        val favoriteDishes: List<String> =
            jsonObject["favoriteDishes"]?.jsonArray?.toListOfString()?.distinctBy { it } ?: emptyList()

        validateFoodPreference(recipes, ingredients, excludedIngredients, preferredIngredients, favoriteDishes)

        return Pair(
            FoodPreference(
                excludedIngredients.map { excludedIngredient ->
                    ingredients.first {
                        it.name == excludedIngredient
                    }
                },
                preferredIngredients.map { preferredIngredient ->
                    ingredients.first {
                        it.name == preferredIngredient
                    }
                },
                favoriteDishes
            ),
            size
        )
    }

    private fun validateFoodPreference(
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        excludedIngredients: List<String>,
        preferredIngredients: List<String>,
        favoriteDishes: List<String>
    ) {
        validateFoodPreferenceExistence(recipes, ingredients, excludedIngredients, preferredIngredients, favoriteDishes)
        validateFoodPreferenceNoIntersection(excludedIngredients, preferredIngredients)
        validateFoodPreferenceNotEverything(
            recipes,
            ingredients,
            excludedIngredients,
            preferredIngredients,
            favoriteDishes
        )
    }

    private fun validateFoodPreferenceExistence(
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        excludedIngredients: List<String>,
        preferredIngredients: List<String>,
        favoriteDishes: List<String>
    ) {
        require(
            ingredients.map { it.name }.containsAll(
                excludedIngredients
            ) && ingredients.map { it.name }.containsAll(
                preferredIngredients
            ) && recipes.map { it.name }.containsAll(
                favoriteDishes
            )
        )
    }

    private fun validateFoodPreferenceNoIntersection(
        excludedIngredients: List<String>,
        preferredIngredients: List<String>
    ) {
        require(excludedIngredients.intersect(preferredIngredients.toSet()).isEmpty())
    }

    private fun validateFoodPreferenceNotEverything(
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        excludedIngredients: List<String>,
        preferredIngredients: List<String>,
        favoriteDishes: List<String>
    ) {
        require(
            !(
                excludedIngredients.containsAll(
                    ingredients.map {
                        it.name
                    }
                ) || preferredIngredients.containsAll(
                    ingredients.map {
                        it.name
                    }
                ) || favoriteDishes.containsAll(recipes.map { it.name })
                )
        )
    }
}
