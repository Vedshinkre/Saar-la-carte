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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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
    @Disabled
    @Test
    fun `valid ingredients are parsed with correct name, unit, packaging volume, and best-before`() {
        val ingredients = jsonArrayOf("[$riceIngredient, $oilIngredient, $onionIngredient]")
        val (parsedIngredients, _) = parser.parse(ingredients, jsonArrayOf(recipeJson()))

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

    @Disabled
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

    // RECIPES: schema fields

    @Test
    fun `valid recipe is parsed with correct fields`() {
        val recipes = """
            [{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC", "SOUS"],
              "ingredients": [{"name": "rice", "amount": 150}], "basicDishFor": "ASIAN"}]
        """
        val (_, parsedRecipes) = parseWithRice(recipes)

        assertEquals(1, parsedRecipes.size)
        val recipe = parsedRecipes.single()
        assertEquals(1, recipe.id)
        assertEquals("Rice Bowl", recipe.name)
        assertEquals(20, recipe.duration)
        assertEquals(listOf(CookType.EXEC, CookType.SOUS), recipe.cookType)
        assertEquals(RestaurantType.ASIAN, recipe.basicDishFor)
        assertEquals(1, recipe.ingredients.size)
        assertEquals(150, recipe.ingredients.entries.single { it.key.name == "rice" }.value)
    }

    @Test
    fun `recipe without basicDishFor is parsed as non-basic`() {
        val (_, parsedRecipes) = parseWithRice(recipeJson())
        assertNull(parsedRecipes.single().basicDishFor)
    }

    @Disabled
    @Test
    fun `empty recipes list is rejected`() {
        assertFailsWith<IllegalArgumentException> { parseWithRice("[]") }
    }

    @Test
    fun `recipe missing id is rejected`() {
        val recipes = """[{"dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"name": "rice", "amount": 150}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe missing dishName is rejected`() {
        val recipes = """[{"id": 1, "duration": 20, "cookType": ["EXEC"],
            "ingredients": [{"name": "rice", "amount": 150}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe missing duration is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "cookType": ["EXEC"],
            "ingredients": [{"name": "rice", "amount": 150}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe missing cookType is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20,
            "ingredients": [{"name": "rice", "amount": 150}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Disabled
    @Test
    fun `recipe with empty cookType is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": [],
            "ingredients": [{"name": "rice", "amount": 150}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe with unknown cook type is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["MANAGER"],
            "ingredients": [{"name": "rice", "amount": 150}]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `recipe missing ingredients is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"]}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Disabled
    @Test
    fun `recipe with empty ingredients is rejected`() {
        val recipes = """[{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
            "ingredients": []}]"""
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    // RECIPE: durations

    @Test
    fun `recipe duration one tick below the minimum is rejected`() {
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipeJson(duration = 1)) }
    }

    @Test
    fun `recipe duration at the minimum boundary is accepted`() {
        val (_, parsedRecipes) = parseWithRice(recipeJson(duration = 2))
        assertEquals(2, parsedRecipes.single().duration)
    }

    @Test
    fun `recipe duration at the maximum boundary is accepted`() {
        val (_, parsedRecipes) = parseWithRice(recipeJson(duration = 40))
        assertEquals(40, parsedRecipes.single().duration)
    }

    @Test
    fun `recipe duration one tick above the maximum is rejected`() {
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipeJson(duration = 41)) }
    }

    // RECIPE: unique id and basic dishname

    @Test
    fun `duplicate recipe id is rejected`() {
        val recipes = """
            [{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
              "ingredients": [{"name": "rice", "amount": 150}]},
             {"id": 1, "dishName": "Other Bowl", "duration": 20, "cookType": ["EXEC"],
              "ingredients": [{"name": "rice", "amount": 150}]}]
        """
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `two basic recipes with distinct dish names are both accepted`() {
        val recipes = """
            [{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
              "ingredients": [{"name": "rice", "amount": 150}], "basicDishFor": "ASIAN"},
             {"id": 2, "dishName": "Rice Soup", "duration": 20, "cookType": ["EXEC"],
              "ingredients": [{"name": "rice", "amount": 150}], "basicDishFor": "EUROPEAN"}]
        """
        val (_, parsedRecipes) = parseWithRice(recipes)
        assertEquals(2, parsedRecipes.size)
    }

    @Test
    fun `two basic recipes sharing the same dish name are rejected`() {
        val recipes = """
            [{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
              "ingredients": [{"name": "rice", "amount": 150}], "basicDishFor": "ASIAN"},
             {"id": 2, "dishName": "Rice Bowl", "duration": 22, "cookType": ["EXEC"],
              "ingredients": [{"name": "rice", "amount": 150}], "basicDishFor": "EUROPEAN"}]
        """
        assertFailsWith<IllegalArgumentException> { parseWithRice(recipes) }
    }

    @Test
    fun `a basic and a non-basic recipe may share the same dish name`() {
        val recipes = """
            [{"id": 1, "dishName": "Rice Bowl", "duration": 20, "cookType": ["EXEC"],
              "ingredients": [{"name": "rice", "amount": 150}]},
             {"id": 2, "dishName": "Rice Bowl", "duration": 22, "cookType": ["EXEC"],
              "ingredients": [{"name": "rice", "amount": 150}], "basicDishFor": "ASIAN"}]
        """
        val (_, parsedRecipes) = parseWithRice(recipes)
        assertEquals(2, parsedRecipes.size)
    }
}
