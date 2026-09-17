package kitchentests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Kitchen
import de.unisaarland.cs.se.selab.restaurant.Pantry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for [Kitchen.processCooking]: the full per-tick pipeline of
 * cleaning the order queue, assigning uncooked recipes to eligible cooks, and
 * advancing cooks. These drive the whole pipeline through the order queue rather
 * than calling the private assignment helpers directly.
 */
class KitchenAssignmentIntegrationTest {

    @BeforeEach
    fun setup() {
        Time.tick = 1
        Logger.setup(LogLevel.DEBUG)
    }

    private fun kitchen(
        cooks: List<Cook>,
        orderQueue: MutableList<Order>,
        restaurantType: RestaurantType = RestaurantType.ASIAN
    ): Kitchen = Kitchen(cooks, Pantry(Stock(emptyList())), orderQueue, restaurantType)

    private fun recipe(
        id: Int,
        cookTypes: List<CookType> = listOf(CookType.TOURNANT),
        basicDishFor: RestaurantType? = null,
        duration: Int = 10
    ): Recipe = Recipe(id, "Recipe $id", duration, cookTypes, mutableMapOf(), basicDishFor)

    @Test
    fun `an order later in the queue waits for a cook that a prior order already took, then is retried next tick`() {
        val cookType = CookType.TOURNANT
        val cook = Cook(cookType)

        val recipeA = recipe(id = 1, cookTypes = listOf(cookType))
        val recipeB = recipe(id = 2, cookTypes = listOf(cookType))
        val dishA = Dish(recipeA)
        val dishB = Dish(recipeB)
        val orderOne = Order(listOf(dishA))
        val orderTwo = Order(listOf(dishB))

        val kitchen = kitchen(listOf(cook), mutableListOf(orderOne, orderTwo))

        kitchen.processCooking()
        assertEquals(DishStatus.COOKED, dishA.status, "the first order's recipe should take the only free cook")
        assertEquals(DishStatus.UNCOOKED, dishB.status, "the second order's recipe has no free cook left this tick")

        kitchen.processCooking()
        assertEquals(DishStatus.COOKED, dishB.status, "on the next tick the now-free cook picks up the retried recipe")
    }

    @Test
    fun `within one order a basic dish for the restaurant type is cooked before a non basic dish`() {
        val cookType = CookType.SOUS
        val cook = Cook(cookType)

        val basicRecipe = recipe(id = 9, cookTypes = listOf(cookType), basicDishFor = RestaurantType.ASIAN)
        val nonBasicRecipe = recipe(id = 1, cookTypes = listOf(cookType))
        val basicDish = Dish(basicRecipe)
        val nonBasicDish = Dish(nonBasicRecipe)
        val order = Order(listOf(nonBasicDish, basicDish))

        val kitchen = kitchen(listOf(cook), mutableListOf(order), restaurantType = RestaurantType.ASIAN)

        kitchen.processCooking()

        assertEquals(DishStatus.COOKED, basicDish.status, "the basic dish has priority despite its higher recipe id")
        assertEquals(DishStatus.UNCOOKED, nonBasicDish.status)
    }

    @Test
    fun `dishes of the same recipe from two different orders are batched onto a single cook`() {
        val cookType = CookType.ROAST
        val cook = Cook(cookType)
        val sharedRecipe = recipe(id = 1, cookTypes = listOf(cookType), duration = 20)

        val dishFromOrderOne = Dish(sharedRecipe)
        val dishFromOrderTwo = Dish(sharedRecipe)
        val orderOne = Order(listOf(dishFromOrderOne))
        val orderTwo = Order(listOf(dishFromOrderTwo))

        val kitchen = kitchen(listOf(cook), mutableListOf(orderOne, orderTwo))

        kitchen.processCooking()

        assertEquals(2, cook.getDishes().size)
        assertTrue(cook.getDishes().containsAll(listOf(dishFromOrderOne, dishFromOrderTwo)))
        assertEquals(orderOne.id, cook.orderId, "the first order encountered becomes the base order")
        assertEquals(DishStatus.COOKING, dishFromOrderOne.status)
        assertEquals(DishStatus.COOKING, dishFromOrderTwo.status)
    }

    @Test
    fun `a cook stays busy for the recipe's full duration and only frees up once it is done`() {
        val cook = Cook(CookType.FISH)
        val slowRecipe = recipe(id = 1, cookTypes = listOf(CookType.FISH), duration = 20)
        val dish = Dish(slowRecipe)
        val order = Order(listOf(dish))

        val kitchen = kitchen(listOf(cook), mutableListOf(order))

        kitchen.processCooking()
        assertTrue(cook.isCooking, "one tick is not enough to finish a 20 minute recipe")
        assertEquals(DishStatus.COOKING, dish.status)

        kitchen.processCooking()
        assertFalse(cook.isCooking)
        assertEquals(DishStatus.COOKED, dish.status)
    }

    @Test
    fun `no eligible cook leaves the dish uncooked without throwing`() {
        val mismatchedCook = Cook(CookType.PASTRY)
        val recipeNeedingSous = recipe(id = 1, cookTypes = listOf(CookType.SOUS))
        val dish = Dish(recipeNeedingSous)
        val order = Order(listOf(dish))

        val kitchen = kitchen(listOf(mismatchedCook), mutableListOf(order))

        kitchen.processCooking()

        assertEquals(DishStatus.UNCOOKED, dish.status)
        assertFalse(mismatchedCook.isCooking)
    }

    @Test
    fun `two different recipes competing for the same cook pool split across ticks in queue order`() {
        val cookType = CookType.VEGETABLE
        val onlyCook = Cook(cookType)

        val recipeX = recipe(id = 3, cookTypes = listOf(cookType))
        val recipeY = recipe(id = 4, cookTypes = listOf(cookType))
        val dishX = Dish(recipeX)
        val dishY = Dish(recipeY)
        val firstOrder = Order(listOf(dishX))
        val secondOrder = Order(listOf(dishY))

        val kitchen = kitchen(listOf(onlyCook), mutableListOf(firstOrder, secondOrder))

        kitchen.processCooking()

        assertEquals(DishStatus.COOKED, dishX.status)
        assertEquals(DishStatus.UNCOOKED, dishY.status)
        assertEquals(0, onlyCook.getDishes().size, "the cook finished and cleared its dishes within the same tick")
    }
}
