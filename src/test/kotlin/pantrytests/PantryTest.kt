package pantrytests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Pantry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Unit tests for [Pantry], covering F09 (buying ingredients) and P01 (best-before / reservations).
 */
class PantryTest {

    private val tomato = Ingredient("tomato", MeasurementUnit.G, bestBefore = 2, initialPackagingVolume = 100)
    private val chicken = Ingredient("chicken", MeasurementUnit.G, bestBefore = 3, initialPackagingVolume = 500)

    @BeforeEach
    fun setup() {
        Time.tick = 1
        Time.evening = 1
        Logger.setup(LogLevel.DEBUG)
    }

    private fun buildPantry(packages: List<IngredientPackage>, stockIngredients: List<Ingredient>): Pantry {
        val stock = Stock(stockIngredients)
        val supplier = Supplier(stock)
        return Pantry(inventory = packages.toMutableList(), supplier = supplier)
    }

    // --- F09: buying ingredients ---

    @Test
    fun `ensureQuantities - only buys the deficit when pantry already has some stock`() {
        // 60g tomato already in the pantry, recipe demand is 100g -> deficit of 40g
        // deficit should round up to one 100g package from the supplier
        val existing = IngredientPackage(tomato, currentAmount = 60, expiryDate = Int.MAX_VALUE, isOpen = true)
        val pantry = buildPantry(listOf(existing), listOf(tomato))

        pantry.ensureQuantities(mapOf(tomato to 100))

        val packagesAfter = pantry.getPackagesForIngredient(tomato)
        // the original package plus exactly one newly procured package
        assertEquals(2, packagesAfter.size)
        val totalAvailable = pantry.checkInventory(tomato, Int.MAX_VALUE / 2)
        assertEquals(60 + tomato.packagingVolume, totalAvailable)
    }

    @Test
    fun `ensureQuantities - does not procure anything when pantry already covers demand`() {
        val existing = IngredientPackage(tomato, currentAmount = 200, expiryDate = Int.MAX_VALUE, isOpen = false)
        val pantry = buildPantry(listOf(existing), listOf(tomato))

        pantry.ensureQuantities(mapOf(tomato to 100))

        // still only the single pre-existing package, nothing new procured
        assertEquals(1, pantry.getPackagesForIngredient(tomato).size)
    }

    @Test
    fun `ensureQuantities - ignores an unrelated ingredient's requirement`() {
        val pantry = buildPantry(emptyList(), listOf(tomato, chicken))

        pantry.ensureQuantities(mapOf(tomato to 50))

        assertTrue(pantry.getPackagesForIngredient(chicken).isEmpty())
    }

    @Test
    fun `ensureQuantities - procures nothing when stock exactly matches the requirement`() {
        // exact-equality boundary: 100g in stock, 100g required -> no deficit, no procurement
        val existing = IngredientPackage(tomato, currentAmount = 100, expiryDate = Int.MAX_VALUE, isOpen = false)
        val pantry = buildPantry(listOf(existing), listOf(tomato))

        pantry.ensureQuantities(mapOf(tomato to 100))

        assertEquals(1, pantry.getPackagesForIngredient(tomato).size)
    }

    @Test
    fun `ensureQuantities - a deficit spanning multiple packages procures exactly enough packages`() {
        // 250g required, nothing in stock, 100g per package -> 3 packages (300g), not 2 (200g)
        val pantry = buildPantry(emptyList(), listOf(tomato))

        pantry.ensureQuantities(mapOf(tomato to 250))

        val packages = pantry.getPackagesForIngredient(tomato)
        assertEquals(3, packages.size)
        assertEquals(300, packages.sumOf { it.currentAmount })
    }

