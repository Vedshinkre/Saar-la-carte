package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe

/**
 * Represents a food preference: which ingredients a customer refuses/prefers, and
 * which dishes they like, used to decide what they order.
 */
class FoodPreference(
    val excludedIngredients: List<Ingredient>,
    val preferredIngredients: List<Ingredient>,
    val favouriteDishes: List<String>
) {

    /**
     * Picks a dish for a customer with this preference to order, given the recipes
     * currently available to cook, or `null` if none of the available recipes work
     * for them at all.
     *
     * For event groups, [eventFavourite] is the one dish they favor specifically for
     * tonight's restaurant type; pass an empty string when not applicable (i.e. for
     * non-event customers).
     *
     * Selection order:
     *  1. Only recipes containing none of [excludedIngredients] are considered at
     *     all. If none qualify, return `null`.
     *  2. For event customers ([eventFavourite] non-empty): if a qualifying recipe
     *     matches [eventFavourite], it's chosen outright - this takes priority over
     *     the customer's own favorite dishes or preferred ingredients.
     *  3. Otherwise (or if the event favorite wasn't available): walk
     *     [favouriteDishes] in order and return the first one that matches a
     *     qualifying recipe.
     *  4. If no favorite matched (including the case of no favorites at all),
     *     narrow to whichever qualifying recipe(s) contain the most distinct
     *     [preferredIngredients] - counted by enumeration (how many of them
     *     appear in the recipe), not by quantity. With no preferred ingredients
     *     either, every recipe ties at zero here.
     *  5. Any remaining tie (including the "no preferences at all" case) is
     *     broken by the highest recipe id.
     */
    fun decideDish(availableMenu: List<Recipe>, eventFavourite: String, restaurantType: RestaurantType): Dish? {
        val notExcluded = availableMenu.filter { recipe ->
            excludedIngredients.none { it in recipe.ingredients.keys }
        }
        if (notExcluded.isEmpty()) {
            return null
        }

        if (eventFavourite != "") {
            val eventMatch = notExcluded.firstOrNull { it.name == eventFavourite }
            if (eventMatch != null) {
                return Dish(eventMatch, restaurantType)
            }
        }

        val favouriteMatch = firstMatchingFavorite(notExcluded)
        if (favouriteMatch != null) {
            return Dish(favouriteMatch, restaurantType)
        }

        val chosen = mostPreferredIngredients(notExcluded).maxByOrNull { it.id } ?: return null
        return Dish(chosen, restaurantType)
    }

    /**
     * The first recipe in [candidates] whose name matches this customer's
     * favorite dishes, trying [favouriteDishes] in order (not menu order) so that
     * their most-preferred favorite wins if more than one happens to be available.
     */
    private fun firstMatchingFavorite(candidates: List<Recipe>): Recipe? {
        for (favoriteName in favouriteDishes) {
            val match = candidates.firstOrNull { it.name == favoriteName }
            if (match != null) {
                return match
            }
        }
        return null
    }

    /**
     * The subset of [candidates] containing the most [preferredIngredients],
     * counted by how many distinct preferred ingredients appear in the recipe
     * (not by the quantities used). If [preferredIngredients] is empty, or none
     * of it appears in any candidate, every candidate ties at zero and the whole
     * list is returned unchanged.
     */
    private fun mostPreferredIngredients(candidates: List<Recipe>): List<Recipe> {
        val maxCount = candidates.maxOf { recipe ->
            preferredIngredients.count { it in recipe.ingredients.keys }
        }
        return candidates.filter { recipe ->
            preferredIngredients.count { it in recipe.ingredients.keys } == maxCount
        }
    }
}
