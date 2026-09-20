package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.max

private const val PERCENT = 100

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
     * in every recipe that uses it by [adaptation] percent). A menu copy of a recipe shares the
     * ingredient map of the original, so each map is only changed once.
     */
    override fun apply() {
        val percentage = PERCENT + adaptation
        val changedIngredientMaps = Collections.newSetFromMap(IdentityHashMap<MutableMap<Ingredient, Int>, Boolean>())
        for (recipe in recipes) {
            if (!changedIngredientMaps.add(recipe.ingredients)) continue
            val currentAmount = recipe.ingredients[ingredient] ?: continue
            // Long arithmetic: a big amount times a big percentage overflows Int before the division
            val scaled = Math.floorDiv(currentAmount.toLong() * percentage, PERCENT.toLong())
            recipe.ingredients[ingredient] = max(scaled, 1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    }
}
