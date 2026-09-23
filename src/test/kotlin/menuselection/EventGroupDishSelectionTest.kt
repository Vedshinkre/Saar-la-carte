package menuselection

import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.Countertop
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests that preferences are taken into account for EventGroups during dish decision.
 */
class EventGroupDishSelectionTest {

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
    fun `Event Favorite Overrides Personal Favorite`() {
        val chicken = mockIngredient("Chicken")
        val beef = mockIngredient("Beef")
        val garlic = mockIngredient("Garlic")

        val personalFavorite = mockRecipe(1, "Garlic Chicken", listOf(chicken, garlic))
        val eventFavorite = mockRecipe(2, "Roast Beef", listOf(beef))

        val preference = FoodPreference(
            emptyList(),
            listOf(garlic),
            listOf("Garlic Chicken")
        )

        val dish = preference.decideDish(
            listOf(personalFavorite, eventFavorite),
            "Roast Beef",
            RestaurantType.EUROPEAN
        )

        assertNotNull(dish)
        assertEquals(2, dish?.recipe?.id)
    }

    @Test
    fun `Event Favorite Excluded`() {
        val mushroom = mockIngredient("Mushroom")
        val chicken = mockIngredient("Chicken")

        val personalFavorite = mockRecipe(1, "Grilled Chicken", listOf(chicken))
        val eventFavorite = mockRecipe(2, "Mushroom Risotto", listOf(mushroom))

        val preference = FoodPreference(
            listOf(mushroom),
            emptyList(),
            listOf("Grilled Chicken")
        )

        val dish = preference.decideDish(
            listOf(personalFavorite, eventFavorite),
            "Mushroom Risotto",
            RestaurantType.EUROPEAN
        )

        assertNotNull(dish)
        assertEquals(1, dish?.recipe?.id)
    }

    @Test
    fun `EventGroup Orders Using Correct Favorite Dish`() {
        val chicken = mockIngredient("Chicken")
        val banquetDish = mockRecipe(10, "Banquet Chicken", listOf(chicken))
        val preference = FoodPreference(
            emptyList(),
            emptyList(),
            emptyList()
        )

        val eventGroup = EventGroup(
            5,
            2,
            TableType.COMMON,
            1,
            listOf(preference, preference),
            listOf(RestaurantType.EUROPEAN),
            1,
            mapOf(RestaurantType.EUROPEAN to "Banquet Chicken", RestaurantType.ASIAN to "Garlic Chicken")
        )
        eventGroup.currentRestaurantType = RestaurantType.EUROPEAN

        val waiter = mockWaiter()
        val waiterQuotaMap = mutableMapOf(waiter to 5)

        val countertop = mock<Countertop>()
        whenever(countertop.restaurantType).thenReturn(RestaurantType.EUROPEAN)
        whenever(countertop.getAvailableRecipes(any())).thenReturn(listOf(banquetDish))

        val somebodyOrdered = eventGroup.placeOrder(waiterQuotaMap, listOf(banquetDish), countertop)

        assertTrue(somebodyOrdered)
        val order = eventGroup.currentOrder
        assertNotNull(order)
        assertEquals(2, order?.dishes?.size)
        assertTrue(order?.dishes?.all { it.recipe.name == "Banquet Chicken" } == true)
        assertEquals(3, waiterQuotaMap[waiter])
    }
}
