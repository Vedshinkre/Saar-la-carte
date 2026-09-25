package stocktests

import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Unit tests for [Stock]: availability of ingredients and lookup by name. */
class StockTest {

    private fun ingredient(name: String) = Ingredient(name, MeasurementUnit.G, 3, 500)

    @Test
    fun `matches by name, not by instance`() {
        val stock = Stock(listOf(ingredient("rice")))
        stock.setIngredientToUnavailable(ingredient("rice"), 2)

        stock.setIngredientToAvailable(ingredient("rice"))

        assertTrue(stock.isIngredientAvailable(ingredient("rice")))
    }

    @Test
    fun `only affects the named ingredient`() {
        val rice = ingredient("rice")
        val oil = ingredient("oil")
        val stock = Stock(listOf(rice, oil))
        stock.setIngredientToUnavailable(rice, 2)
        stock.setIngredientToUnavailable(oil, 2)

        stock.setIngredientToAvailable(rice)

        assertTrue(stock.isIngredientAvailable(rice))
        assertFalse(stock.isIngredientAvailable(oil))
    }

    @Test
    fun `throws for an ingredient not in stock`() {
        val stock = Stock(listOf(ingredient("rice")))

        val ex = assertFailsWith<IllegalArgumentException> {
            stock.setIngredientToAvailable(ingredient("saffron"))
        }
        assertEquals(
            "Cannot make ingredient 'saffron' available because it does not exist in stock.",
            ex.message
        )
    }

    @Test
    fun `getIngredient finds the stored ingredient by name`() {
        val rice = ingredient("rice")
        val stock = Stock(listOf(rice))

        assertSame(rice, stock.getIngredient(ingredient("rice")))
    }

    @Test
    fun `getIngredient is null for an ingredient that is not in stock`() {
        val stock = Stock(listOf(ingredient("rice")))

        assertNull(stock.getIngredient(ingredient("saffron")))
    }

    @Test
    fun `an ingredient that is not in stock is not available`() {
        val stock = Stock(listOf(ingredient("rice")))

        assertFalse(stock.isIngredientAvailable(ingredient("saffron")))
    }

    @Test
    fun `an unavailable ingredient becomes available after its last evening`() {
        val rice = ingredient("rice")
        val stock = Stock(listOf(rice))
        stock.setIngredientToUnavailable(rice, 2)

        stock.applyUnavailableDurations()
        assertFalse(stock.isIngredientAvailable(rice))
        stock.applyUnavailableDurations()
        assertTrue(stock.isIngredientAvailable(rice))
        stock.applyUnavailableDurations()
        assertTrue(stock.isIngredientAvailable(rice), "the duration never drops below zero")
    }

    @Test
    fun `the packaging volume of an ingredient can only be changed to a positive value`() {
        val rice = ingredient("rice")

        assertFailsWith<IllegalArgumentException> { rice.packagingVolume = 0 }
        assertFailsWith<IllegalArgumentException> { rice.packagingVolume = -5 }
        assertEquals(500, rice.packagingVolume, "a rejected value leaves the volume as it was")

        rice.packagingVolume = 250
        assertEquals(250, rice.packagingVolume)
    }
}
