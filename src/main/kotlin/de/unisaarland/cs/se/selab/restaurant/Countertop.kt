package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe

/**
 * Manages ingredient reservations and dish availability for the restaurant.
 */
class Countertop(
    private val pantry: Pantry,
    private val orderQueue: ArrayDeque<Order>,
    private val cooks: List<Cook>,
    /** the type of the restaurant this counter belongs to; decides which recipes count as basic here */
    val restaurantType: RestaurantType
) {
    /** (F13)
     * Returns the recipes from the menu that are currently available to order.
     *
     * A recipe is available if all required ingredients are available in the
     * pantry and the restaurant employs at least one cook who can prepare the
     * recipe - whether or not that cook is busy cooking something else.
     *
     * @param menu the restaurant's menu
     * @return the recipes that can currently be ordered
     */
    fun getAvailableRecipes(menu: List<Recipe>): List<Recipe> {
        val availableRecipes: MutableList<Recipe> = mutableListOf()
        menu.forEach { recipe ->
            // available flag if there are enough packages for all required ingredients
            var available = true
            val ingredients = recipe.ingredients
            ingredients.forEach { (ingredient: Ingredient, amount: Int) ->
                if (availableAmount(ingredient) < amount) {
                    available = false
                }
            }
            val cookTypes = recipe.cookType
            val eligibleCooks = cooks.filter { cookTypes.contains(it.type) }

            if (available && eligibleCooks.isNotEmpty()) {
                availableRecipes.add(recipe)
            }
        }
        return availableRecipes
    }

    /**
     * Sums the non-expired stock currently in the pantry for a single ingredient.
     */
    private fun availableAmount(ingredient: Ingredient): Int {
        val ingredientPackages = pantry.getPackagesForIngredient(ingredient)
        var totalAmount = 0
        ingredientPackages.forEach {
            if (!it.hasExpired()) {
                totalAmount += it.currentAmount
            }
        }
        return totalAmount
    }

    /** Reserves the ingredients of one [recipe] in the pantry when a customer orders it. */
    fun reserveIngredients(recipe: Recipe) {
        val ingredients = recipe.ingredients
        pantry.reserveIngredients(ingredients)
    }

    /** Queues [newOrder] for the kitchen; the queue is shared with it. */
    fun addOrder(newOrder: Order) {
        orderQueue.add(newOrder)
    }
}
