package recipechangeincidenttests

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.incidents.RecipeChangeIncident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecipeChangeIncidentTest {

    private val flour = Ingredient("flour", MeasurementUnit.G, 5, 100)
    private val milk = Ingredient("milk", MeasurementUnit.ML, 3, 100)

    private fun recipe(id: Int, vararg amounts: Pair<Ingredient, Int>): Recipe =
        Recipe(id, "r$id", 10, listOf(CookType.TOURNANT), mutableMapOf(*amounts), null)

    private fun incident(adaptation: Int, vararg recipes: Recipe) =
        RecipeChangeIncident(1, 1, flour, adaptation, recipes.toList())

    @Test
    fun `zero adaptation changes nothing`() {
        val r = recipe(1, flour to 42)
        incident(0, r).apply()
        assertEquals(42, r.ingredients[flour])
    }

    @Test
    fun `decrease is rounded down`() {
        val r = recipe(1, flour to 5)
        incident(-50, r).apply() // 2.5 -> 2
        assertEquals(2, r.ingredients[flour])
    }

    @Test
    fun `small increase on small amount floors to unchanged value`() {
        val r = recipe(1, flour to 5)
        incident(10, r).apply() // 5.5 -> 5
        assertEquals(5, r.ingredients[flour])
    }

    @Test
    fun `minus 100 percent clamps to one`() {
        val r = recipe(1, flour to 200)
        incident(-100, r).apply()
        assertEquals(1, r.ingredients[flour])
    }

    @Test
    fun `adaptation below minus 100 percent clamps to one`() {
        val r = recipe(1, flour to 200)
        incident(-250, r).apply() // negative factor
        assertEquals(1, r.ingredients[flour])
    }

    @Test
    fun `result is exact despite floating point representation`() {
        val a = recipe(1, flour to 100)
        incident(15, a).apply()
        assertEquals(115, a.ingredients[flour])
        val b = recipe(2, flour to 100)
        incident(-93, b).apply()
        assertEquals(7, b.ingredients[flour])
    }

    @Test
    fun `large increase`() {
        val r = recipe(1, flour to 10)
        incident(1000, r).apply()
        assertEquals(110, r.ingredients[flour])
    }

    @Test
    fun `other ingredients in the same recipe are untouched`() {
        val r = recipe(1, flour to 100, milk to 20)
        incident(50, r).apply()
        assertEquals(150, r.ingredients[flour])
        assertEquals(20, r.ingredients[milk])
    }

    @Test
    fun `empty recipe list does not fail`() {
        incident(50).apply()
    }

    @Test
    fun `recipes sharing one ingredient map are changed only once`() {
        val original = recipe(1, flour to 100)
        val menuCopy = original.copy() // as built by RestaurantParser: same ingredient map instance
        incident(50, original, menuCopy).apply()
        assertEquals(150, original.ingredients[flour])
        assertEquals(150, menuCopy.ingredients[flour])
    }

    @Test
    fun `recipes with equal but separate ingredient maps are each changed`() {
        val a = recipe(1, flour to 100)
        val b = recipe(2, flour to 100)
        incident(50, a, b).apply()
        assertEquals(150, a.ingredients[flour])
        assertEquals(150, b.ingredients[flour])
    }
}
