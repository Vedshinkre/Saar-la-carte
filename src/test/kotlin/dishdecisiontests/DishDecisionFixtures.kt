package dishdecisiontests

import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe

internal const val SOUP = "Tomato Soup"
internal const val CURRY = "Chicken Curry"
internal const val STEW = "Lentil Stew"
internal const val TART = "Almond Tart"

/** a dish that is on no menu, e.g. a favourite dish of the event that the restaurant cannot offer */
internal const val GHOST = "Ghost Dish"

/** the amount of chicken in the curry, far more than any other ingredient in any dish */
private const val HEAVY_AMOUNT = 900

/**
 * Shared menus for the dish decision tests (spec 2.3 "Ordering").
 *
 * The recipe ids are 1 Tomato Soup, 2 Chicken Curry, 3 Lentil Stew and 4 Almond Tart, so the dish
 * with the highest recipe id, the default of a customer without preferences, is the Almond Tart.
 * The Tomato Soup is the only basic dish (of the EUROPEAN restaurants).
 */
internal object DishDecisionFixtures {
    val tomato = ingredient("tomato")
    val chicken = ingredient("chicken")
    val lentil = ingredient("lentil")
    val almond = ingredient("almond")

    /** an ingredient that is in no dish of any menu */
    val saffron = ingredient("saffron")

    val allIngredients: List<Ingredient> = listOf(tomato, chicken, lentil, almond)

    /** every dish has exactly one ingredient, the curry uses much more of it than the others do */
    val simpleMenu: List<Recipe> = listOf(
        recipe(3, STEW, mapOf(lentil to 1)),
        recipe(1, SOUP, mapOf(tomato to 1), RestaurantType.EUROPEAN),
        recipe(4, TART, mapOf(almond to 1)),
        recipe(2, CURRY, mapOf(chicken to HEAVY_AMOUNT))
    )

    /**
     * Same names and ids, but the curry contains lentils and the stew tomatoes too, so that the
     * number of preferred ingredients in a dish differs between dishes.
     */
    val mixedMenu: List<Recipe> = listOf(
        recipe(3, STEW, mapOf(lentil to 1, tomato to 1)),
        recipe(1, SOUP, mapOf(tomato to 1), RestaurantType.EUROPEAN),
        recipe(4, TART, mapOf(almond to 1)),
        recipe(2, CURRY, mapOf(chicken to HEAVY_AMOUNT, lentil to 1))
    )

    fun dish(menu: List<Recipe>, name: String): Recipe = menu.first { it.name == name }

    private fun ingredient(name: String) =
        Ingredient(name, MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)

    private fun recipe(id: Int, name: String, ingredients: Map<Ingredient, Int>, basicDishFor: RestaurantType? = null) =
        Recipe(
            id = id,
            name = name,
            duration = 10,
            cookType = listOf(CookType.TOURNANT),
            ingredients = ingredients.toMutableMap(),
            basicDishFor = basicDishFor
        )
}

/** the name of the dish a customer with these preferences decides on, `null` if there is none */
internal fun pick(
    menu: List<Recipe>,
    excluded: List<Ingredient> = emptyList(),
    preferred: List<Ingredient> = emptyList(),
    favourites: List<String> = emptyList(),
    eventDish: String = ""
): String? = FoodPreference(excluded, preferred, favourites)
    .decideDish(menu, eventDish, RestaurantType.EUROPEAN)?.recipe?.name
