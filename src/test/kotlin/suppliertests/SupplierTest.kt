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
    // DENIZ continues
}
