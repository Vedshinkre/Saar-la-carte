package cookingtests

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.CookResult
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** tests for [Cook] (F12): start to cook assigned recipe, process through remaining ticks, and finishing meals */
class CookUnitTest {

    private val ingredient = Ingredient("onion", MeasurementUnit.X, 5, 10)

    private fun recipe(
        id: Int = 1,
        duration: Int,
        name: String = "Soup",
        type: CookType = CookType.TOURNANT
    ): Recipe {
        return Recipe(
            id = id,
            name = name,
            duration = duration,
            cookType = listOf(type),
            ingredients = mutableMapOf(ingredient to 1),
            basicDishFor = RestaurantType.EUROPEAN
        )
    }

    private fun dishesOf(recipe: Recipe, count: Int): List<Dish> = List(count) { Dish(recipe) }

    // --- construction ---

    @Test
    fun `constructor - negative remainingTicks - throws`() {
        assertThrows<IllegalArgumentException> {
            Cook(
                id = null,
                orderId = null,
                type = CookType.TOURNANT,
                currentRecipe = null,
                remainingTicks = -1,
                isCooking = false
            )
        }
    }

    @Test
    fun `constructor - zero remainingTicks - succeeds`() {
        val cook = Cook(
            id = null,
            orderId = null,
            type = CookType.TOURNANT,
            currentRecipe = null,
            remainingTicks = 0,
            isCooking = false
        )

        assertFalse(cook.isCooking)
    }

    @Test
    fun `type-only constructor - starts idle without id or assigned dishes`() {
        val cook = Cook(CookType.SAUCE)

        assertNull(cook.id)
        assertNull(cook.orderId)
        assertNull(cook.currentRecipe)
        assertFalse(cook.isCooking)
        assertTrue(cook.getDishes().isEmpty())
    }

    // --- setId ---

    @Test
    fun `setId - no id yet - assigns given id`() {
        val cook = Cook(CookType.TOURNANT)

        cook.setId(4)

        assertEquals(4, cook.id)
    }

    @Test
    fun `setId - id already assigned - keeps original id`() {
        val cook = Cook(CookType.TOURNANT)
        cook.setId(4)

        cook.setId(9)

        assertEquals(4, cook.id)
    }

    // --- startCooking ---

    @Test
    fun `startCooking - short recipe - zero remaining ticks and dishes marked COOKING`() {
        val recipe = recipe(duration = 10) // ceil(10/10) - 1 = 0
        val cook = Cook(CookType.TOURNANT)
        val dishList = dishesOf(recipe, 2)

        cook.startCooking(recipe, dishList, baseOrderId = 7)

        assertTrue(cook.isCooking)
        assertSame(recipe, cook.currentRecipe)
        assertEquals(7, cook.orderId)
        assertEquals(2, cook.getDishes().size)
        dishList.forEach { assertEquals(DishStatus.COOKING, it.status) }
    }

    @Test
    fun `startCooking - long recipe - needs multiple ticks before finishing`() {
        val recipe = recipe(duration = 30) // ceil(30/10) - 1 = 2
        val cook = Cook(CookType.TOURNANT)
        cook.startCooking(recipe, dishesOf(recipe, 1), baseOrderId = 1)

        assertEquals(CookResult(true, 1, 0), cook.cookDishes())
        assertEquals(CookResult(true, 1, 0), cook.cookDishes())
        assertEquals(CookResult(true, 1, 1), cook.cookDishes())
    }

    @Test
    fun `startCooking - reassigning cook to a new recipe - clears previous dish list`() {
        val recipeOne = recipe(id = 1, duration = 10, name = "Soup")
        val recipeTwo = recipe(id = 2, duration = 10, name = "Stew")
        val cook = Cook(CookType.TOURNANT)
        cook.startCooking(recipeOne, dishesOf(recipeOne, 3), baseOrderId = 1)

        cook.startCooking(recipeTwo, dishesOf(recipeTwo, 1), baseOrderId = 2)

        assertEquals(1, cook.getDishes().size)
        assertSame(recipeTwo, cook.currentRecipe)
        assertEquals(2, cook.orderId)
    }

