package packagingchangeincidenttests

import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
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

    private val oldOnionVolume = 10
    private val oldGarlicVolume = 50
    private val onion = Ingredient("onion", MeasurementUnit.X, 1, oldOnionVolume)
    private val garlic = Ingredient("garlic", MeasurementUnit.G, 1, oldGarlicVolume)

    private fun createIncident(ingredient: Ingredient, packagingVolume: Int): PackagingChangeIncident {
        return PackagingChangeIncident(1, 1, ingredient, packagingVolume)
    }

    @Test
    fun `Increase Packaging Volume`() {
        val newVolume = 11
        val incident: PackagingChangeIncident = createIncident(onion, newVolume)
        incident.apply()
        assertEquals(newVolume, onion.packagingVolume)
    }

    @Test
    fun `Decrease Packaging Volume`() {
        val newVolume = 9
        val incident: PackagingChangeIncident = createIncident(onion, newVolume)
        incident.apply()
        assertEquals(newVolume, onion.packagingVolume)
    }

    @Test
    fun `Incident (Same Volume) Applies To Supplier - single ingredient, same demand`() {
        val newVolume = oldOnionVolume
        val demand = newVolume
        val incident: PackagingChangeIncident = createIncident(onion, newVolume)
        val stock = Stock(listOf(onion))
        val supplier = Supplier(stock)

        incident.apply()
        val onionPackages: List<IngredientPackage> = supplier.procure(onion, demand)
        assertEquals(1, onionPackages.size)
        assertEquals(newVolume, onionPackages.first().currentAmount)
    }

    @Test
    fun `Incident (Increased Volume) Applies To Supplier - single ingredient, lower demand`() {
        val newVolume = oldOnionVolume + 1
        val demand = newVolume - 1
        val incident: PackagingChangeIncident = createIncident(onion, newVolume)
        val stock = Stock(listOf(onion))
        val supplier = Supplier(stock)

        incident.apply()
        val onionPackages: List<IngredientPackage> = supplier.procure(onion, demand)
        assertEquals(1, onionPackages.size)
        assertEquals(newVolume, onionPackages.first().currentAmount)
    }

    @Test
    fun `Incident (Decreased Volume) Applies To Supplier - single ingredient, higher demand`() {
        val newVolume = oldOnionVolume - 1
        val demand = newVolume + 1
        val incident: PackagingChangeIncident = createIncident(onion, newVolume)
        val stock = Stock(listOf(onion))
        val supplier = Supplier(stock)

        incident.apply()
        val onionPackages: List<IngredientPackage> = supplier.procure(onion, demand)
        assertEquals(2, onionPackages.size)
        onionPackages.forEach { assertEquals(newVolume, it.currentAmount) }
    }

    @Test
    fun `Incident (Same Volume) Applies To Supplier - multiple ingredient, same demand`() {
        val newGarlicVolume = oldGarlicVolume
        val garlicDemand = newGarlicVolume
        val garlicIncident: PackagingChangeIncident = createIncident(garlic, newGarlicVolume)

        val newOnionVolume = oldOnionVolume
        val onionDemand = newOnionVolume
        val onionIncident: PackagingChangeIncident = createIncident(onion, newOnionVolume)

        val stock = Stock(listOf(onion, garlic))
        val supplier = Supplier(stock)

        garlicIncident.apply()
        val garlicPackages: List<IngredientPackage> = supplier.procure(garlic, garlicDemand)
        assertEquals(1, garlicPackages.size)
        assertEquals(newGarlicVolume, garlicPackages.first().currentAmount)

        onionIncident.apply()
        val onionPackages: List<IngredientPackage> = supplier.procure(onion, onionDemand)
        assertEquals(1, onionPackages.size)
        assertEquals(newOnionVolume, onionPackages.first().currentAmount)
    }

    @Test
    fun `Incident (Increased Volume) Applies To Supplier - multiple ingredient, lower demand`() {
        val newGarlicVolume = oldGarlicVolume + 1
        val garlicDemand = newGarlicVolume - 1
        val garlicIncident: PackagingChangeIncident = createIncident(garlic, newGarlicVolume)

        val newOnionVolume = oldOnionVolume + 1
        val onionDemand = newOnionVolume - 1
        val onionIncident: PackagingChangeIncident = createIncident(onion, newOnionVolume)

        val stock = Stock(listOf(onion, garlic))
        val supplier = Supplier(stock)

        garlicIncident.apply()
        val garlicPackages: List<IngredientPackage> = supplier.procure(garlic, garlicDemand)
        assertEquals(1, garlicPackages.size)
        assertEquals(newGarlicVolume, garlicPackages.first().currentAmount)

        onionIncident.apply()
        val onionPackages: List<IngredientPackage> = supplier.procure(onion, onionDemand)
        assertEquals(1, onionPackages.size)
        assertEquals(newOnionVolume, onionPackages.first().currentAmount)
    }

    @Test
    fun `Incident (Decreased Volume) Applies To Supplier - multiple ingredient, higher demand`() {
        val newGarlicVolume = oldGarlicVolume - 1
        val garlicDemand = newGarlicVolume + 1
        val garlicIncident: PackagingChangeIncident = createIncident(garlic, newGarlicVolume)

        val newOnionVolume = oldOnionVolume - 1
        val onionDemand = newOnionVolume + 1
        val onionIncident: PackagingChangeIncident = createIncident(onion, newOnionVolume)

        val stock = Stock(listOf(onion, garlic))
        val supplier = Supplier(stock)

        garlicIncident.apply()
        val garlicPackages: List<IngredientPackage> = supplier.procure(garlic, garlicDemand)
        assertEquals(2, garlicPackages.size)
        garlicPackages.forEach { assertEquals(newGarlicVolume, it.currentAmount) }

        onionIncident.apply()
        val onionPackages: List<IngredientPackage> = supplier.procure(onion, onionDemand)
        assertEquals(2, onionPackages.size)
        onionPackages.forEach { assertEquals(newOnionVolume, it.currentAmount) }
    }
}
