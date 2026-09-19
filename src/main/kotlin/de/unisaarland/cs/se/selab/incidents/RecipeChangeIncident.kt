package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe

private const val PERCENT = 100L

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
        val percentage = PERCENT + adaptation // pls pass
        for (recipe in recipes) {
            val currentAmount = recipe.ingredients[ingredient] ?: continue
            val newAmount = Math.floorDiv(currentAmount * percentage, PERCENT).coerceIn(1L, Int.MAX_VALUE.toLong())
            recipe.ingredients[ingredient] = newAmount.toInt()
        }
    }
}
