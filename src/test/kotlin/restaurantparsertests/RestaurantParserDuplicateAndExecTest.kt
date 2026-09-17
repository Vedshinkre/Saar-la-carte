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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFailsWith

class RestaurantParserDuplicateAndExecTest {

    private val basePath =
        "src/systemtest/resources/RestaurantParserTests/"

    private lateinit var parser: RestaurantParser

    @BeforeEach
    fun setUp() {
        parser = RestaurantParser()
    }

    private fun assertInvalidRestaurants(fileName: String) {
        val file = File(basePath + fileName)

        val jsonArray = Json.parseToJsonElement(file.readText()).jsonObject.getValue("restaurants").jsonArray
        val rice = Ingredient(
            name = "rice",
            unit = MeasurementUnit.G,
            bestBefore = 5,
            initialPackagingVolume = 20
        )
        val ingredients = listOf<Ingredient>(rice)
        val recipes = listOf(
            Recipe(
                id = 1,
                name = "Rice Bowl",
                duration = 2,
                cookType = listOf(CookType.EXEC),
                ingredients = mutableMapOf(rice to 1),
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

    // PLEASE_FIX These tests fail because the files you pass to the asserInvalidRestaurants don't exist
    @Disabled
    @Test
    fun `restaurantsDuplicateRestaurantIds should fail`() {
        assertInvalidRestaurants("restaurantsDuplicateRestaurantIds.json")
    }

    @Disabled
    @Test
    fun `restaurantsDuplicateTableIds should fail`() {
        assertInvalidRestaurants("restaurantsDuplicateTableIds.json")
    }
    // Ansh Fix Head Cooks

    @Disabled
    @Test
    fun `restaurantsMultipleExecCooks should fail`() {
        assertInvalidRestaurants("restaurantsMultipleExecCooks.json")
    }

    @Disabled
    @Test
    fun `restaurantsNonExistingRecipe should fail`() {
        assertInvalidRestaurants("restaurantsNonExistingRecipe.json")
    }

    @Disabled
    @Test
    fun `restaurantsSameId should fail`() {
        assertInvalidRestaurants("restaurantsSameId.json")
    }

    @Disabled
    @Test
    fun `restaurantsSameName should fail`() {
        assertInvalidRestaurants("restaurantsSameName.json")
    }
}
