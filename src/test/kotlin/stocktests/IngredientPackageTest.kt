package stocktests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Unit tests for [IngredientPackage]: a package opens when something is taken from it and never goes below 0. */
class IngredientPackageTest {
    private val rice = Ingredient("rice", MeasurementUnit.G, bestBefore = 3, initialPackagingVolume = 500)

    @BeforeEach
    fun setUp() {
        Time.evening = 1
    }

    @Test
    fun `a new package is full and closed`() {
        val pack = IngredientPackage(rice)

        assertEquals(500, pack.currentAmount)
        assertFalse(pack.isOpen)
    }

    @Test
    fun `taking part of a package opens it`() {
        val pack = IngredientPackage(rice)

        assertEquals(120, pack.removeAmount(120))

        assertEquals(380, pack.currentAmount)
        assertTrue(pack.isOpen)
    }

    @Test
    fun `taking more than is left empties the package and returns what was left`() {
        val pack = IngredientPackage(rice)
        pack.removeAmount(450)

        assertEquals(50, pack.removeAmount(200))

        assertEquals(0, pack.currentAmount)
    }

    @Test
    fun `taking nothing or a negative amount changes nothing and keeps the package closed`() {
        val pack = IngredientPackage(rice)

        assertEquals(0, pack.removeAmount(0))
        assertEquals(0, pack.removeAmount(-10))

        assertEquals(500, pack.currentAmount)
        assertFalse(pack.isOpen)
    }

    @Test
    fun `taking from an empty package returns nothing`() {
        val pack = IngredientPackage(rice, currentAmount = 0, expiryDate = 4, isOpen = true)

        assertEquals(0, pack.removeAmount(30))
    }

    @Test
    fun `a package obtained on evening 1 with best before 3 is good until evening 3`() {
        val pack = IngredientPackage(rice)

        Time.evening = 3
        assertFalse(pack.hasExpired())
        Time.evening = 4
        assertTrue(pack.hasExpired())
    }
}
