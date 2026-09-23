package suppliertests

import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [Supplier.procure], targeting the boundary/off-by-one and negation
 * conditions around how many packages get bought for a given deficit.
 */
class SupplierTest {

    // 100g per package
    private val tomato = Ingredient("tomato", MeasurementUnit.G, bestBefore = 2, initialPackagingVolume = 100)

    private fun supplierFor(vararg ingredients: Ingredient): Supplier = Supplier(Stock(ingredients.toList()))

    @Test
    fun `procure - unavailable ingredient yields no packages even for a positive amount`() {
        val stock = Stock(listOf(tomato))
        stock.setIngredientToUnavailable(tomato, 3)
        val supplier = Supplier(stock)

        val procured = supplier.procure(tomato, 250)

        assertTrue(procured.isEmpty())
    }

    @Test
    fun `procure - zero requested amount yields no packages`() {
        val supplier = supplierFor(tomato)

        assertTrue(supplier.procure(tomato, 0).isEmpty())
    }

    @Test
    fun `procure - negative requested amount yields no packages`() {
        val supplier = supplierFor(tomato)

        assertTrue(supplier.procure(tomato, -50).isEmpty())
    }

    @Test
    fun `procure - deficit exactly matching one package size buys exactly one package`() {
        val supplier = supplierFor(tomato)

        val procured = supplier.procure(tomato, 100)

        assertEquals(1, procured.size)
        assertEquals(100, procured.sumOf { it.currentAmount })
    }

    // --- F08: procuring in whole packages ("next bigger packaging volume") ---

    @Test
    fun `procure - deficit just below one package size still rounds up to one full package`() {
        val supplier = supplierFor(tomato)

        val procured = supplier.procure(tomato, 99)

        assertEquals(1, procured.size)
        assertEquals(100, procured.sumOf { it.currentAmount })
    }

    @Test
    fun `procure - deficit just above one package size requires a second full package`() {
        val supplier = supplierFor(tomato)

        val procured = supplier.procure(tomato, 101)

        assertEquals(2, procured.size)
        assertEquals(200, procured.sumOf { it.currentAmount })
    }

    @Test
    fun `procure - multi-package deficit buys exactly enough whole packages to cover it`() {
        val supplier = supplierFor(tomato)

        val procured = supplier.procure(tomato, 350)

        assertEquals(4, procured.size)
        assertEquals(400, procured.sumOf { it.currentAmount })
    }

    // --- F08: managing packaging volume ---

    @Test
    fun `procure - reflects an updated packaging volume, eg after a Packaging Change incident`() {
        val supplier = supplierFor(tomato)
        tomato.packagingVolume = 250 // simulates a Packaging Change incident resizing the packaging

        val procured = supplier.procure(tomato, 300)

        assertEquals(2, procured.size)
        assertTrue(procured.all { it.currentAmount == 250 })
    }

    @Test
    fun `procure - always uses the packaging volume tracked by stock, not a caller-supplied ingredient instance`() {
        val supplier = supplierFor(tomato)
        // a distinct instance sharing the same name but a stale packaging volume, e.g. captured before an incident
        val staleTomatoReference = Ingredient("tomato", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 999)

        val procured = supplier.procure(staleTomatoReference, 150)

        assertEquals(2, procured.size)
        assertTrue(procured.all { it.currentAmount == 100 })
    }

    // --- F08: ingredient availability ---

    @Test
    fun `isAvailable - true for an ingredient present and not marked unavailable`() {
        val supplier = supplierFor(tomato)

        assertTrue(supplier.isAvailable(tomato))
    }

    @Test
    fun `isAvailable - false for an ingredient never registered in stock`() {
        val cheese = Ingredient("cheese", MeasurementUnit.G, bestBefore = 3, initialPackagingVolume = 200)
        val supplier = supplierFor(tomato)

        assertTrue(!supplier.isAvailable(cheese))
        assertTrue(supplier.procure(cheese, 200).isEmpty())
    }

    @Test
    fun `isAvailable - becomes available again only once the unavailable duration is fully decremented`() {
        val stock = Stock(listOf(tomato))
        stock.setIngredientToUnavailable(tomato, 2)
        val supplier = Supplier(stock)

        assertTrue(!supplier.isAvailable(tomato))

        stock.applyUnavailableDurations() // duration 2 -> 1
        assertTrue(!supplier.isAvailable(tomato))

        stock.applyUnavailableDurations() // duration 1 -> 0
        assertTrue(supplier.isAvailable(tomato))
    }

    @Test
    fun `procure - ingredient becomes procurable again once its unavailable duration ends`() {
        val stock = Stock(listOf(tomato))
        stock.setIngredientToUnavailable(tomato, 1)
        val supplier = Supplier(stock)

        assertTrue(supplier.procure(tomato, 100).isEmpty())

        stock.applyUnavailableDurations() // duration 1 -> 0, available again

        val procured = supplier.procure(tomato, 100)
        assertEquals(1, procured.size)
    }
}
