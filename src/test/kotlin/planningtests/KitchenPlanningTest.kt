package planningtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
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
import de.unisaarland.cs.se.selab.restaurant.Kitchen
import de.unisaarland.cs.se.selab.restaurant.Pantry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Planning of the ingredients in the preparation phase (F09): the shopping list is built from the orders of the
 * known REGULAR groups, the favourite dishes of the EVENT groups and an estimate of ⌈otherSeats / 10⌉ orders per
 * menu dish, and the missing ingredients are bought in whole packages.
 */
class KitchenPlanningTest {
    private val rice = Ingredient("rice", MeasurementUnit.G, bestBefore = 3, initialPackagingVolume = 1000)
    private val beef = Ingredient("beef", MeasurementUnit.G, bestBefore = 3, initialPackagingVolume = 100)

    private val riceBowl = recipe(1, "Rice Bowl", rice to 200)
    private val beefRice = recipe(2, "Beef Rice", rice to 50, beef to 150)

    private lateinit var output: StringWriter

    private fun recipe(id: Int, name: String, vararg amounts: Pair<Ingredient, Int>) =
        Recipe(id, name, 10, listOf(CookType.TOURNANT), mutableMapOf(*amounts), null)

    private fun pantry(vararg packages: IngredientPackage) = Pantry(
        inventory = packages.toMutableList(),
        supplier = Supplier(Stock(listOf(rice, beef)))
    )

    private fun kitchen(pantry: Pantry) =
        Kitchen(listOf(Cook(CookType.TOURNANT)), pantry, mutableListOf(), RestaurantType.ASIAN)

    private fun order(vararg recipes: Recipe) = Order(recipes.map { Dish(it) })

    private fun amountInPantry(pantry: Pantry, ingredient: Ingredient) =
        pantry.getPackagesForIngredient(ingredient).sumOf { it.currentAmount }

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.evening = 1
        Time.tick = 1
    }

    // ---- the shopping list

    @Test
    fun `the shopping list sums an ingredient over all recipes of the order history`() {
        val list = kitchen(pantry()).createShoppingList(mapOf(riceBowl to 2, beefRice to 1), 0, emptyList())

        assertEquals(mapOf(rice to 450, beef to 150), list)
    }

    @Test
    fun `no other seats and no history give an empty shopping list`() {
        assertEquals(emptyMap(), kitchen(pantry()).createShoppingList(emptyMap(), 0, listOf(riceBowl)))
    }

    @Test
    fun `every menu dish is planned once for up to ten other seats`() {
        val menu = listOf(riceBowl, beefRice)

        for (seats in listOf(1, 9, 10)) {
            val list = kitchen(pantry()).createShoppingList(emptyMap(), seats, menu)
            assertEquals(mapOf(rice to 250, beef to 150), list, "$seats seats")
        }
    }

    @Test
    fun `the estimate per menu dish is rounded up to the next full ten seats`() {
        val menu = listOf(riceBowl, beefRice)

        assertEquals(mapOf(rice to 500, beef to 300), kitchen(pantry()).createShoppingList(emptyMap(), 11, menu))
        assertEquals(mapOf(rice to 500, beef to 300), kitchen(pantry()).createShoppingList(emptyMap(), 20, menu))
        assertEquals(mapOf(rice to 750, beef to 450), kitchen(pantry()).createShoppingList(emptyMap(), 21, menu))
    }

    @Test
    fun `the estimate is added on top of the order history`() {
        val list = kitchen(pantry()).createShoppingList(mapOf(riceBowl to 3), 10, listOf(riceBowl))

        assertEquals(mapOf(rice to 800), list)
    }

    // ---- procuring

    @Test
    fun `planning buys the ingredients of the known orders in whole packages`() {
        val pantry = pantry()

        kitchen(pantry).planForIngredients(listOf(order(riceBowl, riceBowl, riceBowl)), 0, emptyList(), emptyList())

        assertEquals(1000, amountInPantry(pantry, rice))
        assertEquals(0, amountInPantry(pantry, beef))
    }

    @Test
    fun `planning buys as many packages as the demand needs`() {
        val pantry = pantry()

        // 6 x 200 rice + 3 x 50 rice = 1350, so two packages of 1000; 3 x 150 beef = 450, so five packages of 100
        kitchen(pantry).planForIngredients(
            listOf(order(riceBowl, riceBowl, riceBowl), order(riceBowl, riceBowl, riceBowl)),
            0,
            emptyList(),
            listOf(beefRice to 3)
        )

        assertEquals(2000, amountInPantry(pantry, rice))
        assertEquals(500, amountInPantry(pantry, beef))
    }

    @Test
    fun `the orders of several regular groups are planned together`() {
        val pantry = pantry()

        kitchen(pantry).planForIngredients(
            listOf(order(riceBowl), order(beefRice, riceBowl)),
            0,
            emptyList(),
            emptyList()
        )

        // 3 x 200 rice + 50, beef 150
        assertEquals(1000, amountInPantry(pantry, rice))
        assertEquals(200, amountInPantry(pantry, beef))
    }

    @Test
    fun `an event plans its favourite dish for every customer of the group`() {
        val pantry = pantry()

        kitchen(pantry).planForIngredients(emptyList(), 0, emptyList(), listOf(riceBowl to 12))

        // 12 x 200 rice = 2400, so three packages
        assertEquals(3000, amountInPantry(pantry, rice))
    }

    @Test
    fun `the favourite dish of an event adds to the orders of regulars for the same dish`() {
        val pantry = pantry()

        kitchen(pantry).planForIngredients(listOf(order(riceBowl)), 0, emptyList(), listOf(riceBowl to 4))

        // 5 x 200 rice = 1000, exactly one package
        assertEquals(1000, amountInPantry(pantry, rice))
    }

    @Test
    fun `stock that is still in the pantry only reduces the deficit that is bought`() {
        val pantry = pantry(IngredientPackage(rice, 300, expiryDate = 5, isOpen = true))

        kitchen(pantry).planForIngredients(listOf(order(riceBowl, riceBowl)), 0, emptyList(), emptyList())

        // 400 needed, 300 in stock: one package of 1000 for the missing 100
        assertEquals(1300, amountInPantry(pantry, rice))
    }

    @Test
    fun `stock that covers the demand is not topped up`() {
        val pantry = pantry(IngredientPackage(rice, 300, expiryDate = 5, isOpen = true))

        kitchen(pantry).planForIngredients(listOf(order(riceBowl)), 0, emptyList(), emptyList())

        assertEquals(300, amountInPantry(pantry, rice))
        assertEquals(emptyList(), output.toString().lines().filter { it.contains("Procured") })
    }

    @Test
    fun `expired packages are thrown away before they count towards the demand`() {
        Time.evening = 4
        val pantry = pantry(IngredientPackage(rice, 900, expiryDate = 4, isOpen = true))

        kitchen(pantry).planForIngredients(listOf(order(riceBowl)), 0, emptyList(), emptyList())

        assertEquals(1000, amountInPantry(pantry, rice), "the expired 900 g are gone and a fresh package was bought")
        assertTrue(output.toString().lines().any { it.contains("Pantry (R 1): Removed 900") && it.contains("of rice") })
    }

    @Test
    fun `dishes for the estimate are planned even when no cook could cook them`() {
        val fish = recipe(3, "Fish", rice to 10).let {
            Recipe(3, "Fish", 10, listOf(CookType.FISH), it.ingredients, null)
        }
        val pantry = pantry()

        kitchen(pantry).planForIngredients(emptyList(), 10, listOf(fish), emptyList())

        assertEquals(1000, amountInPantry(pantry, rice))
    }
}
