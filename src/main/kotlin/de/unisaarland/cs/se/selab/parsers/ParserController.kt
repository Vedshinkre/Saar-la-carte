package de.unisaarland.cs.se.selab.parsers

import com.github.erosb.jsonsKema.FormatValidationPolicy
import com.github.erosb.jsonsKema.JsonParseException
import com.github.erosb.jsonsKema.JsonParser
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.Validator
import com.github.erosb.jsonsKema.ValidatorConfig
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.incidents.UnavailabilityIncident
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.system.SimulationConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException

/**
 * delegates objects to other parsers
 */
class ParserController {

    private val foodParser: FoodParser = FoodParser()
    private val restaurantParser: RestaurantParser = RestaurantParser()
    private val scenarioParser: ScenarioParser = ScenarioParser()
    private val simConfig: SimulationConfig = SimulationConfig()

    /**
     * delagtes JsonObjects to muliple Parsers and Validates File with JsonSkema
     **/
    fun parseFiles(
        foodFilePath: String,
        restaurantsFilePath: String,
        scenarioFilePath: String,
    ): SimulationConfig {
        validateFilesWithSchema(foodFilePath, "classpath:/schema/food.schema")
        validateFilesWithSchema(restaurantsFilePath, "classpath:/schema/restaurants.schema")
        validateFilesWithSchema(scenarioFilePath, "classpath:/schema/scenario.schema")
        val jsonSources = readJsonSources(foodFilePath, restaurantsFilePath, scenarioFilePath)

        val foodData = parseFoodData(foodFilePath, jsonSources.ingredientArray, jsonSources.recipeArray)
            ?: return simConfig
        val stock = Stock(foodData.first)

        val restaurantData = parseRestaurantData(
            restaurantsFilePath, jsonSources.restaurantsArray, foodData.second, stock
        ) ?: return simConfig

        val scenarioData = parseScenarioData(
            scenarioFilePath,
            jsonSources.incidentJson,
            jsonSources.customerJson,
            restaurantData.second,
            foodData.second,
            foodData.first,
            stock,
        ) ?: return simConfig

        if (!crossvalidateScenario(scenarioFilePath)) return simConfig
        InitialAndPrepLogger.logInitialization(true, scenarioFilePath)

        simConfig.restaurants = restaurantData.second
        simConfig.ingredients = foodData.first
        simConfig.recipes = foodData.second
        simConfig.restaurantStats = restaurantData.first
        simConfig.incidents = scenarioData.first
        simConfig.customers = scenarioData.second
        return simConfig
    }

    private data class JsonSources(
        val ingredientArray: JsonArray,
        val recipeArray: JsonArray,
        val restaurantsArray: JsonArray?,
        val incidentJson: JsonArray?,
        val customerJson: JsonArray?,
    )

    private fun readJsonSources(
        foodFilePath: String,
        restaurantsFilePath: String,
        scenarioFilePath: String,
    ): JsonSources {
        val foodStr = File(foodFilePath).readText()
        val foodObject = Json.parseToJsonElement(foodStr).jsonObject
        val ingredientArray = (foodObject["ingredients"] ?: error("null assertion message")) as JsonArray
        val recipeArray = (foodObject["recipes"] ?: error("null assertion message")).jsonArray
        val restaurantsStr = File(restaurantsFilePath).readText()
        val restaurantsArray = Json.parseToJsonElement(restaurantsStr).jsonObject["restaurants"]?.jsonArray
        val scenarioStr = File(scenarioFilePath).readText()
        val scenarioObject = Json.parseToJsonElement(scenarioStr).jsonObject
        val incidentJson = scenarioObject["incidents"]?.jsonArray
        val customerJson = scenarioObject["customerGroups"]?.jsonArray
        return JsonSources(ingredientArray, recipeArray, restaurantsArray, incidentJson, customerJson)
    }

    private fun parseFoodData(
        foodFilePath: String,
        ingredientArray: JsonArray,
        recipeArray: JsonArray,
    ): Pair<List<Ingredient>, List<Recipe>>? {
        val foodData = try {
            foodParser.parse(ingredientArray, recipeArray)
        } catch (e: IllegalArgumentException) {
            InitialAndPrepLogger.logInitialization(false, foodFilePath)
            System.err.println("Invalid food data in '$foodFilePath': ${e.message ?: "Unknown error"}")
            simConfig.wasInvalidFile = true
            return null
        }
        InitialAndPrepLogger.logInitialization(true, foodFilePath)
        return foodData
    }

