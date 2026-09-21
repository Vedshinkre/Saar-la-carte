package packagingchangeincidenttests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.PackagingChangeIncident
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.Pantry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * F33 together with what a new packaging volume changes: the supplier hands out packages of the new
 * size (F08), the pantry stores and logs what it bought (F09), and the countertop offers dishes
 * according to what is in the pantry (F13). Spec: ingredients are bought in packaging-volume
 * multiples, so the volume decides how much is bought, and a change only concerns future purchases.
 */
class PackagingChangeIntegrationTest {
    private lateinit var output: StringWriter
    private val flour = Ingredient("flour", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 100)
    private lateinit var stock: Stock
    private lateinit var pantry: Pantry

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.evening = 1
        stock = Stock(listOf(flour))
        pantry = Pantry(stock)
    }

    private fun packagingChange(volume: Int, evening: Int = 2) =
        PackagingChangeIncident(id = 1, evening = evening, ingredient = flour, packagingVolume = volume)

    private fun procuredLines() = output.toString().lines().filter { it.contains("Procured") }

    private fun stored() = pantry.checkInventory(flour, Int.MAX_VALUE)

    private fun packageSizes() = pantry.getPackagesForIngredient(flour).map { it.currentAmount }

    @Test
    fun `without an incident the supplier sells whole packages of the original size`() {
        pantry.ensureQuantities(mapOf(flour to 250))

        assertEquals(listOf(100, 100, 100), packageSizes())
        val logged = procuredLines().map { it.substringAfter("] ") }
        assertEquals(listOf("Pantry (R 1): Procured 300 g of flour from the supplier."), logged)
    }

    @Test
    fun `after a smaller volume the same demand is bought in more, smaller packages`() {
        packagingChange(40).apply()

        pantry.ensureQuantities(mapOf(flour to 250))

        assertEquals(List(7) { 40 }, packageSizes())
        assertEquals(280, stored())
        assertTrue(procuredLines().single().contains("Procured 280 g of flour"), procuredLines().toString())
    }

    @Test
    fun `after a bigger volume a small demand still buys one whole big package`() {
        packagingChange(1000).apply()

        pantry.ensureQuantities(mapOf(flour to 50))

        assertEquals(listOf(1000), packageSizes())
        assertTrue(procuredLines().single().contains("Procured 1000 g of flour"), procuredLines().toString())
    }

    @Test
    fun `a demand that exactly fills the new volume buys exactly one package`() {
        packagingChange(250).apply()

        pantry.ensureQuantities(mapOf(flour to 250))

        assertEquals(listOf(250), packageSizes())
    }

    @Test
    fun `packages bought before the incident keep their size and only the missing rest is bought new`() {
        pantry.ensureQuantities(mapOf(flour to 250)) // three packages of 100
        packagingChange(40).apply()

        pantry.ensureQuantities(mapOf(flour to 350)) // 50 g missing

        assertEquals(listOf(100, 100, 100, 40, 40), packageSizes())
        assertEquals(380, stored())
        assertTrue(procuredLines().last().contains("Procured 80 g of flour"), procuredLines().toString())
    }

    @Test
    fun `enough stock means the incident does not cause any new purchase`() {
        pantry.ensureQuantities(mapOf(flour to 250))
        packagingChange(40).apply()
        val linesBefore = procuredLines().size

        pantry.ensureQuantities(mapOf(flour to 300)) // 300 g are already there

        assertEquals(linesBefore, procuredLines().size)
        assertEquals(List(3) { 100 }, packageSizes())
    }

    @Test
    fun `an ingredient that is unavailable is not bought, and once available it is bought at the new size`() {
        stock.setIngredientToUnavailable(flour, 2)
        packagingChange(40).apply()

        pantry.ensureQuantities(mapOf(flour to 100))
        assertTrue(packageSizes().isEmpty(), "the supplier has none while the ingredient is unavailable")
        assertTrue(procuredLines().isEmpty())

        stock.setIngredientToAvailable(flour)
        pantry.ensureQuantities(mapOf(flour to 100))

        assertEquals(List(3) { 40 }, packageSizes())
        assertTrue(procuredLines().single().contains("Procured 120 g of flour"), procuredLines().toString())
    }

    @Test
    fun `two incidents in a row leave the last volume in force`() {
        packagingChange(40, evening = 2).apply()
        packagingChange(500, evening = 3).apply()

        pantry.ensureQuantities(mapOf(flour to 100))

        assertEquals(listOf(500), packageSizes())
    }

    @Test
    fun `the countertop offers a dish only when the packages bought at the new size cover it`() {
        val recipeFor280 = recipe(1, 280)
        val recipeFor290 = recipe(2, 290)
        packagingChange(40).apply()
        pantry.ensureQuantities(mapOf(flour to 250)) // buys 7 x 40 = 280 g
        val countertop = Countertop(
            pantry = pantry,
            orderQueue = ArrayDeque<Order>(),
            cooks = listOf(Cook(CookType.TOURNANT)),
            restaurantType = RestaurantType.EUROPEAN
        )

        val available = countertop.getAvailableRecipes(listOf(recipeFor280, recipeFor290))

        assertEquals(listOf(recipeFor280), available)
    }

    private fun recipe(id: Int, flourAmount: Int) = Recipe(
        id = id,
        name = "Bread$id",
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(flour to flourAmount),
        basicDishFor = null
    )
}
