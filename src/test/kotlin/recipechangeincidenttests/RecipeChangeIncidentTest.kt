package recipechangeincidenttests

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.incidents.RecipeChangeIncident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class RecipeChangeIncidentTest {

    private val flour = Ingredient("flour", MeasurementUnit.G, 5, 100)
    private val milk = Ingredient("milk", MeasurementUnit.ML, 3, 100)

    private fun recipe(id: Int, vararg amounts: Pair<Ingredient, Int>): Recipe =
        Recipe(id, "r$id", 10, listOf(CookType.TOURNANT), mutableMapOf(*amounts), null)

    private fun incident(adaptation: Int, vararg recipes: Recipe) =
        RecipeChangeIncident(1, 1, flour, adaptation, recipes.toList())

    @Test
    fun `type is RECIPE`() {
        assertEquals("RECIPE", incident(10).type)
    }

    @Test
    fun `positive adaptation increases amount`() {
        val r = recipe(1, flour to 100)
        incident(50, r).apply()
        assertEquals(150, r.ingredients[flour])
    }

    @Test
    fun `negative adaptation decreases amount`() {
        val r = recipe(1, flour to 100)
        incident(-25, r).apply()
        assertEquals(75, r.ingredients[flour])
    }

    @Test
    fun `zero adaptation changes nothing`() {
        val r = recipe(1, flour to 42)
        incident(0, r).apply()
        assertEquals(42, r.ingredients[flour])
    }

    @Test
    fun `increase is rounded down`() {
        val r = recipe(1, flour to 3)
        incident(50, r).apply() // 4.5 -> 4
        assertEquals(4, r.ingredients[flour])
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
    fun `amount never drops below one`() {
        val r = recipe(1, flour to 1)
        incident(-1, r).apply() // 0.99 -> 0 -> clamped to 1
        assertEquals(1, r.ingredients[flour])
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
    fun `large increase`() {
        val r = recipe(1, flour to 10)
        incident(1000, r).apply()
        assertEquals(110, r.ingredients[flour])
    }

    @Test
    fun `recipe without the ingredient is untouched and not extended`() {
        val r = recipe(1, milk to 20)
        incident(50, r).apply()
        assertEquals(mapOf(milk to 20), r.ingredients.toMap())
        assertFalse(r.ingredients.containsKey(flour))
    }

    @Test
    fun `other ingredients in the same recipe are untouched`() {
        val r = recipe(1, flour to 100, milk to 20)
        incident(50, r).apply()
        assertEquals(150, r.ingredients[flour])
        assertEquals(20, r.ingredients[milk])
    }

    @Test
    fun `all affected recipes are changed and unaffected ones skipped`() {
        val a = recipe(1, flour to 10)
        val b = recipe(2, milk to 10)
        val c = recipe(3, flour to 20, milk to 5)
        incident(100, a, b, c).apply()
        assertEquals(20, a.ingredients[flour])
        assertEquals(10, b.ingredients[milk])
        assertEquals(40, c.ingredients[flour])
        assertEquals(5, c.ingredients[milk])
    }

    @Test
    fun `empty recipe list does not fail`() {
        incident(50).apply()
    }

    @Test
    fun `recipe with no ingredients does not fail`() {
        val r = recipe(1)
        incident(50, r).apply()
        assertEquals(0, r.ingredients.size)
    }

    @Test
    fun `applying twice compounds`() {
        val r = recipe(1, flour to 100)
        val inc = incident(10, r)
        inc.apply()
        inc.apply()
        assertEquals(121, r.ingredients[flour])
    }

    @Test
    fun `ingredient is matched by identity not by name`() {
        val otherFlour = Ingredient("flour", MeasurementUnit.G, 5, 100)
        val r = recipe(1, otherFlour to 100)
        incident(50, r).apply()
        assertEquals(100, r.ingredients[otherFlour])
    }
}