    private fun parseRestaurantData(
        restaurantsFilePath: String,
        restaurantsArray: JsonArray?,
        recipes: List<Recipe>,
        stock: Stock,
    ): Pair<List<RestaurantStats>, List<Restaurant>>? {
        requireNotNull(restaurantsArray) { "Restaurant JSON is invalid" }
        val restaurantData = try {
            restaurantParser.parseRestaurants(
                restaurantsArray,
                recipes,
                stock,
            )
        } catch (e: IllegalArgumentException) {
            InitialAndPrepLogger.logInitialization(false, restaurantsFilePath)
            System.err.println("Invalid Restaurant data in '$restaurantsFilePath: ${e.message ?: "Unknown error"}")
            simConfig.wasInvalidFile = true
            return null
        } catch (e: NoSuchElementException) {
            InitialAndPrepLogger.logInitialization(false, restaurantsFilePath)
            System.err.println("Invalid Restaurant data in '$restaurantsFilePath': ${e.message ?: "Unknown error"}")
            simConfig.wasInvalidFile = true
            return null
        }
        InitialAndPrepLogger.logInitialization(true, restaurantsFilePath)
        return restaurantData
    }

    private fun parseScenarioData(
        scenarioFilePath: String,
        incidentJson: JsonArray?,
        customerJson: JsonArray?,
        restaurants: List<Restaurant>,
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        stock: Stock,
    ): Pair<List<Incident>, List<CustomerGroup>>? {
        requireNotNull(incidentJson)
        requireNotNull(customerJson)
        return try {
            scenarioParser.parseScenario(
                incidentJson,
                customerJson,
                restaurants,
                recipes,
                ingredients,
                stock,
            )
        } catch (e: NoSuchElementException) {
            InitialAndPrepLogger.logInitialization(false, scenarioFilePath)
            System.err.println("Invalid Scenario data in '$scenarioFilePath': ${e.message ?: "Unknown error"}")
            simConfig.wasInvalidFile = true
            null
        } catch (e: IllegalArgumentException) {
            InitialAndPrepLogger.logInitialization(false, scenarioFilePath)
            System.err.println("Invalid Scenario data in '$scenarioFilePath': ${e.message ?: "Unknown error"}")
            simConfig.wasInvalidFile = true
            null
        }
    }

    private fun crossvalidateScenario(scenarioFilePath: String): Boolean {
        return try {
            crossvalidateIncidents()
            true
        } catch (e: IllegalArgumentException) {
            InitialAndPrepLogger.logInitialization(false, scenarioFilePath)
            System.err.println("Invalid Scenario data in '$scenarioFilePath:${e.message ?: "Unknown error"}'")
            simConfig.wasInvalidFile = true
            false
        }
    }

    private fun validateFilesWithSchema(filePath: String, schemaPath: String) {
        try {
            val jsonInstance = JsonParser(File(filePath).readText()).parse()
            val schema = SchemaLoader.forURL(schemaPath).load()
            val config = ValidatorConfig(FormatValidationPolicy.ALWAYS)
            val validator = Validator.create(schema, config)

            val failure = validator.validate(jsonInstance) ?: return // Logger.logInitialization(false, filePath)
            System.err.println(failure)
        } catch (e: IOException) {
            System.err.println("Could not read file: ${e.message ?: "Unknown error"}")
        } catch (e: JsonParseException) {
            System.err.println("Invalid JSON: ${e.message}")
        }
    }

    private fun crossvalidateIncidents(): Boolean {
        val incidents = simConfig.incidents
        val restaurants = simConfig.restaurants
        val seenIds = mutableSetOf<Int>()
        for (incident in incidents) {
            require(!seenIds.add(incident.id))
        }
        validateNoOverlappingUnavailability(incidents)
        return true
    }

    private fun validateNoOverlappingUnavailability(incidents: List<Incident>) {
        val unavailabilities = incidents.filterIsInstance<UnavailabilityIncident>()

        for (i in unavailabilities.indices) {
            for (j in i + 1 until unavailabilities.size) {
                require(!unavailabilities[i].overlapsWith(unavailabilities[j])) {
                    unavailabilities[i].conflictMessage(unavailabilities[j])
                }
            }
        }
    }
}
