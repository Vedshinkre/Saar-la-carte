package parsertests

import de.unisaarland.cs.se.selab.parsers.FoodParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [FoodParser] is called directly, so the JSON schema does not run first: a recipe with a missing field must be
 * rejected by the parser itself with a message that names the recipe and the field.
 */
class FoodParserMissingFieldsTest {
    private val parser = FoodParser()

    private val rice = """{"name": "rice", "unit": "g", "packagingVolume": 500, "bestBefore": 3}"""

    private fun array(json: String): JsonArray = Json.parseToJsonElement(json).jsonArray

    private fun recipe(
        id: String = "\"id\": 1,",
        dishName: String = "\"dishName\": \"Rice Bowl\",",
        duration: String = "\"duration\": 20,",
        cookType: String = "\"cookType\": [\"EXEC\"],",
        ingredients: String = "\"ingredients\": [{\"name\": \"rice\", \"amount\": 150}]"
    ) = "[{$id $dishName $duration $cookType $ingredients}]"

    private fun rejectionOf(recipes: String): String =
        assertFailsWith<IllegalArgumentException> { parser.parse(array("[$rice]"), array(recipes)) }.message.orEmpty()

    @Test
    fun `an empty recipe list is rejected`() {
        assertEquals("The recipe list cannot be empty.", rejectionOf("[]"))
    }

    @Test
    fun `a recipe without an id is rejected`() {
        assertTrue(rejectionOf(recipe(id = "")).contains("missing an 'id'"))
    }

    @Test
    fun `a recipe without a dish name is rejected and named by its id`() {
        val message = rejectionOf(recipe(dishName = ""))

        assertTrue(message.contains("'1'") && message.contains("dishName"), message)
    }

    @Test
    fun `a recipe without a duration is rejected and named by its dish`() {
        val message = rejectionOf(recipe(duration = ""))

        assertTrue(message.contains("Rice Bowl") && message.contains("duration"), message)
    }

    @Test
    fun `a recipe without cook types is rejected and named by its dish`() {
        val message = rejectionOf(recipe(cookType = ""))

        assertTrue(message.contains("Rice Bowl") && message.contains("cookType"), message)
    }

    @Test
    fun `a recipe without ingredients is rejected and named by its dish`() {
        val message = rejectionOf(recipe(ingredients = "\"x\": 0"))

        assertTrue(message.contains("Rice Bowl") && message.contains("ingredients"), message)
    }
}
