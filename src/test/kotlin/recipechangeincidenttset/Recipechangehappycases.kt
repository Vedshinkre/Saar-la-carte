package recipechangeincidenttset

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.incidents.RecipeChangeIncident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class Recipechangehappycases {
    private fun createDummyIngredient(): Ingredient {
        // Mocking the ingredient constructor
        return mock<Ingredient>()
    }

    private fun createDummyRecipe(id: Int, ingredients: MutableMap<Ingredient, Int>): Recipe {
        // recipe constructor
        return Recipe(id, "Test Dish", 10, listOf(CookType.TOURNANT), ingredients, null)
    }

    @Test
    fun `apply - increase ingredient amount correctly`() {
        val wheat = createDummyIngredient()
        // Start with 100g
        val recipe = createDummyRecipe(1, mutableMapOf(wheat to 100))

        // +25% adaptation
        val incident = RecipeChangeIncident(1, 1, wheat, 25, listOf(recipe))
        incident.apply()

        // 100 * 1.25 = 125
        assertEquals(125, recipe.ingredients[wheat])
    }

    @Test
    fun `apply -decrease ingredient amount correctly`() {
        val wheat = createDummyIngredient()
        // Starts with 100g
        val recipe = createDummyRecipe(1, mutableMapOf(wheat to 100))

        // -20% adaptation
        val incident = RecipeChangeIncident(1, 1, wheat, -20, listOf(recipe))
        incident.apply()

        // 100 * 0.80 = 80
        assertEquals(80, recipe.ingredients[wheat])
    }

    @Test
    fun `apply - updates multiple recipes in the same list`() {
        val wheat = createDummyIngredient()

        // Two separate recipes with different starting amounts of wheat
        val breadRecipe = createDummyRecipe(1, mutableMapOf(wheat to 100))
        val cakeRecipe = createDummyRecipe(2, mutableMapOf(wheat to 200))

        // +50% adaptation applied to the list containing both
        val incident = RecipeChangeIncident(1, 1, wheat, 50, listOf(breadRecipe, cakeRecipe))
        incident.apply()

        // Both must be updated based on their individual starting amounts
        assertEquals(150, breadRecipe.ingredients[wheat]) // 100 -> 150
        assertEquals(300, cakeRecipe.ingredients[wheat]) // 200 -> 300
    }

    @Test
    fun `apply - massive decrease to a minimum of 1`() {
        val salt = createDummyIngredient()
        // Starts with 10g
        val recipe = createDummyRecipe(2, mutableMapOf(salt to 10))

        // -99% adaptation (would mathematically be 0.1)
        val incident = RecipeChangeIncident(1, 1, salt, -99, listOf(recipe))
        incident.apply()

        // Must hit the maxOf(1, ...) limit
        assertEquals(1, recipe.ingredients[salt])
    }

    @Test
    fun `apply - skip recipes that do not contain the ingredient`() {
        val wheat = createDummyIngredient()
        val sugar = createDummyIngredient()

        val breadRecipe = createDummyRecipe(1, mutableMapOf(wheat to 100))
        val mcSundaeRecipe = createDummyRecipe(2, mutableMapOf(sugar to 50))

        // Incident only affects wheat
        val incident = RecipeChangeIncident(1, 1, wheat, 50, listOf(breadRecipe, mcSundaeRecipe))
        incident.apply()

        // Bread changes (100 -> 150), mcSundae remains completely untouched
        assertEquals(150, breadRecipe.ingredients[wheat])
        assertEquals(50, mcSundaeRecipe.ingredients[sugar])
    }

    @Test
    fun `apply - rounds down fractional amounts`() {
        val wheat = createDummyIngredient()
        // Starts with 10g
        val recipe = createDummyRecipe(1, mutableMapOf(wheat to 10))

        // +19% adaptation (mathematically 11.9)
        val incident = RecipeChangeIncident(1, 1, wheat, 19, listOf(recipe))
        incident.apply()

        // 11.9 must be floored down to 11
        assertEquals(11, recipe.ingredients[wheat])
    }
}