    @Test
    fun `cookDishes - not cooking - returns inactive result without side effects`() {
        val cook = Cook(CookType.TOURNANT)

        val result = cook.cookDishes()

        assertEquals(CookResult(false, 0, 0), result)
        assertFalse(cook.isCooking)
    }

    @Test
    fun `cookDishes - zero-tick recipe - finishes in the same call it was started`() {
        val recipe = recipe(duration = 10)
        val cook = Cook(CookType.TOURNANT)
        val dishList = dishesOf(recipe, 3)
        cook.startCooking(recipe, dishList, baseOrderId = 5)

        val result = cook.cookDishes()

        assertEquals(CookResult(true, 3, 3), result)
        assertFalse(cook.isCooking)
        assertNull(cook.currentRecipe)
        assertNull(cook.orderId)
        assertTrue(cook.getDishes().isEmpty())
        dishList.forEach { assertEquals(DishStatus.COOKED, it.status) }
    }

    @Test
    fun `cookDishes - multi-tick recipe - stays cooking until the final tick`() {
        val recipe = recipe(duration = 30) // 2 remaining ticks
        val cook = Cook(CookType.TOURNANT)
        val dishList = dishesOf(recipe, 2)
        cook.startCooking(recipe, dishList, baseOrderId = 9)

        val firstTick = cook.cookDishes()
        assertEquals(CookResult(true, 2, 0), firstTick)
        assertTrue(cook.isCooking)
        dishList.forEach { assertEquals(DishStatus.COOKING, it.status) }

        val secondTick = cook.cookDishes()
        assertEquals(CookResult(true, 2, 0), secondTick)
        assertTrue(cook.isCooking)

        val finalTick = cook.cookDishes()
        assertEquals(CookResult(true, 2, 2), finalTick)
        assertFalse(cook.isCooking)
        dishList.forEach { assertEquals(DishStatus.COOKED, it.status) }
    }

    @Test
    fun `cookDishes - after finishing - returns inactive result again`() {
        val recipe = recipe(duration = 10)
        val cook = Cook(CookType.TOURNANT)
        cook.startCooking(recipe, dishesOf(recipe, 1), baseOrderId = 1)
        cook.cookDishes() // finishes immediately

        val result = cook.cookDishes()

        assertEquals(CookResult(false, 0, 0), result)
    }

    @Test
    fun `cookDishes - id keeps its value across cooking and finishing`() {
        val recipe = recipe(duration = 10)
        val cook = Cook(CookType.TOURNANT)
        cook.setId(3)
        cook.startCooking(recipe, dishesOf(recipe, 1), baseOrderId = 1)

        cook.cookDishes()

        // id is retained for the rest of the evening even though the assignment is done
        assertEquals(3, cook.id)
    }

    // --- setRemainingTicks / getDishes exposure ---

    @Test
    fun `setRemainingTicks - overrides the remaining duration`() {
        val recipe = recipe(duration = 30) // would normally need 2 more ticks
        val cook = Cook(CookType.TOURNANT)
        cook.startCooking(recipe, dishesOf(recipe, 1), baseOrderId = 1)

        cook.setRemainingTicks(0)

        assertEquals(CookResult(true, 1, 1), cook.cookDishes())
    }

    @Test
    fun `getDishes - reflects the live internal list until it is cleared`() {
        val recipe = recipe(duration = 30)
        val cook = Cook(CookType.TOURNANT)
        val dishList = dishesOf(recipe, 2)
        cook.startCooking(recipe, dishList, baseOrderId = 1)

        assertEquals(2, cook.getDishes().size)

        cook.setRemainingTicks(0)
        cook.cookDishes()

        assertTrue(cook.getDishes().isEmpty())
    }
}
