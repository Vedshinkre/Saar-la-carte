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
    fun `only buys the deficit when pantry already has some stock`() {
        val existing = IngredientPackage(tomato, currentAmount = 60, expiryDate = Int.MAX_VALUE, isOpen = true)
        val pantry = buildPantry(listOf(existing), listOf(tomato))

        pantry.ensureQuantities(mapOf(tomato to 100))

        val packagesAfter =
            pantry.getPackagesForIngredient(tomato) // the original package plus exactly one newly procured package
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
        val existing = IngredientPackage(tomato, currentAmount = 100, expiryDate = Int.MAX_VALUE, isOpen = false)
        val pantry = buildPantry(listOf(existing), listOf(tomato))

        pantry.ensureQuantities(mapOf(tomato to 100))

        assertEquals(1, pantry.getPackagesForIngredient(tomato).size)
    }

    @Test
    fun `a deficit spanning multiple packages procures exactly enough packages`() {
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

    @Test
    fun `reserveIngredients - among closed packages prefers the earliest best-before date`() {
        val expiresLater = IngredientPackage(tomato, currentAmount = 30, expiryDate = 10, isOpen = false)
        val expiresSooner = IngredientPackage(tomato, currentAmount = 30, expiryDate = 5, isOpen = false)
        val pantry = buildPantry(listOf(expiresLater, expiresSooner), listOf(tomato))

        pantry.reserveIngredients(mapOf(tomato to 20))

        assertEquals(10, expiresSooner.currentAmount)
        assertEquals(30, expiresLater.currentAmount)
    }

    // --- P01: throwExpiredIngredients ---

    private fun captureLog(): StringWriter {
        val output = StringWriter()
        Logger.setup(PrintWriter(output))
        return output
    }

    private fun removedLines(log: StringWriter): List<String> = log.toString().lines().filter { it.contains("Removed") }

    @Test
    fun `throwExpiredIngredients - empty pantry does nothing and logs nothing`() {
        val log = captureLog()
        val pantry = buildPantry(emptyList(), listOf(tomato))

        pantry.throwExpiredIngredients()

        assertTrue(pantry.getPackagesForIngredient(tomato).isEmpty())
        assertTrue(removedLines(log).isEmpty())
    }

    @Test
    fun `throwExpiredIngredients - keeps everything and logs nothing when no package has expired`() {
        Time.evening = 4
        val log = captureLog()
        val first = IngredientPackage(tomato, currentAmount = 100, expiryDate = 5, isOpen = false)
        val second = IngredientPackage(chicken, currentAmount = 250, expiryDate = 9, isOpen = true)
        val pantry = buildPantry(listOf(first, second), listOf(tomato, chicken))

        pantry.throwExpiredIngredients()

        assertEquals(100, pantry.checkInventory(tomato, 1000))
        assertEquals(250, pantry.checkInventory(chicken, 1000))
        assertTrue(removedLines(log).isEmpty())
    }

    @Test
    fun `throwExpiredIngredients - removes every package when all of them have expired`() {
        Time.evening = 8
        val pantry = buildPantry(
            listOf(
                IngredientPackage(tomato, currentAmount = 100, expiryDate = 8, isOpen = false),
                IngredientPackage(chicken, currentAmount = 500, expiryDate = 3, isOpen = true)
            ),
            listOf(tomato, chicken)
        )

        pantry.throwExpiredIngredients()

        assertTrue(pantry.getPackagesForIngredient(tomato).isEmpty())
        assertTrue(pantry.getPackagesForIngredient(chicken).isEmpty())
    }

    @Test
    fun `throwExpiredIngredients - a package whose expiry date is already in the past is removed`() {
        Time.evening = 10
        val longGone = IngredientPackage(tomato, currentAmount = 70, expiryDate = 4, isOpen = false)
        val pantry = buildPantry(listOf(longGone), listOf(tomato))

        pantry.throwExpiredIngredients()

        assertEquals(0, pantry.checkInventory(tomato, 1000))
    }

    @Test
    fun `throwExpiredIngredients - a package expiring one evening later is kept`() {
        Time.evening = 5
        val expiresTomorrow = IngredientPackage(tomato, currentAmount = 70, expiryDate = 6, isOpen = false)
        val pantry = buildPantry(listOf(expiresTomorrow), listOf(tomato))

        pantry.throwExpiredIngredients()

        assertEquals(70, pantry.checkInventory(tomato, 1000))
    }

    @Test
    fun `throwExpiredIngredients - sums several expired packages of one ingredient into a single log line`() {
        Time.evening = 6
        val log = captureLog()
        val pantry = buildPantry(
            listOf(
                IngredientPackage(tomato, currentAmount = 100, expiryDate = 6, isOpen = false),
                IngredientPackage(tomato, currentAmount = 30, expiryDate = 5, isOpen = true),
                IngredientPackage(tomato, currentAmount = 100, expiryDate = 9, isOpen = false)
            ),
            listOf(tomato)
        )

        pantry.throwExpiredIngredients()

        val lines = removedLines(log)
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("Removed 130 g of tomato from the pantry."), lines[0])
        assertEquals(100, pantry.checkInventory(tomato, 1000))
    }

    @Test
    fun `throwExpiredIngredients - logs the removed amount unit and name`() {
        Time.evening = 6
        val log = captureLog()
        val milk = Ingredient("milk", MeasurementUnit.ML, bestBefore = 2, initialPackagingVolume = 1000)
        val pantry = buildPantry(
            listOf(IngredientPackage(milk, currentAmount = 350, expiryDate = 6, isOpen = true)),
            listOf(milk)
        )

        pantry.throwExpiredIngredients()

        val lines = removedLines(log)
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("Removed 350 mL of milk from the pantry."), lines[0])
    }

    @Test
    fun `throwExpiredIngredients - only the expired ingredient is logged`() {
        Time.evening = 6
        val log = captureLog()
        val pantry = buildPantry(
            listOf(
                IngredientPackage(chicken, currentAmount = 200, expiryDate = 6, isOpen = false),
                IngredientPackage(tomato, currentAmount = 100, expiryDate = 7, isOpen = false)
            ),
            listOf(tomato, chicken)
        )

        pantry.throwExpiredIngredients()

        val lines = removedLines(log)
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("chicken"), lines[0])
        assertEquals(100, pantry.checkInventory(tomato, 1000))
    }

    @Test
    fun `throwExpiredIngredients - logs removed ingredients alphabetically regardless of pantry order`() {
        Time.evening = 6
        val log = captureLog()
        val beef = Ingredient("beef", MeasurementUnit.G, bestBefore = 2, initialPackagingVolume = 300)
        val pantry = buildPantry(
            listOf(
                IngredientPackage(tomato, currentAmount = 10, expiryDate = 6, isOpen = false),
                IngredientPackage(chicken, currentAmount = 20, expiryDate = 6, isOpen = false),
                IngredientPackage(beef, currentAmount = 30, expiryDate = 6, isOpen = false)
            ),
            listOf(tomato, chicken, beef)
        )

        pantry.throwExpiredIngredients()

        val lines = removedLines(log)
        assertEquals(3, lines.size)
        assertTrue(lines[0].contains("beef"), lines[0])
        assertTrue(lines[1].contains("chicken"), lines[1])
        assertTrue(lines[2].contains("tomato"), lines[2])
    }

    @Test
    fun `throwExpiredIngredients - an expired package with nothing left is removed without a log line`() {
        Time.evening = 6
        val log = captureLog()
        val emptied = IngredientPackage(tomato, currentAmount = 50, expiryDate = 6, isOpen = false)
        emptied.removeAmount(50)
        val pantry = buildPantry(listOf(emptied), listOf(tomato))

        pantry.throwExpiredIngredients()

        assertTrue(removedLines(log).isEmpty())
        assertEquals(0, pantry.checkInventory(tomato, 1000))
    }

    @Test
    fun `throwExpiredIngredients - is idempotent and logs nothing on a second call`() {
        Time.evening = 6
        val log = captureLog()
        val pantry = buildPantry(
            listOf(
                IngredientPackage(tomato, currentAmount = 100, expiryDate = 6, isOpen = false),
                IngredientPackage(tomato, currentAmount = 40, expiryDate = 8, isOpen = false)
            ),
            listOf(tomato)
        )

        pantry.throwExpiredIngredients()
        pantry.throwExpiredIngredients()

        assertEquals(1, removedLines(log).size)
        assertEquals(40, pantry.checkInventory(tomato, 1000))
    }

    @Test
    fun `surviving packages keep their amount and open state`() {
        Time.evening = 6
        val survivor = IngredientPackage(tomato, currentAmount = 45, expiryDate = 9, isOpen = true)
        val doomed = IngredientPackage(tomato, currentAmount = 100, expiryDate = 6, isOpen = false)
        val pantry = buildPantry(listOf(doomed, survivor), listOf(tomato))

        pantry.throwExpiredIngredients()

        val remaining = pantry.getPackagesForIngredient(tomato)
        assertEquals(1, remaining.size)
        assertTrue(remaining[0] === survivor)
        assertEquals(45, survivor.currentAmount)
        assertTrue(survivor.isOpen)
    }

    @Test
    fun ` a partially used package is discarded with its whole remaining amount`() {
        Time.evening = 6
        val log = captureLog()
        val partial = IngredientPackage(tomato, currentAmount = 300, expiryDate = 6, isOpen = true)
        val pantry = buildPantry(listOf(partial), listOf(tomato))

        pantry.throwExpiredIngredients()

        assertTrue(removedLines(log).single().contains("Removed 300 g of tomato"))
        assertEquals(0, pantry.checkInventory(tomato, 1000))
    }

    @Test
    fun `package from the convenience constructor lives through E plus N minus 1 only`() {
        Time.evening = 1
        val pantry = buildPantry(listOf(IngredientPackage(chicken)), listOf(chicken))

        Time.evening = 3
        pantry.throwExpiredIngredients()
        assertEquals(500, pantry.checkInventory(chicken, 1000))

        Time.evening = 4
        pantry.throwExpiredIngredients()
        assertEquals(0, pantry.checkInventory(chicken, 1000))
    }

    @Test
    fun `unexpired stock carries over across several evenings while older stock is dropped`() {
        Time.evening = 1
        val old = IngredientPackage(tomato) // bestBefore 2 -> expires at evening 3
        Time.evening = 2
        val fresh = IngredientPackage(tomato) // expires at evening 4
        val pantry = buildPantry(listOf(old, fresh), listOf(tomato))

        Time.evening = 2
        pantry.throwExpiredIngredients()
        assertEquals(200, pantry.checkInventory(tomato, 1000))

        Time.evening = 3
        pantry.throwExpiredIngredients()
        assertEquals(100, pantry.checkInventory(tomato, 1000))

        Time.evening = 4
        pantry.throwExpiredIngredients()
        assertEquals(0, pantry.checkInventory(tomato, 1000))
    }

    // --- F09/P01: incidents affecting procurement ---

    @Test
    fun `ensureQuantities - a PackagingChangeIncident changes the size of subsequently procured packages`() {
        val pantry = buildPantry(emptyList(), listOf(tomato))

        pantry.ensureQuantities(mapOf(tomato to 50))
        assertEquals(100, pantry.checkInventory(tomato, 1000))

        tomato.packagingVolume = 40
        pantry.ensureQuantities(mapOf(tomato to 150))

        val packages = pantry.getPackagesForIngredient(tomato)
        assertEquals(2, packages.count { it.currentAmount == 40 })
        assertEquals(180, packages.sumOf { it.currentAmount })
    }

    @Test
    fun `ensureQuantities - does not procure an ingredient made unavailable by an UnavailabilityIncident`() {
        val pantry = buildPantry(emptyList(), listOf(tomato))
        pantry.supplier.stock.setIngredientToUnavailable(tomato, 2)

        pantry.ensureQuantities(mapOf(tomato to 100))

        assertTrue(pantry.getPackagesForIngredient(tomato).isEmpty())
    }
}
