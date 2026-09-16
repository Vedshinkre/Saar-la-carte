package de.unisaarland.cs.se.selab.parsers

import com.github.erosb.jsonsKema.JsonParser
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.Validator
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger
import de.unisaarland.cs.se.selab.system.SimulationConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File

/** delegates objects to other parsers */
class ParserController {

    private val foodParser = FoodParser()
    private val restaurantParser = RestaurantParser()
    private val scenarioParser = ScenarioParser()
    private val simConfig = SimulationConfig()

    /** delegates JsonObjects to multiple Parsers and Validates File with JsonSkema **/
    fun parseFiles(
        foodFilePath: String,
        restaurantsFilePath: String,
        scenarioFilePath: String,
    ): SimulationConfig {
        val (ingredients, recipes) = processFile(
            foodFilePath,
            "classpath:/schema/food.schema"
        ) { json ->
            foodParser.parse(
                json.getValue("ingredients").jsonArray,
                json.getValue("recipes").jsonArray
            )
        } ?: return simConfig

        val stock = Stock(ingredients)

        val (restaurantStats, restaurants) = processFile(
            restaurantsFilePath, "classpath:/schema/restaurants.schema"
        ) { json ->
            restaurantParser.parseRestaurants(
                json.getValue("restaurants").jsonArray,
                recipes,
                stock
            )
        } ?: return simConfig

        val (incidents, customers) = processFile(
            scenarioFilePath, "classpath:/schema/scenario.schema"
        ) { json ->
            scenarioParser.parseScenario(
                json.getValue("incidents").jsonArray,
                json.getValue("customerGroups").jsonArray,
                restaurants,
                recipes,
                ingredients,
                stock,
            )
        } ?: return simConfig

        simConfig.ingredients = ingredients
        simConfig.recipes = recipes
        simConfig.restaurants = restaurants
        simConfig.restaurantStats = restaurantStats
        simConfig.incidents = incidents
        simConfig.customers = customers

        return simConfig
    }

    /** Schema-validates [filePath], runs [block] over JSON object, logging and recording file errors */
    private inline fun <T> processFile(filePath: String, schemaPath: String, block: (JsonObject) -> T): T? {
        if (!isSchemaValid(filePath, schemaPath)) return null

        val json = Json.parseToJsonElement(File(filePath).readText()).jsonObject
        return try {
            block(json).also { InitialAndPrepLogger.logInitialization(true, filePath) }
        } catch (_: IllegalArgumentException) {
            invalidate(filePath)
            null
        } catch (_: NoSuchElementException) {
            invalidate(filePath)
            null
        }
    }

    private fun isSchemaValid(filePath: String, schemaPath: String): Boolean {
        val jsonInstance = JsonParser(File(filePath).readText()).parse()
        val schema = SchemaLoader.forURL(schemaPath).load()
        val isValid = Validator.forSchema(schema).validate(jsonInstance) == null
        if (!isValid) {
            InitialAndPrepLogger.logInitialization(false, filePath)
            simConfig.wasInvalidFile = true
        }
        return isValid
    }

    private fun invalidate(filePath: String /**e: Exception*/) {
        InitialAndPrepLogger.logInitialization(false, filePath)
        // System.err.println("Invalid data in '$filePath': ${e.message ?: "Unknown error"}")
        simConfig.wasInvalidFile = true
    }
}
