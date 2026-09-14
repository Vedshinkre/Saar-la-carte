package menuavailabilitytests

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.Pantry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuAvailabilityCookTest {

    // --- Helpers ---

    private val testIngredient = Ingredient("tomato", MeasurementUnit.G, 50, 100)

    private fun createDummyRecipe(requiredAmount: Int): Recipe {
        return Recipe(
            id = 1,
            name = "Tomato Soup",
            duration = 10,
            cookType = listOf(CookType.TOURNANT),
            ingredients = mapOf(testIngredient to requiredAmount),
            basicDishFor = null
        )
    }

    private fun createDummyDish(recipe: Recipe): Dish {
        return Dish(recipe)
    }

    private fun createDummyCook(type: CookType, isCooking: Boolean): Cook {
        val cook = Cook(type)
        if (isCooking) {
            val recipe = createDummyRecipe(1)
            val dummyDish = createDummyDish(recipe)
            cook.startCooking(recipe, listOf(dummyDish), 1)
        }
        return cook
    }

    private fun setupCountertop(packages: List<IngredientPackage>, cooks: List<Cook>): Countertop {
        // Inject the packages directly into the primary constructor
        val testStock = Stock(listOf(testIngredient))
        val testSupplier = Supplier(testStock)

        val pantry = Pantry(
            inventory = packages.toMutableList(),
            supplier = testSupplier
        )

        return Countertop(
            pantry = pantry,
            orderQueue = ArrayDeque(),
            cooks = cooks
        )
    }

    // --- Tests ---

    @Test
    fun `getAvailableRecipes - Eligible Cook Is Free - Succeeds`() {
        val recipe = createDummyRecipe(requiredAmount = 50)
        val pkg = IngredientPackage(testIngredient)

        // Correct cook type and is free
        val cook = createDummyCook(CookType.TOURNANT, isCooking = false)

        val countertop = setupCountertop(listOf(pkg), listOf(cook))
        val available = countertop.getAvailableRecipes(listOf(recipe))

        assertTrue(available.contains(recipe))
    }

    @Test
    fun `getAvailableRecipes - Eligible Cook Is Busy - Excludes Recipe`() {
        val recipe = createDummyRecipe(requiredAmount = 50)
        val pkg = IngredientPackage(testIngredient)

        // Correct cook type but is currently cooking something else
        val cook = createDummyCook(CookType.TOURNANT, isCooking = true)

        val countertop = setupCountertop(listOf(pkg), listOf(cook))
        val available = countertop.getAvailableRecipes(listOf(recipe))

        assertFalse(available.contains(recipe))
    }

    @Test
    fun `getAvailableRecipes - No Cook Of Required Type - Excludes Recipe`() {
        val recipe = createDummyRecipe(requiredAmount = 50)
        val pkg = IngredientPackage(testIngredient)

        // Recipe requires TOURNANT, but we only have a PASTRY cook
        val cook = createDummyCook(CookType.PASTRY, isCooking = false)

        val countertop = setupCountertop(listOf(pkg), listOf(cook))
        val available = countertop.getAvailableRecipes(listOf(recipe))

        assertFalse(available.contains(recipe))
    }

    @Test
    fun `getAvailableRecipes - One Busy One Free Eligible Cook - Includes Recipe`() {
        val recipe = createDummyRecipe(requiredAmount = 50)
        val pkg = IngredientPackage(testIngredient)

        // Two eligible cooks: one is busy, but the other can take the order
        val busyCook = createDummyCook(CookType.TOURNANT, isCooking = true)
        val freeCook = createDummyCook(CookType.TOURNANT, isCooking = false)

        val countertop = setupCountertop(listOf(pkg), listOf(busyCook, freeCook))
        val available = countertop.getAvailableRecipes(listOf(recipe))

        assertTrue(available.contains(recipe))
    }
}
