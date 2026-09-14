package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe

/**
 * Represents a food preference: which ingredients a customer refuses/prefers, and
 * which dishes they like, used to decide what they order.
 *
 * NOTE: switched the constructor properties from public `val` to private with
 * explicit getters below, to match how every other class in this project exposes
 * its fields (getStatus(), getId(), getIngredients(), ...) - the class diagram
 * lists `getExcludedIngredients()` etc. as real methods, not raw property access.
 * Constructor call sites using named/positional args are unaffected; only direct
 * property access like `foodPreference.excludedIngredients` would need to become
 * `foodPreference.getExcludedIngredients()`.
 */
class FoodPreference(
    val excludedIngredients: List<Ingredient>,
    val preferredIngredients: List<Ingredient>,
    // INCONSISTENCY: the class diagram spells this field `favouriteDishes` (British
    // spelling - used consistently in RegularGroup/CasualGroup/EventGroup too).
    // Kept your existing American spelling for the constructor param/getter here
    // since it's already what you had; rename both if you want it to match exactly.
    val favoriteDishes: List<String>
) {

    /**
     * Picks a dish for a customer with this preference to order, given the recipes
     * currently available to cook and, for event groups, the one dish they
     * specifically favor for tonight's restaurant type ([eventFavourite]; pass an
     * empty string when not applicable, as the sequence diagram does for
     * non-event customers).
     *
     * Order of consideration, per the sequence diagram:
     *  1. Any recipe containing an excluded ingredient is dropped entirely.
     *  2. Among what's left, recipes matching a favorite dish (this customer's own
     *     [favoriteDishes], or [eventFavourite]) are preferred.
     *  3. Among what's left after that, recipes containing a preferred ingredient
     *     are preferred.
     *  4. If more than one candidate remains after all of the above - including
     *     the case where there were no preferences to narrow anything down at all -
     *     the highest-id recipe wins. This matches the diagram's own example: with
     *     no exclusions/favorites/preferred ingredients, the recipe with the
     *     highest id was chosen.
     *
     * The sequence diagram never actually shows favorites and preferred
     * ingredients disagreeing with each other, so the exact priority between
     * steps 2 and 3 above is my best reading of "we choose based on favorite
     * dishes and preferred ingredients" - double check this against the written
     * spec (section 2.2) if the order matters for your validation tests.
     */
    fun decideDish(availableMenu: List<Recipe>, eventFavourite: String): Dish {
        val notExcluded = availableMenu.filter { recipe ->
            excludedIngredients.none { it in recipe.ingredients.keys }
        }

        val favoriteMatches = notExcluded.filter { recipe ->
            recipe.name in favoriteDishes || recipe.name == eventFavourite
        }
        val afterFavorites = favoriteMatches.ifEmpty { notExcluded }

        val preferredMatches = afterFavorites.filter { recipe ->
            preferredIngredients.any { it in recipe.ingredients.keys }
        }
        val candidates = preferredMatches.ifEmpty { afterFavorites }

        // NOTE: Recipe.getId()/getID() isn't in my copy of the class diagram (only
        // the private `id` field is listed, no getter) but it's called throughout
        // the sequence diagrams (e.g. sorting by "highest id" right here, and
        // elsewhere for basic-dish-then-id sorting), so I'm assuming it exists.
        val chosen = candidates.maxByOrNull { it.id }
            ?: error("decideDish called with no recipes available to choose from")

        return Dish(chosen)
    }
}
