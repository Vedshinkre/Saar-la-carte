package cookingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Tests the Kitchen's order queue: dish-selection prioritization within an order
 * (basic dish first, then ascending recipe id) and the batch-cooking/scaling effect across queued orders.
 */
class OrderQueueTest {

    private lateinit var output: StringWriter

    private fun logContains(expected: String): Boolean = output.toString().contains(expected)

    @BeforeEach
    fun setUp() {
        Time.tick = 1
        Time.evening = 1
        Order.resetIds()
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
    }

    private fun recipe(
        id: Int,
        name: String,
        duration: Int = 10,
        type: CookType = CookType.TOURNANT,
        basicFor: RestaurantType? = null,
        ingredients: MutableMap<Ingredient, Int> = mutableMapOf()
    ): Recipe = Recipe(
        id = id,
        name = name,
        duration = duration,
        cookType = listOf(type),
        ingredients = ingredients,
        basicDishFor = basicFor
    )

    private fun emptyPantry(): Pantry = Pantry(Stock(emptyList()))

    private fun kitchen(
        cooks: List<Cook>,
        orderQueue: MutableList<Order> = mutableListOf(),
        restaurantType: RestaurantType = RestaurantType.EUROPEAN
    ): Kitchen = Kitchen(cooks, emptyPantry(), orderQueue, restaurantType)

    @Test
    fun `sortRecipesByBasicnessAndId - basic dish takes precedence over non-basic dishes regardless of id`() {
        val chickenRice = recipe(id = 1, name = "Chicken Rice")
        val beefPasta = recipe(id = 2, name = "Beef Pasta")
        val potatoSoup = recipe(id = 3, name = "Potato Soup", basicFor = RestaurantType.EUROPEAN)

        val sorted = kitchen(listOf(Cook(CookType.TOURNANT)), restaurantType = RestaurantType.EUROPEAN)
            .sortRecipesByBasicnessAndId(listOf(beefPasta, chickenRice, potatoSoup))

        assertEquals(listOf(potatoSoup, chickenRice, beefPasta), sorted)
    }

    @Test
    fun `sortRecipesByBasicnessAndId - ties among non-basic recipes are broken by ascending recipe id`() {
        val high = recipe(id = 8, name = "Cheesecake")
        val low = recipe(id = 2, name = "Onion Rings")
        val mid = recipe(id = 5, name = "Fries")

        val sorted = kitchen(listOf(Cook(CookType.TOURNANT))).sortRecipesByBasicnessAndId(listOf(high, low, mid))

        assertEquals(listOf(low, mid, high), sorted)
    }

    @Test
    fun `sortRecipesByBasicnessAndId - ties among basic recipes are also broken by ascending recipe id`() {
        val basicHigh = recipe(id = 7, name = "Goulash", basicFor = RestaurantType.EUROPEAN)
        val basicLow = recipe(id = 3, name = "Potato Soup", basicFor = RestaurantType.EUROPEAN)

        val sorted = kitchen(listOf(Cook(CookType.TOURNANT)), restaurantType = RestaurantType.EUROPEAN)
            .sortRecipesByBasicnessAndId(listOf(basicHigh, basicLow))

        assertEquals(listOf(basicLow, basicHigh), sorted)
    }

    @Test
    fun `sortRecipesByBasicnessAndId - a recipe basic for a different restaurant type counts as non-basic here`() {
        val basicElsewhere = recipe(id = 9, name = "Sushi", basicFor = RestaurantType.ASIAN)
        val ordinary = recipe(id = 1, name = "Onion Rings")

        val sorted = kitchen(listOf(Cook(CookType.TOURNANT)), restaurantType = RestaurantType.EUROPEAN)
            .sortRecipesByBasicnessAndId(listOf(basicElsewhere, ordinary))

        assertEquals(listOf(ordinary, basicElsewhere), sorted)
    }

    @Test
    fun `processCooking - one cook for three dishes - the basic dish starts first, others wait`() {
        val chickenRice = recipe(id = 1, name = "Chicken Rice", duration = 30)
        val beefPasta = recipe(id = 2, name = "Beef Pasta", duration = 30)
        val potatoSoup = recipe(id = 3, name = "Potato Soup", duration = 30, basicFor = RestaurantType.EUROPEAN)
        val order = Order(listOf(Dish(beefPasta), Dish(chickenRice), Dish(potatoSoup)))
        val cook = Cook(CookType.TOURNANT)

        kitchen(listOf(cook), mutableListOf(order)).processCooking()

        assertEquals(DishStatus.COOKING, order.dishes.single { it.recipe.id == 3 }.status)
        assertEquals(DishStatus.UNCOOKED, order.dishes.single { it.recipe.id == 1 }.status)
        assertEquals(DishStatus.UNCOOKED, order.dishes.single { it.recipe.id == 2 }.status)
        assertEquals("Potato Soup", cook.currentRecipe?.name)
        val expected =
            "Kitchen Dish Assignment (R 1): Cook 1 of type TOURNANT starts cooking 1 meals " +
                "of dish Potato Soup based on order ${order.id} for orders ${order.id}."
        assertTrue(logContains(expected), "expected the basic dish to be picked first, got:\n$output")
    }

