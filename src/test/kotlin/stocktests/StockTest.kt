package stocktests

import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Unit tests for [Stock.setIngredientToAvailable]. */
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
}
