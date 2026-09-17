package cookingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Kitchen
import de.unisaarland.cs.se.selab.restaurant.Pantry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

/** Integration tests for [Kitchen]'s cooking (F12) */
class KitchenCookingProcessTest {

    private lateinit var output: StringWriter
    private val ingredient = Ingredient("onion", MeasurementUnit.X, 5, 10)

    @BeforeEach
    fun setUp() {
        Time.tick = 1
        Time.evening = 1
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    private fun recipe(
        id: Int = 1,
        duration: Int,
        name: String = "Soup",
        type: CookType = CookType.TOURNANT,
        basicFor: RestaurantType? = RestaurantType.EUROPEAN
    ): Recipe {
        return Recipe(
            id = id,
            name = name,
            duration = duration,
            cookType = listOf(type),
            ingredients = mutableMapOf(ingredient to 1),
            basicDishFor = basicFor
        )
    }

    private fun emptyPantry(): Pantry = Pantry(Stock(listOf(ingredient)))

    private fun kitchen(cooks: List<Cook>, orderQueue: MutableList<Order> = mutableListOf()): Kitchen {
        return Kitchen(cooks, emptyPantry(), orderQueue, RestaurantType.EUROPEAN)
    }

    /** A cook constructed with an explicit (possibly null) id, without ever having cooked. */
    private fun rawCook(id: Int?): Cook {
        return Cook(
            id = id,
            orderId = null,
            type = CookType.TOURNANT,
            currentRecipe = null,
            remainingTicks = 0,
            isCooking = false
        )
    }

    // private fun logContains(expected: String): Boolean = output.toString().contains(expected)

    // --- getServableDishesNumber ---

    @Test
    fun `getServableDishesNumber - counts only COOKED dishes still queued`() {
        val recipe = recipe(duration = 10)
        val cookedDish = Dish(recipe).apply { status = DishStatus.COOKED }
        val uncookedDish = Dish(recipe)
        val servedDish = Dish(recipe).apply { status = DishStatus.SERVED }
        val order = Order(listOf(cookedDish, uncookedDish, servedDish))
        val k = kitchen(listOf(Cook(CookType.TOURNANT)), mutableListOf(order))

        assertEquals(1, k.getServableDishesNumber())
    }

    @Test
    fun `getServableDishesNumber - empty order queue - is zero`() {
        val k = kitchen(listOf(Cook(CookType.TOURNANT)))

        assertEquals(0, k.getServableDishesNumber())
    }

    // --- statistics accumulate across ticks ---

    @Test
    fun `numberOfCookedMeals - accumulates finished meals across multiple ticks`() {
        val quickRecipe = recipe(id = 1, duration = 10, name = "Potato Soup")
        val slowRecipe = recipe(id = 2, duration = 30, name = "Chicken Rice", type = CookType.ROAST)
        val quickDish = Dish(quickRecipe)
        val slowDish = Dish(slowRecipe)
        val orderOne = Order(listOf(quickDish))
        val orderTwo = Order(listOf(slowDish))
        val cooks = listOf(Cook(CookType.TOURNANT), Cook(CookType.ROAST))
        val k = kitchen(cooks, mutableListOf(orderOne, orderTwo))

        k.processCooking() // quick dish finishes this tick, slow dish starts
        assertEquals(1, k.numberOfCookedMeals)

        Time.tick = 2
        k.processCooking() // slow dish still cooking
        assertEquals(1, k.numberOfCookedMeals)

        Time.tick = 3
        k.processCooking() // slow dish finishes
        assertEquals(2, k.numberOfCookedMeals)
    }

    // --- getCooksSorted ---

    @Test
    fun `getCooksSorted - orders cooks by ascending id and puts unassigned ids last`() {
        val withoutId = rawCook(id = null)
        val idTwo = rawCook(id = 2)
        val idOne = rawCook(id = 1)
        val k = kitchen(listOf(withoutId, idTwo, idOne))

        val sorted = k.getCooksSorted()

        assertEquals(listOf(idOne, idTwo, withoutId), sorted)
    }

    // --- resetKitchen ---

    @Test
    fun `resetKitchen - aborts all queued dishes regardless of their current status`() {
        val recipe = recipe(duration = 10)
        val cookedDish = Dish(recipe).apply { status = DishStatus.COOKED }
        val cookingDish = Dish(recipe).apply { status = DishStatus.COOKING }
        val uncookedDish = Dish(recipe)
        val order = Order(listOf(cookedDish, cookingDish, uncookedDish))
        val k = kitchen(listOf(Cook(CookType.TOURNANT)), mutableListOf(order))

        k.resetKitchen()

        assertEquals(DishStatus.ABORTED, cookedDish.status)
        assertEquals(DishStatus.ABORTED, cookingDish.status)
        assertEquals(DishStatus.ABORTED, uncookedDish.status)
        assertEquals(0, k.getServableDishesNumber())
    }

    @Test
    fun `resetKitchen - clears cook state so cooks are unassigned for the next evening`() {
        val recipe = recipe(duration = 30)
        val dish = Dish(recipe)
        val order = Order(listOf(dish))
        val cook = Cook(CookType.TOURNANT)
        val k = kitchen(listOf(cook), mutableListOf(order))
        k.processCooking() // cook is now assigned, has an id, and is mid-recipe

        k.resetKitchen()

        assertEquals(null, cook.id)
        assertEquals(null, cook.orderId)
        assertEquals(null, cook.currentRecipe)
        assertFalse(cook.isCooking)
    }

    @Test
    fun `resetKitchen - next cook id assignment restarts from 1`() {
        val recipe = recipe(duration = 10)
        val order = Order(listOf(Dish(recipe)))
        val cook = Cook(CookType.TOURNANT)
        val k = kitchen(listOf(cook), mutableListOf(order))
        k.processCooking() // cook gets id 1

        k.resetKitchen()
        val nextId = k.getNextCookId()

        assertEquals(1, nextId)
    }

    @Test
    fun `resetKitchen - empty order queue - only resets cooks`() {
        val cook = Cook(CookType.TOURNANT)
        cook.setId(5)
        val k = kitchen(listOf(cook))

        k.resetKitchen()

        assertEquals(null, cook.id)
    }
}
