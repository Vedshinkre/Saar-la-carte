package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe

/**
 * Manages ingredient reservations and dish availability for the restaurant.
 */
class Countertop(
    private val pantry: Pantry,
    private val orderQueue: ArrayDeque<Order>,
    private val cooks: List<Cook>
) {
    /** (F13)
     * Returns the recipes from the menu that are currently available to order.
     *
     * A recipe is available if all required ingredients are available in the
     * pantry and at least one cook can prepare the recipe.
     *
     * @param menu the restaurant's menu
     * @return the recipes that can currently be ordered
     */
    fun getAvailableRecipes(menu: List<Recipe>): List<Recipe> {
        val availableRecipes: MutableList<Recipe> = mutableListOf()
        menu.forEach { recipe ->
            // available flag if there are enough packages for all required ingredients
            var available = true
            val ingredients = recipe.getIngredients()
            ingredients.forEach { (ingredient: Ingredient, amount: Int) ->
                val ingredientPackages = pantry.getPackagesForIngredient(ingredient)
                var totalAmount = 0
                ingredientPackages.forEach {
                    totalAmount += it.getCurrentAmount()
                }
                if (totalAmount < amount) {
                    available = false
                }
            }
            val cookTypes = recipe.getCookTypes()
            val eligibleCooks = cooks.filter { !it.getIsCooking() && cookTypes.contains(it.getCookType()) }

            if (available && eligibleCooks.isNotEmpty()) {
                availableRecipes.add(recipe)
            }
        }
        return availableRecipes
    }
}
