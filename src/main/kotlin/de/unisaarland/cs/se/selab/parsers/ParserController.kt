package de.unisaarland.cs.se.selab.parsers

import com.github.erosb.jsonsKema.JsonParseException
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
import java.io.IOException

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
        simConfig.stock = stock
        simConfig.incidents = incidents
        simConfig.customers = customers

        return simConfig
    }

    /** Schema-validates [filePath], runs [block] over JSON object, logging and recording file errors */
    private inline fun <T> processFile(filePath: String, schemaPath: String, block: (JsonObject) -> T): T? {
        // a file that cannot be read at all (missing, a directory, no permission) is an invalid
        // configuration file just like one that fails the schema, not a crash (item 20)
        val content = readConfigFile(filePath) ?: return null
        if (!isSchemaValid(content, filePath, schemaPath)) return null

        val json = Json.parseToJsonElement(content).jsonObject
        return try {
            block(json).also { InitialAndPrepLogger.logInitialization(true, filePath) }
        } catch (_: IllegalArgumentException) {
            markFileInvalid(filePath)
            null
        }
        // NoSuchElementException isn't caught, it means we should fix the getValue, not that the file is invalid
        // only IllegalArgumentException is thrown intentionally, catching other errors will hide problems in parsers
    }

    /** the file's contents, or `null` (logged as invalid) if it cannot be read */
    private fun readConfigFile(filePath: String): String? =
        try {
            File(filePath).readText()
        } catch (_: IOException) {
            markFileInvalid(filePath)
            null
        }

    /** logs [filePath] as an invalid configuration file and records it in the config */
    private fun markFileInvalid(filePath: String) {
        InitialAndPrepLogger.logInitialization(false, filePath)
        simConfig.wasInvalidFile = true
    }

    private fun isSchemaValid(content: String, filePath: String, schemaPath: String): Boolean {
        // malformed JSON is an invalid configuration file, not a crash either
        val jsonInstance = try {
            JsonParser(content).parse()
        } catch (_: JsonParseException) {
            markFileInvalid(filePath)
            return false
        }
        val schema = SchemaLoader.forURL(schemaPath).load()
        val isValid = Validator.forSchema(schema).validate(jsonInstance) == null
        if (!isValid) {
            markFileInvalid(filePath)
        }
        return isValid
    }
}
