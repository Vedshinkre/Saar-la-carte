package restaurantparsertests

/**import de.unisaarland.cs.se.selab.enums.CookType
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

class RestaurantParserTestDiffrentFoodConfigs {
 private val basePath = "src/test/resources/RestaurantParserTests/"
 val parser = RestaurantParser()
 val chicken = Ingredient(
 name = "chicken",
 unit = MeasurementUnit.G,
 bestBefore = 1,
 packagingVolume = 1000
 )

 val rice = Ingredient(
 name = "rice",
 unit = MeasurementUnit.G,
 bestBefore = 3,
 packagingVolume = 1000
 )

 val pasta = Ingredient(
 name = "pasta",
 unit = MeasurementUnit.G,
 bestBefore = 3,
 packagingVolume = 1000
 )

 val garlic = Ingredient(
 name = "garlic",
 unit = MeasurementUnit.G,
 bestBefore = 1,
 packagingVolume = 50
 )

 val oil = Ingredient(
 name = "oil",
 unit = MeasurementUnit.ML,
 bestBefore = 1,
 packagingVolume = 500
 )

 val beef = Ingredient(
 name = "beef",
 unit = MeasurementUnit.G,
 bestBefore = 1,
 packagingVolume = 100
 )

 val potato = Ingredient(
 name = "potato",
 unit = MeasurementUnit.G,
 bestBefore = 1,
 packagingVolume = 1000
 )

 val onion = Ingredient(
 name = "onion",
 unit = MeasurementUnit.X,
 bestBefore = 1,
 packagingVolume = 10
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
 ingredients = mapOf(
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
 ingredients = mapOf(
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
 ingredients = mapOf(
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
 ingredients = mapOf(
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
 ingredients = mapOf(
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
 ingredients = mapOf(
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

 val jsonArray = Json
 .parseToJsonElement(file.readText())
 .jsonObject
 .getValue("restaurants")
 .jsonArray

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
 assertInvalidRestaurants("restaurantsRecipesNotExist.json", recipes = recipesNoDuplicateDishName, stock = stock)
 }
}
**/