    @Test
    fun `processCooking - one cook for two non-basic dishes - the lower recipe id starts first`() {
        val chickenRice = recipe(id = 1, name = "Chicken Rice", duration = 30)
        val beefPasta = recipe(id = 2, name = "Beef Pasta", duration = 30)
        val order = Order(listOf(Dish(beefPasta), Dish(chickenRice)))
        val cook = Cook(CookType.TOURNANT)

        kitchen(listOf(cook), mutableListOf(order)).processCooking()

        assertEquals(DishStatus.COOKING, order.dishes.single { it.recipe.id == 1 }.status)
        assertEquals(DishStatus.UNCOOKED, order.dishes.single { it.recipe.id == 2 }.status)
        assertEquals("Chicken Rice", cook.currentRecipe?.name)
    }

    @Test
    fun `processCooking - scales a single cook assignment across all currently queued orders of the same dish`() {
        val chickenRice = recipe(id = 1, name = "Chicken Rice", duration = 30)
        val order1 = Order(listOf(Dish(chickenRice)))
        val order2 = Order(listOf(Dish(chickenRice), Dish(chickenRice)))
        val order3 = Order(listOf(Dish(chickenRice)))
        val cook = Cook(CookType.TOURNANT)

        kitchen(listOf(cook), mutableListOf(order1, order2, order3)).processCooking()

        val allChickenRiceDishes = order1.dishes + order2.dishes + order3.dishes
        assertTrue(allChickenRiceDishes.all { it.status == DishStatus.COOKING })
        assertEquals(mapOf(order1.id to 1, order2.id to 2, order3.id to 1), cook.getAssignedCountsByOrder())
        val expected =
            "Kitchen Dish Assignment (R 1): Cook 1 of type TOURNANT starts cooking 4 meals " +
                "of dish Chicken Rice based on order ${order1.id} for orders " +
                "${order1.id},${order2.id},${order3.id}."
        assertTrue(logContains(expected), "expected the batched assignment log, got:\n$output")
    }

    @Test
    fun `processCooking - a second dish type in the queue gets its own separate assignment, not merged in`() {
        val chickenRice = recipe(id = 1, name = "Chicken Rice", duration = 30, type = CookType.TOURNANT)
        val beefPasta = recipe(id = 2, name = "Beef Pasta", duration = 10, type = CookType.SAUCE)
        val order1 = Order(listOf(Dish(chickenRice)))
        val order2 = Order(listOf(Dish(chickenRice), Dish(beefPasta)))
        val cooks = listOf(Cook(CookType.TOURNANT), Cook(CookType.SAUCE))

        kitchen(cooks, mutableListOf(order1, order2)).processCooking()

        val expectedChickenRice =
            "Kitchen Dish Assignment (R 1): Cook 1 of type TOURNANT starts cooking 2 meals " +
                "of dish Chicken Rice based on order ${order1.id} for orders ${order1.id},${order2.id}."
        val expectedBeefPasta =
            "Kitchen Dish Assignment (R 1): Cook 2 of type SAUCE starts cooking 1 meals " +
                "of dish Beef Pasta based on order ${order2.id} for orders ${order2.id}."
        assertTrue(logContains(expectedChickenRice), output.toString())
        assertTrue(logContains(expectedBeefPasta), output.toString())
    }

    @Test
    fun `processCooking - an order placed after a cook already started its batch is not folded in`() {
        val recipe = recipe(id = 1, name = "Potato Soup", duration = 30)
        val firstOrder = Order(listOf(Dish(recipe)))
        val cook = Cook(CookType.TOURNANT)
        val queue = mutableListOf(firstOrder)
        val kitchen = kitchen(listOf(cook), queue)
        kitchen.processCooking()

        Time.tick = 2
        val lateOrder = Order(listOf(Dish(recipe)))
        queue.add(lateOrder)
        kitchen.processCooking()

        assertEquals(DishStatus.UNCOOKED, lateOrder.dishes.single().status)
    }
}
