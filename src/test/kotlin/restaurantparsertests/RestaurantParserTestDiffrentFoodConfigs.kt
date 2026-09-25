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
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFailsWith

/**
 * Unit tests for [RestaurantParser.parseRestaurants] where the rejection depends on the recipes and
 * stock passed alongside the restaurant fixture, rather than on the restaurant JSON alone: a
 * duplicate dish name across two recipes, no basic dish covering one of the restaurant's types, and
 * a restaurant referencing a recipe id that is not in the supplied recipe list.
 */
class RestaurantParserTestDiffrentFoodConfigs {
    private val basePath =
        "src/systemtest/resources/RestaurantParserTests/"
    val parser = RestaurantParser()
    val chicken = Ingredient(
        name = "chicken",
        unit = MeasurementUnit.G,
        bestBefore = 1,
        initialPackagingVolume = 1000
    )

    val rice = Ingredient(
        name = "rice",
        unit = MeasurementUnit.G,
        bestBefore = 3,
        initialPackagingVolume = 1000
    )

    val pasta = Ingredient(
        name = "pasta",
        unit = MeasurementUnit.G,
        bestBefore = 3,
        initialPackagingVolume = 1000
    )

    val garlic = Ingredient(
        name = "garlic",
        unit = MeasurementUnit.G,
        bestBefore = 1,
        initialPackagingVolume = 50
    )

    val oil = Ingredient(
        name = "oil",
        unit = MeasurementUnit.ML,
        bestBefore = 1,
        initialPackagingVolume = 500
    )

    val beef = Ingredient(
        name = "beef",
        unit = MeasurementUnit.G,
        bestBefore = 1,
        initialPackagingVolume = 100
    )

    val potato = Ingredient(
        name = "potato",
        unit = MeasurementUnit.G,
        bestBefore = 1,
        initialPackagingVolume = 1000
    )

    val onion = Ingredient(
        name = "onion",
        unit = MeasurementUnit.X,
        bestBefore = 1,
        initialPackagingVolume = 10
    )

    val stock = Stock(
        listOf<Ingredient>(
            chicken,
            rice,
            pasta,
            garlic,
            oil,
            beef,
            potato,
            onion
        )
    )
    val recipesNoDuplicateDishName = listOf(
        Recipe(
            id = 1,
            name = "chicken rice",
            duration = 30,
            cookType = listOf(
                CookType.EXEC,
                CookType.SOUS,
                CookType.TOURNANT,
                CookType.ROAST
            ),
            ingredients = mutableMapOf(
                chicken to 100,
                rice to 200,
                onion to 1,
                garlic to 10,
                oil to 20
            ),
            basicDishFor = RestaurantType.ASIAN
        ),

        Recipe(
            id = 2,
            name = "chicken rice",
            duration = 25,
            cookType = listOf(
                CookType.EXEC,
                CookType.SOUS,
                CookType.TOURNANT,
                CookType.ROAST
            ),
            ingredients = mutableMapOf(
                beef to 150,
                pasta to 200,
                onion to 1,
                garlic to 10,
                oil to 20
            ),
            basicDishFor = null
        ),

        Recipe(
            id = 3,
            name = "potato soup",
            duration = 10,
            cookType = listOf(
                CookType.EXEC,
                CookType.SOUS,
                CookType.TOURNANT,
                CookType.VEGETABLE
            ),
            ingredients = mutableMapOf(
                potato to 300,
                onion to 1,
                garlic to 10,
                oil to 20
            ),
            basicDishFor = RestaurantType.EUROPEAN
        )
    )
    val recipesNoBasicDishOfItsType = listOf(
        recipesNoDuplicateDishName[0],

        Recipe(
            id = 2,
            name = "beef pasta",
            duration = 25,
            cookType = listOf(
                CookType.EXEC,
                CookType.SOUS,
                CookType.TOURNANT,
                CookType.ROAST
            ),
            ingredients = mutableMapOf(
                beef to 150,
                pasta to 200,
                onion to 1,
                garlic to 10,
                oil to 20
            ),
            basicDishFor = null
        ),

        Recipe(
            id = 3,
            name = "potato soup",
            duration = 10,
            cookType = listOf(
                CookType.EXEC,
                CookType.SOUS,
                CookType.TOURNANT,
                CookType.VEGETABLE
            ),
            ingredients = mutableMapOf(
                potato to 300,
                onion to 1,
                garlic to 10,
                oil to 20
            ),
            basicDishFor = RestaurantType.ASIAN
        )
    )
    val recipesNotExist = listOf(
        recipesNoDuplicateDishName[0],

        Recipe(
            id = 2,
            name = "beef pasta",
            duration = 25,
            cookType = listOf(
                CookType.EXEC,
                CookType.SOUS,
                CookType.TOURNANT,
                CookType.ROAST
            ),
            ingredients = mutableMapOf(
                beef to 150,
                pasta to 200,
                onion to 1,
                garlic to 10,
                oil to 20
            ),
            basicDishFor = null
        ),

        recipesNoDuplicateDishName[2]
    )

    private fun assertInvalidRestaurants(fileName: String, recipes: List<Recipe>, stock: Stock) {
        val file = File(basePath + fileName)

        val jsonArray = Json.parseToJsonElement(file.readText()).jsonObject.getValue("restaurants").jsonArray

        assertFailsWith<IllegalArgumentException> {
            parser.parseRestaurants(
                jsonArray,
                recipes = recipes,
                stock = stock
            )
        }
    }

    @Test
    fun `noDuplicateDishName`() {
        assertInvalidRestaurants(
            "restaurantsDifferentNameSameId.json",
            recipes = recipesNoDuplicateDishName,
            stock = stock
        )
    }

    @Test
    fun `NoBasicDishOfItsType`() {
        assertInvalidRestaurants("restaurantsNoBasicDishOfItsType.json", recipesNoBasicDishOfItsType, stock = stock)
    }

    @Test
    fun `restaurantsRecipesNotExist`() {
        assertInvalidRestaurants(
            "restaurantsRecipiesNotExist.json",
            recipes = recipesNoDuplicateDishName,
            stock = stock
        )
    }
}
