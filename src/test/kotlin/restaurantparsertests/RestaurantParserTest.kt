package restaurantparsertests

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.parsers.RestaurantParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFailsWith

class RestaurantParserTest {


    private val basePath = "src/test/resources/RestaurantParserTests/"

    private lateinit var parser: RestaurantParser

    @BeforeEach
    fun setUp() {
        parser = RestaurantParser()
    }

    private fun assertInvalidRestaurants(fileName: String) {
        val file = File(basePath + fileName)

        val jsonArray = Json
            .parseToJsonElement(file.readText())
            .jsonObject
            .getValue("restaurants")
            .jsonArray
        val rice = Ingredient(
            name = "rice",
            unit = MeasurementUnit.G, // use your enum value for "g"
            bestBefore = 5,
            packagingVolume = 20
        )
        val ingredients = listOf<Ingredient>(rice)
        val recipes = listOf(
            Recipe(
                id = 1,
                name = "Rice Bowl",
                duration = 2,
                cookType = listOf(CookType.EXEC),
                ingredients = mapOf(rice to 1),
                basicDishFor = RestaurantType.ASIAN
            )
        )
        val stock = Stock(ingredients)

        assertFailsWith<IllegalArgumentException> {
            parser.parseRestaurants(
                jsonArray,
                recipes = recipes,
                stock = stock
            )
        }
    }

    @Test
    fun `restaurantsDifferentNameSameId should fail`() {
        assertInvalidRestaurants("restaurantsDifferentNameSameId.json")
    }

    @Test
    fun `restaurantsSameNameDifferentId should fail`() {
        assertInvalidRestaurants("restaurantsSameNameDifferentId.json")
    }

    @Test
    fun `restaurantsNotUniqueTables should fail`() {
        assertInvalidRestaurants("restaurantsNotUniqueTables.json")
    }

    @Test
    fun `restaurantsNoCookExists should fail`() {
        assertInvalidRestaurants("restaurantsNoCookExists.json")
    }

    @Test
    fun `restaurantsNoWaiterExists should fail`() {
        assertInvalidRestaurants("restaurantsNoWaiterExists.json")
    }

    @Test
    fun `restaurantsNoTableExists should fail`() {
        assertInvalidRestaurants("restaurantsNoTableExists.json")
    }

    @Test
    fun `restaurantsNoRecipieExists should fail`() {
        assertInvalidRestaurants("restaurantsNoRecipieExists.json")
    }

    @Test
    fun `restaurantsNoMultipleHeadCooksieEXEC should fail`() {
        assertInvalidRestaurants("restaurantsNoMultipleHeadCooksieEXEC.json")
    }

    @Test
    fun `restaurantsNoInvalidTableSize should fail`() {
        assertInvalidRestaurants("restaurantsNoInvalidTableSize.json")
    }

    @Test
    fun `restaurantsNoInvalidTableSize1 should fail`() {
        assertInvalidRestaurants("restaurantsNoInvalidTableSize1.json")
    }

    @Test
    fun `restaurantsOpeningTickEndLessThanOpeningTickStart should fail`() {
        assertInvalidRestaurants(
            "restaurantsOpeningTickEndLessThanOpeningTickStart.json"
        )
    }

    @Test
    fun `restaurantsTickNotIn1to24 should fail`() {
        assertInvalidRestaurants("restaurantsTickNotIn1to24.json")
    }

    @Test
    fun `restaurantsPositiveRatingsG0 should fail`() {
        assertInvalidRestaurants("restaurantsPositiveRatingsG0.json")
    }

    @Test
    fun `restaurantsPositiveRatingsNeg0 should fail`() {
        assertInvalidRestaurants("restaurantsPositiveRatingsNeg0.json")
    }
}
