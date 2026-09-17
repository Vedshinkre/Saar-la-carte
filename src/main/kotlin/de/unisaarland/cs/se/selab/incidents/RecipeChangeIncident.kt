package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Constants.HUNDRED
import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import kotlin.math.floor

/**
 * Incident that changes the amount of a specific ingredient
 * across all recipes that use it, by a relative percentage.
 */
class RecipeChangeIncident(
    override val id: Id,
    override val evening: Evening,
    private val ingredient: Ingredient,
    private val adaptation: Int,
    private val recipes: List<Recipe>
) : Incident(id, evening) {

    override val type: String = "RECIPE"

    /**
     * applies the recipe change incident (adapts the amount of [ingredient]
     * in every recipe that uses it by [adaptation] percent)
     */
    override fun apply() {
        val factor = 1.0 + adaptation / HUNDRED
        for (recipe in recipes) {
            val currentAmount = recipe.ingredients[ingredient] ?: continue
            val newAmount = maxOf(1, floor(currentAmount * factor).toInt())
            recipe.ingredients[ingredient] = newAmount
        }
    }
}
