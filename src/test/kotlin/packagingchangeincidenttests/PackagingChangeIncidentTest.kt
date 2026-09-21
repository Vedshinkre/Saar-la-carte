package packagingchangeincidenttests

import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.incidents.PackagingChangeIncident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PackagingChangeIncidentTest {

    private fun flour() = Ingredient("flour", MeasurementUnit.G, 5, 100)

    private fun incident(ingredient: Ingredient, volume: Int) =
        PackagingChangeIncident(1, 3, ingredient, volume)

    @Test
    fun `zero volume is rejected and leaves the volume unchanged`() {
        val f = flour()
        assertThrows(IllegalArgumentException::class.java) { incident(f, 0).apply() }
        assertEquals(100, f.packagingVolume)
    }

    @Test
    fun `negative volume is rejected and leaves the volume unchanged`() {
        val f = flour()
        assertThrows(IllegalArgumentException::class.java) { incident(f, -5).apply() }
        assertEquals(100, f.packagingVolume)
    }

    @Test
    fun `same volume as before changes nothing`() {
        val f = flour()
        incident(f, 100).apply()
        assertEquals(100, f.packagingVolume)
    }

    @Test
    fun `applying twice is idempotent`() {
        val f = flour()
        val inc = incident(f, 40)
        inc.apply()
        inc.apply()
        assertEquals(40, f.packagingVolume)
    }
}
