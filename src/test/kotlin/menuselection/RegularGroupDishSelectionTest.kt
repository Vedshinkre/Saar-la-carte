package menuselection

import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.Countertop
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests that preferences are taken into account for RegularGroups during dish decision.
 */
class RegularGroupDishSelectionTest {

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
    fun `RegularGroup Orders Adaptively Using Countertop`() {
        val tomato = mockIngredient("Tomato")
        val cheese = mockIngredient("Cheese")

        val soup = mockRecipe(1, "Tomato Soup", listOf(tomato))
        val pizza = mockRecipe(2, "Cheese Pizza", listOf(cheese))

        val preference1 = FoodPreference(
            emptyList(),
            emptyList(),
            listOf("Tomato Soup")
        )
        val preference2 = FoodPreference(
            emptyList(),
            emptyList(),
            listOf("Tomato Soup", "Cheese Pizza")
        )

        val regularGroup = RegularGroup(
            10,
            2,
            TableType.COMMON,
            1,
            listOf(preference1, preference2),
            1,
            1,
            1
        )

        val countertop = mock<Countertop>()
        whenever(countertop.restaurantType).thenReturn(RestaurantType.EUROPEAN)

        whenever(countertop.getAvailableRecipes(any()))
            .thenReturn(listOf(soup, pizza))
            .thenReturn(listOf(pizza))

        val somebodyOrdered = regularGroup.placeOrder(listOf(mockWaiter()), listOf(soup, pizza), countertop)

        assertTrue(somebodyOrdered)
        val dishes = regularGroup.currentOrder?.dishes.orEmpty()
        assertEquals(2, dishes.size)
        assertEquals("Tomato Soup", dishes[0].recipe.name)
        assertEquals("Cheese Pizza", dishes[1].recipe.name)
    }

    @Test
    fun `RegularGroup Order History Updates Correctly`() {
        val regularGroup = RegularGroup(
            11,
            1,
            TableType.COMMON,
            1,
            emptyList(),
            1,
            1,
            1
        )

        val order1 = Order(emptyList())
        val order2 = Order(emptyList())
        val order3 = Order(emptyList())
        val order4 = Order(emptyList())

        regularGroup.addOrderToHistory(order1)
        regularGroup.addOrderToHistory(order2)
        regularGroup.addOrderToHistory(order3)
        assertEquals(3, regularGroup.orderHistory.size)
        assertEquals(order1, regularGroup.orderHistory.first())

        regularGroup.addOrderToHistory(order4)
        assertEquals(3, regularGroup.orderHistory.size)
        assertEquals(order2, regularGroup.orderHistory.first())
        assertEquals(order4, regularGroup.orderHistory.last())
    }
}
