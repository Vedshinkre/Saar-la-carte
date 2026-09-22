package menuselection

import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class Decidedishforcasuals {

    // Helpers
    private fun mockIngredient(name: String): Ingredient {
        val ingredient = mock<Ingredient>()
        whenever(ingredient.name).thenReturn(name)
        return ingredient
    }

    private fun mockRecipe(
        id: Int,
        name: String,
        ingredients: List<Ingredient>
    ): Recipe {
        val recipe = mock<Recipe>()
        whenever(recipe.id).thenReturn(id)
        whenever(recipe.name).thenReturn(name)
        // The FoodPreference logic checks `it in recipe.ingredients.keys`
        val ingredientMap = ingredients.associateWith { 1 }
        whenever(recipe.ingredients).thenReturn(ingredientMap as? MutableMap<Ingredient, Int>?)
        return recipe
    }

    // --- Tests ---

    @Test
    fun `decideDish returns null when all available recipes contain excluded ingredients`() {
        val tomato = mockIngredient("Tomato")
        val chicken = mockIngredient("Chicken")

        val recipe1 = mockRecipe(1, "Tomato Soup", listOf(tomato))
        val recipe2 = mockRecipe(2, "Chicken Stew", listOf(chicken, tomato))

        // Customer excludes tomatoes
        val preference = FoodPreference(
            excludedIngredients = listOf(tomato),
            preferredIngredients = emptyList(),
            favouriteDishes = emptyList()
        )

        val result = preference.decideDish(
            availableMenu = listOf(recipe1, recipe2),
            eventFavourite = "No Dish",
            restaurantType = RestaurantType.EUROPEAN
        )

        // Must return null because both dishes contain the excluded ingredient 'Tomato'
        assertNull(result, "Customer should reject all dishes containing excluded ingredients")
    }

    @Test
    fun `decideDish prioritizes the event favourite dish over personal favourites`() {
        val chicken = mockIngredient("Chicken")
        val beef = mockIngredient("Beef")

        val recipe1 = mockRecipe(1, "Chicken Stew", listOf(chicken))
        val recipe2 = mockRecipe(2, "Beef Roast", listOf(beef))

        // Customer's personal favorite is Chicken Stew
        val preference = FoodPreference(
            excludedIngredients = emptyList(),
            preferredIngredients = emptyList(),
            favouriteDishes = listOf("Chicken Stew")
        )

        val result = preference.decideDish(
            availableMenu = listOf(recipe1, recipe2),
            eventFavourite = "Beef Roast",
            restaurantType = RestaurantType.EUROPEAN
        )

        assertNotNull(result)
        assertEquals(2, result?.recipe?.id, "Event favorite should override personal favorite")
    }

    @Test
    fun `decideDish respects the order of personal favourite dishes`() {
        val pasta = mockIngredient("Pasta")
        val rice = mockIngredient("Rice")
        val potato = mockIngredient("Potato")

        val recipe1 = mockRecipe(1, "Pasta", listOf(pasta))
        val recipe2 = mockRecipe(2, "Rice", listOf(rice))
        val recipe3 = mockRecipe(3, "Potato", listOf(potato))

        // Customer prefers Rice over Pasta
        val preference = FoodPreference(
            excludedIngredients = emptyList(),
            preferredIngredients = emptyList(),
            favouriteDishes = listOf("Rice", "Pasta")
        )

        val result = preference.decideDish(
            availableMenu = listOf(recipe1, recipe2, recipe3),
            eventFavourite = "No Dish",
            restaurantType = RestaurantType.EUROPEAN
        )

        assertNotNull(result)
        assertEquals(2, result?.recipe?.id, "Customer should choose the first available personal favorite")
    }

    @Test
    fun `decideDish falls back to recipe with the highest count of preferred ingredients`() {
        val garlic = mockIngredient("Garlic")
        val onion = mockIngredient("Onion")
        val chicken = mockIngredient("Chicken")
        val beef = mockIngredient("Beef")

        // Has 1 preferred ingredient (Garlic)
        val recipe1 = mockRecipe(1, "Garlic Chicken", listOf(chicken, garlic))
        // Has 2 preferred ingredients (Garlic, Onion)
        val recipe2 = mockRecipe(2, "Garlic Onion Beef", listOf(beef, garlic, onion))
        // Has 0 preferred ingredients
        val recipe3 = mockRecipe(3, "Plain Chicken", listOf(chicken))

        val preference = FoodPreference(
            excludedIngredients = emptyList(),
            preferredIngredients = listOf(garlic, onion),
            favouriteDishes = emptyList() // No favorites, rely on ingredients
        )

        val result = preference.decideDish(
            availableMenu = listOf(recipe1, recipe2, recipe3),
            eventFavourite = "No Dish",
            restaurantType = RestaurantType.EUROPEAN
        )

        assertNotNull(result)
        assertEquals(2, result?.recipe?.id, "Should select recipe with the most preferred ingredients")
    }

    @Test
    fun `decideDish resolves ties by selecting the recipe with the highest ID`() {
        val chicken = mockIngredient("Chicken")
        val beef = mockIngredient("Beef")

        // Neither recipe contains any preferred ingredients, causing a 0-0 tie.
        val recipe1 = mockRecipe(1, "Chicken Dish", listOf(chicken))
        val recipe5 = mockRecipe(5, "Beef Dish", listOf(beef))
        val recipe2 = mockRecipe(2, "Another Dish", listOf(chicken, beef))

        val preference = FoodPreference(
            excludedIngredients = emptyList(),
            preferredIngredients = emptyList(),
            favouriteDishes = emptyList()
        )

        val result = preference.decideDish(
            availableMenu = listOf(recipe1, recipe5, recipe2),
            eventFavourite = "No Dish",
            restaurantType = RestaurantType.EUROPEAN
        )

        assertNotNull(result)
        // Highest ID among available recipes is 5
        assertEquals(5, result?.recipe?.id, "Ties must be broken by highest recipe ID")
    }
}
