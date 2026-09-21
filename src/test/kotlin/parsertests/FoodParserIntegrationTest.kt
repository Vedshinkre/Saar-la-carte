package parsertests

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.parsers.ParserController
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val RESTAURANT_FIXTURES =
    "src/systemtest/kotlin/de/unisaarland/cs/se/selab/systemtest/selab26/restaurantparsertests/"
private const val FIXTURES = "src/test/kotlin/parsertests/fixtures/"

private const val RESTAURANTS_VALID = RESTAURANT_FIXTURES + "restaurants.json"
private const val SCENARIO_VALID = FIXTURES + "scenarioValid.json"

/**
 * Integration tests for the food configuration: unlike [FoodParserTest], these go through
 * [ParserController], meaning the JSON-schema validation, the JSON parsing library and
 * [de.unisaarland.cs.se.selab.parsers.FoodParser]'s cross-validation all run together, and
 * the resulting ingredients/recipes are checked for value-correctness, not merely
 * accept/reject outcomes.
 */
class FoodParserIntegrationTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    private fun parse(food: String) =
        ParserController().parseFiles(food, RESTAURANTS_VALID, SCENARIO_VALID)

    @Disabled
    @Test
    fun `a food file with several ingredients and recipes is parsed with correct values end-to-end`() {
        val result = parse(FIXTURES + "foodMultipleIngredientsAndRecipes.json")

        assertFalse(result.wasInvalidFile)
        assertEquals(3, result.ingredients.size)
        assertEquals(3, result.recipes.size)

        val oil = result.ingredients.single { it.name == "oil" }
        assertEquals(MeasurementUnit.ML, oil.unit)
        assertEquals(500, oil.packagingVolume)
        assertEquals(3, oil.bestBefore)

        val onion = result.ingredients.single { it.name == "onion" }
        assertEquals(MeasurementUnit.X, onion.unit)
        assertEquals(10, onion.packagingVolume)
        assertEquals(2, onion.bestBefore)

        val friedRice = result.recipes.single { it.name == "Fried Rice" }
        assertEquals(3, friedRice.ingredients.size)
        assertEquals(100, friedRice.ingredients.entries.single { it.key.name == "rice" }.value)
        assertEquals(20, friedRice.ingredients.entries.single { it.key.name == "oil" }.value)
        assertEquals(1, friedRice.ingredients.entries.single { it.key.name == "onion" }.value)

        val onionSoup = result.recipes.single { it.name == "Onion Soup" }
        assertEquals(RestaurantType.EUROPEAN, onionSoup.basicDishFor)
    }

    @Test
    fun `schema rejects an ingredient with an unrecognized unit before the parser ever runs`() {
        val result = parse(FIXTURES + "foodSchemaInvalidUnit.json")

        assertTrue(result.wasInvalidFile)
        assertTrue(result.ingredients.isEmpty())
        assertTrue(result.recipes.isEmpty())
    }

    @Test
    fun `two basic recipes sharing a dish name are parsed by the schema but rejected by the parser`() {
        val result = parse(FIXTURES + "foodDuplicateDishNameMixedBasic.json")

        assertTrue(result.wasInvalidFile)
        assertTrue(result.ingredients.isEmpty())
        assertTrue(result.recipes.isEmpty())
    }

    @Test
    fun `a food file that is not valid JSON is an invalid file and stops the parsing`() {
        val food = tempDir.resolve("food.json").toFile().also { it.writeText("{ \"ingredients\": [ ") }

        val result = parse(food.path)

        assertTrue(result.wasInvalidFile)
        assertTrue(result.ingredients.isEmpty())
        assertEquals(
            listOf("[IMPORTANT] Initialization Info: ${food.path} is invalid."),
            output.toString().lines().filter { it.isNotBlank() }
        )
    }

    @Test
    fun `a food file that does not exist is an invalid file`() {
        val missing = tempDir.resolve("missing.json").toString()

        val result = parse(missing)

        assertTrue(result.wasInvalidFile)
        assertTrue(output.toString().contains("$missing is invalid."))
    }
}
