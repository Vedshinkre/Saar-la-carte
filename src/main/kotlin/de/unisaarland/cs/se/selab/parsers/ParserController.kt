package de.unisaarland.cs.se.selab.parsers

import com.github.erosb.jsonsKema.FormatValidationPolicy
import com.github.erosb.jsonsKema.JsonParseException
import com.github.erosb.jsonsKema.JsonParser
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.Validator
import com.github.erosb.jsonsKema.ValidatorConfig
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.incidents.UnavailabilityIncident
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

    private var foodParser: FoodParser = FoodParser()
    private var restaurantParser: RestaurantParser = RestaurantParser()
    private var scenarioParser: ScenarioParser = ScenarioParser()
    private val simConfig: SimulationConfig = SimulationConfig()

    /**
     * delagtes JsonObjects to muliple Parsers and Validates File with JsonSkema
     **/
    fun parseFiles(
        foodFilePath: String, restaurantsFilePath: String, scenarioFilePath: String
    ): SimulationConfig {
        validateFilesWithSchema(foodFilePath, "classpath:/schema/food.schema")
        validateFilesWithSchema(restaurantsFilePath, "classpath:/schema/restaurants.schema")
        validateFilesWithSchema(scenarioFilePath, "classpath:/schema/scenario.schema")
        val foodStr = File(foodFilePath).readText()
        val foodObject = Json.parseToJsonElement(foodStr).jsonObject
        val ingredientArray = (foodObject["ingredients"] ?: error("null assertion message")) as JsonArray
        val recipeArray = (foodObject["recipes"] ?: error("null assertion message"))
        val restaurantsStr = File(restaurantsFilePath).readText()
        val restaurantsArray = Json.parseToJsonElement(restaurantsStr).jsonObject["restaurants"]?.jsonArray
        val scenarioStr = File(scenarioFilePath).readText()
        val scenarioObject = Json.parseToJsonElement(scenarioStr).jsonObject
        val incidentJson = scenarioObject["incidents"]?.jsonArray
        val customerJson = scenarioObject["customerGroups"]?.jsonArray

        val foodData = foodParser.parse(ingredientArray, recipeArray)
        val stock = Stock(foodData.first)
        val restaurantData = restaurantParser.parseRestaurants(
            restaurantsArray,
            foodData.second,
            stock,
        )

        val scenarioData = scenarioParser.parseScenario(
            incidentJson,
            customerJson,
            restaurantData.second,
            foodData.second,
            foodData.first,
            stock,
        )
        crossvalidateIncidents()

        simConfig.restaurants = restaurantData.second
        simConfig.ingredients = foodData.first
        simConfig.recipes = foodData.second
        simConfig.restaurantStats = restaurantData.first
        simConfig.incidents = scenarioData.first
        simConfig.customers = scenarioData.second

        return simConfig
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