    @Test
    fun `ensureQuantities - procures and logs multiple ingredients in ascending alphabetical order`() {
        val output = StringWriter()
        Logger.setup(PrintWriter(output))
        val pantry = buildPantry(emptyList(), listOf(tomato, chicken))

        pantry.ensureQuantities(mapOf(tomato to 50, chicken to 50))

        val logged = output.toString()
        val chickenIndex = logged.indexOf("chicken")
        val tomatoIndex = logged.indexOf("tomato")
        assertTrue(chickenIndex in 0 until tomatoIndex, "expected 'chicken' to be logged before 'tomato'")
    }

    @Test
    fun `checkInventory - sums amounts across multiple packages of the same ingredient`() {
        val first = IngredientPackage(tomato, currentAmount = 60, expiryDate = Int.MAX_VALUE, isOpen = true)
        val second = IngredientPackage(tomato, currentAmount = 50, expiryDate = Int.MAX_VALUE, isOpen = false)
        val pantry = buildPantry(listOf(first, second), listOf(tomato))

        // ask for far more than either single package holds, to force the additive sum
        val available = pantry.checkInventory(tomato, 1000)

        assertEquals(110, available)
    }

    // --- P01: expiry handling ---

    @Test
    fun `throwExpiredIngredients - removes only expired packages`() {
        Time.evening = 5
        val expired = IngredientPackage(tomato, currentAmount = 80, expiryDate = 5, isOpen = false)
        val valid = IngredientPackage(tomato, currentAmount = 40, expiryDate = 6, isOpen = false)
        val pantry = buildPantry(listOf(expired, valid), listOf(tomato))

        pantry.throwExpiredIngredients()

        val remaining = pantry.getPackagesForIngredient(tomato)
        assertEquals(1, remaining.size)
        assertEquals(40, remaining[0].currentAmount)
    }

    @Test
    fun `checkInventory - does not count expired packages toward availability`() {
        Time.evening = 5
        val expired = IngredientPackage(tomato, currentAmount = 200, expiryDate = 5, isOpen = false)
        val pantry = buildPantry(listOf(expired), listOf(tomato))

        val available = pantry.checkInventory(tomato, 100)

        assertEquals(0, available)
    }

    // --- P01: reservation priority (spec adjustment #2: open packages first, then earliest best-before) ---

    @Test
    fun `reserveIngredients - prefers an open package over a closed one that expires sooner`() {
        val openButLaterExpiry = IngredientPackage(tomato, currentAmount = 30, expiryDate = 10, isOpen = true)
        val closedButEarlierExpiry = IngredientPackage(tomato, currentAmount = 30, expiryDate = 2, isOpen = false)
        val pantry = buildPantry(listOf(closedButEarlierExpiry, openButLaterExpiry), listOf(tomato))

        pantry.reserveIngredients(mapOf(tomato to 20))

        // the open package must be drawn from first, regardless of its later expiry date
        assertEquals(10, openButLaterExpiry.currentAmount)
        assertEquals(30, closedButEarlierExpiry.currentAmount)
    }

    @Test
    fun `reserveIngredients - among equally open packages prefers the earliest best-before date`() {
        val expiresLater = IngredientPackage(tomato, currentAmount = 30, expiryDate = 10, isOpen = true)
        val expiresSooner = IngredientPackage(tomato, currentAmount = 30, expiryDate = 5, isOpen = true)
        val pantry = buildPantry(listOf(expiresLater, expiresSooner), listOf(tomato))

        pantry.reserveIngredients(mapOf(tomato to 20))

        assertEquals(10, expiresSooner.currentAmount)
        assertEquals(30, expiresLater.currentAmount)
    }

    @Test
    fun `reserveIngredients - splits removal across multiple packages once the first is exhausted`() {
        val first = IngredientPackage(tomato, currentAmount = 20, expiryDate = 5, isOpen = true)
        val second = IngredientPackage(tomato, currentAmount = 50, expiryDate = 10, isOpen = true)
        val pantry = buildPantry(listOf(first, second), listOf(tomato))

        pantry.reserveIngredients(mapOf(tomato to 30))

        assertEquals(0, first.currentAmount)
        assertEquals(40, second.currentAmount)
    }
}
