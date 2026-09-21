package unavailabilityincidenttests

import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.UnavailabilityIncident
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * F34 - Incident: Ingredient Unavailability unit tests
 */
class UnavailabilityIncidentTest {

    private fun rice() = Ingredient("rice", MeasurementUnit.G, bestBefore = 10, initialPackagingVolume = 100)

    private fun garlic() = Ingredient("garlic", MeasurementUnit.G, bestBefore = 50, initialPackagingVolume = 50)

    private fun incident(id: Int, evening: Int, ingredient: Ingredient, duration: Int, stock: Stock) =
        UnavailabilityIncident(id = id, evening = evening, ingredient = ingredient, duration = duration, stock = stock)

    // --- apply() ---

    @Test
    fun `apply marks the ingredient unavailable in stock`() {
        val rice = rice()
        val stock = Stock(listOf(rice))
        val inc = incident(1, 1, rice, 3, stock)

        assertTrue(stock.isIngredientAvailable(rice))
        inc.apply()
        assertFalse(stock.isIngredientAvailable(rice))
    }

    @Test
    fun `apply's duration expires after exactly duration evenings via applyUnavailableDurations`() {
        val rice = rice()
        val stock = Stock(listOf(rice))
        incident(1, 1, rice, 2, stock).apply()

        assertFalse(stock.isIngredientAvailable(rice))
        stock.applyUnavailableDurations() // 2 -> 1, evening it occurred counts towards duration
        assertFalse(stock.isIngredientAvailable(rice))
        stock.applyUnavailableDurations() // 1 -> 0
        assertTrue(stock.isIngredientAvailable(rice))
    }

    @Test
    fun `apply only affects the targeted ingredient`() {
        val rice = rice()
        val garlic = garlic()
        val stock = Stock(listOf(rice, garlic))
        incident(1, 1, rice, 5, stock).apply()

        assertFalse(stock.isIngredientAvailable(rice))
        assertTrue(stock.isIngredientAvailable(garlic))
    }

    @Test
    fun `apply throws when the ingredient does not exist in the given stock`() {
        val rice = rice()
        val stock = Stock(listOf(garlic())) // rice never registered
        val inc = incident(1, 1, rice, 2, stock)

        assertThrows(IllegalArgumentException::class.java) { inc.apply() }
    }

    // --- overlapsWith() ---

    @Test
    fun `overlapsWith is false for different ingredients even with identical evening and duration`() {
        val rice = rice()
        val garlic = garlic()
        val riceIncident = incident(1, 1, rice, 5, Stock(listOf(rice)))
        val garlicIncident = incident(2, 1, garlic, 5, Stock(listOf(garlic)))

        assertFalse(riceIncident.overlapsWith(garlicIncident))
        assertFalse(garlicIncident.overlapsWith(riceIncident))
    }

    @Test
    fun `overlapsWith is false for two distinct Ingredient instances that only share a name`() {
        // the parser always hands out the same shared Ingredient instance per name
        // this documents that overlapsWith relies on that reference identity, not on the name
        val first = incident(1, 1, rice(), 5, Stock(listOf(rice())))
        val second = incident(2, 3, rice(), 5, Stock(listOf(rice())))

        assertFalse(first.overlapsWith(second))
    }

    @Test
    fun `overlapsWith is true when the ranges partially overlap`() {
        val rice = rice()
        val stock = Stock(listOf(rice))
        // covers evenings 1..5 (end exclusive at 6)
        val first = incident(1, 1, rice, 5, stock)
        // covers evenings 3..7 (end exclusive at 8)
        val second = incident(2, 3, rice, 5, stock)

        assertTrue(first.overlapsWith(second))
        assertTrue(second.overlapsWith(first))
    }

    @Test
    fun `overlapsWith is true when one range is fully nested in the other`() {
        val rice = rice()
        val stock = Stock(listOf(rice))
        // covers evenings 1..10
        val outer = incident(1, 1, rice, 10, stock)
        // covers evenings 4..5, fully inside the outer range
        val inner = incident(2, 4, rice, 2, stock)

        assertTrue(outer.overlapsWith(inner))
        assertTrue(inner.overlapsWith(outer))
    }

    @Test
    fun `overlapsWith is false for the same ingredient with adjacent, touching ranges`() {
        val rice = rice()
        val stock = Stock(listOf(rice))
        // covers evenings 1..2 (end exclusive at 3)
        val first = incident(1, 1, rice, 2, stock)
        // starts exactly where the first one's range ends: no overlap
        val second = incident(2, 3, rice, 2, stock)

        assertFalse(first.overlapsWith(second))
        assertFalse(second.overlapsWith(first))
    }

    @Test
    fun `overlapsWith is false for the same ingredient with a gap between the ranges`() {
        val rice = rice()
        val stock = Stock(listOf(rice))
        // covers evenings 1..2
        val first = incident(1, 1, rice, 2, stock)
        // covers evenings 10..11, far away from the first range
        val second = incident(2, 10, rice, 2, stock)

        assertFalse(first.overlapsWith(second))
        assertFalse(second.overlapsWith(first))
    }

    // --- conflictMessage() ---

    @Test
    fun `conflictMessage reports both incidents' id, evening and duration`() {
        val rice = rice()
        val stock = Stock(listOf(rice))
        val first = incident(1, 1, rice, 5, stock)
        val second = incident(2, 3, rice, 5, stock)

        val message = first.conflictMessage(second)

        assertTrue(message.startsWith("Ingredient "))
        assertTrue(message.endsWith(": incident 1 (evening 1, duration 5) overlaps incident 2 (evening 3, duration 5)"))
    }
}
