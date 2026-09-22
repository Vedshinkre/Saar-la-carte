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
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.Kitchen
import de.unisaarland.cs.se.selab.restaurant.Pantry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Tests for F10 (Cooking - Order Queue)
 */
class OrderQueueLifecycleTest {

    private lateinit var output: StringWriter

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

    // --- fixtures ---

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
        pantry: Pantry = emptyPantry(),
        restaurantType: RestaurantType = RestaurantType.EUROPEAN
    ): Kitchen = Kitchen(cooks, pantry, orderQueue, restaurantType)

    // --- Cook selection: rank order and id tie-break (chooseCook), needed to reach specific batches ---

    @Test
    fun `chooseCook - among EXEC, SOUS and a specialist, the specialist (lowest authority) is chosen`() {
        val multiTypeRecipe = Recipe(
            id = 1,
            name = "Potato Soup",
            duration = 10,
            cookType = listOf(CookType.EXEC, CookType.SOUS, CookType.VEGETABLE),
            ingredients = mutableMapOf(),
            basicDishFor = null
        )
        val exec = Cook(CookType.EXEC)
        val sous = Cook(CookType.SOUS)
        val vegetable = Cook(CookType.VEGETABLE)
        val k = kitchen(listOf(exec, sous, vegetable))

        val chosen = k.chooseCook(multiTypeRecipe)

        assertEquals(vegetable, chosen)
    }

    @Test
    fun `chooseCook - two eligible cooks of the same type - the lower id wins the tie`() {
        val onionRings = recipe(id = 1, name = "Onion Rings", type = CookType.VEGETABLE)
        val higherId = Cook(
            id = 5,
            orderId = null,
            type = CookType.VEGETABLE,
            currentRecipe = null,
            remainingTicks = 0,
            isCooking = false
        )
        val lowerId = Cook(
            id = 2,
            orderId = null,
            type = CookType.VEGETABLE,
            currentRecipe = null,
            remainingTicks = 0,
            isCooking = false
        )
        val k = kitchen(listOf(higherId, lowerId))

        val chosen = k.chooseCook(onionRings)

        assertEquals(lowerId, chosen)
    }

    // --- Immediate ingredient reservation in the pantry when a dish is ordered (Countertop.reserveIngredients) ---

    @Test
    fun `Countertop reserveIngredients - immediately deducts the exact recipe amount from the pantry`() {
        val pasta = Ingredient("pasta", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)
        val pkg = IngredientPackage(pasta, currentAmount = 1000, expiryDate = Int.MAX_VALUE, isOpen = true)
        val pantry = Pantry(inventory = mutableListOf(pkg), supplier = Supplier(Stock(listOf(pasta))))
        val beefPastaRecipe = recipe(id = 1, name = "Beef Pasta", ingredients = mutableMapOf(pasta to 200))
        val countertop = Countertop(pantry, ArrayDeque(), listOf(Cook(CookType.TOURNANT)), RestaurantType.EUROPEAN)

        // two customers each ordering one meal of "Beef Pasta", reserved one dish at a time as FOH does
        countertop.reserveIngredients(beefPastaRecipe)
        countertop.reserveIngredients(beefPastaRecipe)

        assertEquals(600, pantry.checkInventory(pasta, 100000))
        assertEquals(600, pkg.currentAmount)
    }

    @Test
    fun `Countertop addOrder - queues the order into the kitchen's shared order queue immediately`() {
        val potatoSoup = recipe(id = 1, name = "Potato Soup", duration = 10, type = CookType.TOURNANT)
        val sharedQueue = ArrayDeque<Order>()
        val cook = Cook(CookType.TOURNANT)
        val pantry = emptyPantry()
        val countertop = Countertop(pantry, sharedQueue, listOf(cook), RestaurantType.EUROPEAN)
        val k = Kitchen(listOf(cook), pantry, sharedQueue, RestaurantType.EUROPEAN)
        val order = Order(listOf(Dish(potatoSoup)))

        countertop.addOrder(order)

        assertEquals(1, sharedQueue.size)
        assertTrue(sharedQueue.contains(order))
        k.processCooking() // proves the Kitchen sees the order immediately via the shared queue
        assertEquals(DishStatus.COOKED, order.dishes.single().status)
    }

    // --- Order cancellation & ingredient disposal (resetKitchen) ---

    @Test
    fun `resetKitchen - aborts the order but never returns its reserved ingredients to the pantry`() {
        val potato = Ingredient("potato", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 300)
        val pkg = IngredientPackage(potato, currentAmount = 300, expiryDate = Int.MAX_VALUE, isOpen = false)
        val pantry = Pantry(inventory = mutableListOf(pkg), supplier = Supplier(Stock(listOf(potato))))
        val potatoSoup = recipe(id = 1, name = "Potato Soup", ingredients = mutableMapOf(potato to 300))
        val countertop = Countertop(pantry, ArrayDeque(), listOf(Cook(CookType.TOURNANT)), RestaurantType.EUROPEAN)
        countertop.reserveIngredients(potatoSoup) // reserved immediately, as at order placement
        assertEquals(0, pantry.checkInventory(potato, 100000))

        val dish = Dish(potatoSoup)
        val order = Order(listOf(dish))
        val k = kitchen(listOf(Cook(CookType.TOURNANT)), mutableListOf(order), pantry = pantry)

        k.resetKitchen() // customer left / evening ended before the meal was served

        assertEquals(
            DishStatus.ABORTED,
            dish.status
        ) // per the spec adjustment, reserved/unfinished ingredients are eaten/discarded by staff,
        // never returned to the pantry
        assertEquals(0, pantry.checkInventory(potato, 100000))
    }

    // --- Order id assignment: globally unique, incrementing from 1 ---

    @Test
    fun `Order id - assigned globally, unique and incrementing from 1`() {
        val potatoSoup = recipe(id = 1, name = "Potato Soup")
        val first = Order(listOf(Dish(potatoSoup)))
        val second = Order(listOf(Dish(potatoSoup)))
        val third = Order(listOf(Dish(potatoSoup)))

        assertEquals(1, first.id)
        assertEquals(2, second.id)
        assertEquals(3, third.id)
    }
}
