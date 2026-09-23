package menuselection

import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.Countertop
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * tests the dish selection (f13 and f 26 of only casuals)
 */
class CasualDishSelectionTest {

    private fun mockIngredient(name: String): Ingredient {
        val ingredient = mock<Ingredient>()
        whenever(ingredient.name).thenReturn(name)
        return ingredient
    }

    private fun mockRecipe(id: Int, name: String, ingredients: List<Ingredient>): Recipe {
        val recipe = mock<Recipe>()
        whenever(recipe.id).thenReturn(id)
        whenever(recipe.name).thenReturn(name)
        val ingredientMap = ingredients.associateWith { 1 }
        whenever(recipe.ingredients).thenReturn(ingredientMap.toMutableMap())
        return recipe
    }

    private fun mockWaiter(): Waiter {
        val waiter = mock<Waiter>()
        whenever(waiter.getTickLoad(ActionType.TAKE_ORDER)).thenReturn(0)
        return waiter
    }

    @Test
    fun `casual customer rejects dishes with excluded ingredients and selects highest ID on tie`() {
        val tomato = mockIngredient("Tomato")
        val pasta = mockIngredient("Pasta")
        val cheese = mockIngredient("Cheese")

        val recipe1 = mockRecipe(1, "Tomato Pasta", listOf(tomato, pasta))
        val recipe2 = mockRecipe(2, "Cheese Pasta", listOf(cheese, pasta))
        val recipe5 = mockRecipe(5, "Plain Pasta", listOf(pasta))

        val preference = FoodPreference(
            excludedIngredients = listOf(tomato),
            preferredIngredients = emptyList(),
            favouriteDishes = emptyList()
        )

        // Event favorite is passed as empty string for casual groups
        val dish = preference.decideDish(
            availableMenu = listOf(recipe1, recipe2, recipe5),
            eventFavourite = "",
            restaurantType = RestaurantType.EUROPEAN
        )

        assertNotNull(dish)
        assertEquals(5, dish?.recipe?.id, "Must exclude Tomato Pasta and pick highest ID (5) between 2 and 5")
    }

    @Test
    fun `casual customer picks personal favourite in defined order`() {
        val pasta = mockIngredient("Pasta")
        val beef = mockIngredient("Beef")

        val recipe1 = mockRecipe(1, "Pasta Bolognese", listOf(pasta, beef))
        val recipe2 = mockRecipe(2, "Steak", listOf(beef))

        val preference = FoodPreference(
            excludedIngredients = emptyList(),
            preferredIngredients = emptyList(),
            favouriteDishes = listOf("Steak", "Pasta Bolognese")
        )

        val dish = preference.decideDish(
            availableMenu = listOf(recipe1, recipe2),
            eventFavourite = "",
            restaurantType = RestaurantType.EUROPEAN
        )

        assertNotNull(dish)
        assertEquals(2, dish?.recipe?.id, "Must pick Steak because it appears first in favouriteDishes")
    }

    @Test
    fun `casual group receives negative experience when a member cannot order a dish`() {
        val tomato = mockIngredient("Tomato")
        val recipe1 = mockRecipe(1, "Tomato Soup", listOf(tomato))

        // Member cannot eat tomatoes; menu only has Tomato Soup
        val strictPref = FoodPreference(
            excludedIngredients = listOf(tomato),
            preferredIngredients = emptyList(),
            favouriteDishes = emptyList()
        )

        val casualGroup = CasualGroup(
            id = 2,
            size = 1,
            tableType = TableType.COMMON,
            visitingAt = 1,
            foodPreferences = listOf(strictPref),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = 0,
            ratingLikelihood = RatingLikelihood.ALWAYS
        )

        val countertop = mock<Countertop>()
        whenever(countertop.restaurantType).thenReturn(RestaurantType.EUROPEAN)
        whenever(countertop.getAvailableRecipes(any())).thenReturn(listOf(recipe1))

        val placed = casualGroup.placeOrder(listOf(mockWaiter()), listOf(recipe1), countertop)

        assertFalse(placed, "Order should fail when 0 dishes could be ordered")
        assertEquals(0, casualGroup.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
    }

    @Test
    fun `casual group orders in sequence of most excluded ingredients then fewest favourites`() {
        val tomato = mockIngredient("Tomato")
        val cheese = mockIngredient("Cheese")
        val beef = mockIngredient("Beef")
        val chicken = mockIngredient("Chicken")

        val beefDish = mockRecipe(1, "Beef Dish", listOf(beef))
        val chickenDish = mockRecipe(2, "Chicken Dish", listOf(chicken))

        // Customer A: 1 exclusion, favorite is Beef Dish
        val prefA = FoodPreference(
            excludedIngredients = listOf(tomato),
            preferredIngredients = emptyList(),
            favouriteDishes = listOf("Beef Dish")
        )
        // Customer B: 2 exclusions, favorite is Chicken Dish (must order FIRST)
        val prefB = FoodPreference(
            excludedIngredients = listOf(tomato, cheese),
            preferredIngredients = emptyList(),
            favouriteDishes = listOf("Chicken Dish")
        )

        val casualGroup = CasualGroup(
            id = 1,
            size = 2,
            tableType = TableType.COMMON,
            visitingAt = 1,
            foodPreferences = listOf(prefA, prefB), // prefA is passed first in the list
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = 0,
            ratingLikelihood = RatingLikelihood.ALWAYS
        )

        val countertop = mock<Countertop>()
        whenever(countertop.restaurantType).thenReturn(RestaurantType.EUROPEAN)
        whenever(countertop.getAvailableRecipes(any())).thenReturn(listOf(beefDish, chickenDish))

        val placed = casualGroup.placeOrder(listOf(mockWaiter()), listOf(beefDish, chickenDish), countertop)

        assertTrue(placed)
        val dishes = casualGroup.currentOrder?.dishes.orEmpty()
        assertEquals(2, dishes.size)

        // PROOF OF SEQUENCE: Customer B (Chicken) ordered first, Customer A (Beef) ordered second
        assertEquals("Chicken Dish", dishes[0].recipe.name, "Customer B with 2 exclusions must order first")
        assertEquals("Beef Dish", dishes[1].recipe.name, "Customer A with 1 exclusion must order second")
    }

    @Test
    fun `casual customer falls back to recipe with the highest count of preferred ingredients`() {
        val garlic = mockIngredient("Garlic")
        val onion = mockIngredient("Onion")
        val chicken = mockIngredient("Chicken")
        val beef = mockIngredient("Beef")

        val recipe1 = mockRecipe(1, "Garlic Chicken", listOf(chicken, garlic)) // 1 preferred
        val recipe2 = mockRecipe(2, "Garlic Onion Beef", listOf(beef, garlic, onion)) // 2 preferred
        val recipe3 = mockRecipe(3, "Plain Chicken", listOf(chicken)) // 0 preferred

        val preference = FoodPreference(
            excludedIngredients = emptyList(),
            preferredIngredients = listOf(garlic, onion),
            favouriteDishes = emptyList()
        )

        val dish = preference.decideDish(
            availableMenu = listOf(recipe1, recipe2, recipe3),
            eventFavourite = "",
            restaurantType = RestaurantType.EUROPEAN
        )

        assertNotNull(dish)
        assertEquals(2, dish?.recipe?.id, "Should select recipe with the most distinct preferred ingredients")
    }
}
