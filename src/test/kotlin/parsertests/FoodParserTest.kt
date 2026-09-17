package parsertests

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.parsers.FoodParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun jsonArrayOf(json: String): JsonArray = Json.parseToJsonElement(json).jsonArray

/** Unit tests for [FoodParser], call the parser with example JSONs */
class FoodParserTest {

    private val parser = FoodParser()

    private val riceIngredient = """{"name": "rice", "unit": "g", "packagingVolume": 500, "bestBefore": 3}"""
    private val oilIngredient = """{"name": "oil", "unit": "mL", "packagingVolume": 1000, "bestBefore": 7}"""
    private val onionIngredient = """{"name": "onion", "unit": "X", "packagingVolume": 10, "bestBefore": 2}"""

    private val singleIngredient = jsonArrayOf("[$riceIngredient]")

    private fun parseWithRice(recipesJson: String): Pair<List<Ingredient>, List<Recipe>> =
        parser.parse(singleIngredient, jsonArrayOf(recipesJson))

    private fun recipeJson(id: Int = 1, duration: Int = 20, dishName: String = "Rice Bowl"): String = """
        [{"id": $id, "dishName": "$dishName", "duration": $duration, "cookType": ["EXEC"],
          "ingredients": [{"name": "rice", "amount": 150}]}]
    """

    // INGREDIENTS

    @Test
    fun `valid ingredients are parsed with correct name, unit, packaging volume, and best-before`() {
        val ingredients = jsonArrayOf("[$riceIngredient, $oilIngredient, $onionIngredient]")
        val (parsedIngredients, _) = parser.parse(ingredients, jsonArrayOf("[]"))

        assertEquals(3, parsedIngredients.size)

        val rice = parsedIngredients.single { it.name == "rice" }
        assertEquals(MeasurementUnit.G, rice.unit)
        assertEquals(500, rice.packagingVolume)
        assertEquals(3, rice.bestBefore)

        val oil = parsedIngredients.single { it.name == "oil" }
        assertEquals(MeasurementUnit.ML, oil.unit)
        assertEquals(1000, oil.packagingVolume)
        assertEquals(7, oil.bestBefore)

        val onion = parsedIngredients.single { it.name == "onion" }
        assertEquals(MeasurementUnit.X, onion.unit)
        assertEquals(10, onion.packagingVolume)
        assertEquals(2, onion.bestBefore)
    }

    @Test
    fun `empty ingredients list is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            parser.parse(jsonArrayOf("[]"), jsonArrayOf("[]"))
        }
    }

    @Test
    fun `ingredient missing name is rejected`() {
        val bad = jsonArrayOf("""[{"unit": "g", "packagingVolume": 500, "bestBefore": 3}]""")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    @Test
    fun `ingredient missing unit is rejected`() {
        val bad = jsonArrayOf("""[{"name": "rice", "packagingVolume": 500, "bestBefore": 3}]""")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    @Test
    fun `ingredient missing packaging volume is rejected`() {
        val bad = jsonArrayOf("""[{"name": "rice", "unit": "g", "bestBefore": 3}]""")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    @Test
    fun `ingredient missing best-before is rejected`() {
        val bad = jsonArrayOf("""[{"name": "rice", "unit": "g", "packagingVolume": 500}]""")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    @Test
    fun `duplicate ingredient name is rejected`() {
        val bad = jsonArrayOf("[$riceIngredient, $riceIngredient]")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    @Test
    fun `unknown measurement unit is rejected`() {
        val bad = jsonArrayOf("""[{"name": "rice", "unit": "kg", "packagingVolume": 500, "bestBefore": 3}]""")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    @Test
    fun `zero packaging volume is rejected`() {
        val bad = jsonArrayOf("""[{"name": "rice", "unit": "g", "packagingVolume": 0, "bestBefore": 3}]""")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    @Test
    fun `negative packaging volume is rejected`() {
        val bad = jsonArrayOf("""[{"name": "rice", "unit": "g", "packagingVolume": -5, "bestBefore": 3}]""")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    @Test
    fun `zero best-before is rejected`() {
        val bad = jsonArrayOf("""[{"name": "rice", "unit": "g", "packagingVolume": 500, "bestBefore": 0}]""")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    @Test
    fun `negative best-before is rejected`() {
        val bad = jsonArrayOf("""[{"name": "rice", "unit": "g", "packagingVolume": 500, "bestBefore": -1}]""")
        assertFailsWith<IllegalArgumentException> { parser.parse(bad, jsonArrayOf("[]")) }
    }

    // RECIPE: ingredient references

    @Test
    fun `recipe ingredient missing name is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"amount": 150}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe ingredient missing amount is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"name": "rice"}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe ingredient amount of zero is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"name": "rice", "amount": 0}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe ingredient amount below zero is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"name": "rice", "amount": -5}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe referencing an ingredient that does not exist is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"name": "unobtainium", "amount": 5}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe ingredient unit matching the ingredient's defined unit is accepted`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"name": "rice", "unit": "g", "amount": 150}]}]"""
        val (_, parsedRecipes) = parseWithRice(recipes)
        assertEquals(150, parsedRecipes.single().ingredients.values.single())
    }

    @Test
    fun `recipe ingredient unit mismatching the ingredient's defined unit is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"name": "rice", "unit": "mL", "amount": 150}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe ingredient without an explicit unit is accepted using the ingredient's defined unit`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"name": "rice", "amount": 150}]}]"""
        val (_, parsedRecipes) = parseWithRice(recipes)
        val (ingredient, amount) = parsedRecipes.single().ingredients.entries.single()
        assertEquals(MeasurementUnit.G, ingredient.unit)
        assertEquals(150, amount)
    }

    @Test
    fun `multiple ingredients in one recipe are all captured with their own amounts`() {
        val ingredients = jsonArrayOf("[$riceIngredient, $oilIngredient, $onionIngredient]")
        val recipes = jsonArrayOf(
            """[{"id": 1, "dishName": "Fried Rice", "duration": 20, "cookType": ["EXEC"],
                "ingredients": [
                  {"name": "rice", "amount": 200},
                  {"name": "oil", "amount": 20},
                  {"name": "onion", "amount": 1}
                ]}]"""
        )
        val (_, parsedRecipes) = parser.parse(ingredients, recipes)
        val recipeIngredients = parsedRecipes.single().ingredients
        assertEquals(3, recipeIngredients.size)
        assertEquals(200, recipeIngredients.entries.single { it.key.name == "rice" }.value)
        assertEquals(20, recipeIngredients.entries.single { it.key.name == "oil" }.value)
        assertEquals(1, recipeIngredients.entries.single { it.key.name == "onion" }.value)
    }

    @Test
    fun `parse returns both ingredients and recipes together`() {
        val (parsedIngredients, parsedRecipes) = parseWithRice(recipeJson())
        assertEquals(1, parsedIngredients.size)
        assertEquals(1, parsedRecipes.size)
    }

    // DOTO: SKERDI's OTHER RECIPE TESTS
}
