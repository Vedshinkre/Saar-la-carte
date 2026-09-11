package de.unisaarland.cs.se.selab.parsers

import com.github.erosb.jsonsKema.*
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
        foodFilePath: String,
        restaurantsFilePath: String,
        scenarioFilePath: String
    ): SimulationConfig {
        validateFilesWithSchema(foodFilePath, "src/main/resources/schema/food.schema")
        validateFilesWithSchema(restaurantsFilePath, "classpath:///src/main/resources/schema/restaurants.schema")
        validateFilesWithSchema(scenarioFilePath, "src/main/resources/schema/scenario.schema")
        val foodStr = File(foodFilePath).readText()
        val foodObject = Json.parseToJsonElement(foodStr).jsonObject
        val ingredientArray = foodObject["ingredients"] as JsonArray
        val recipeArray = foodObject["recipe"] as JsonArray
        val restaurantsStr = File(restaurantsFilePath).readText()
        val restaurantsArray = Json.parseToJsonElement(restaurantsStr).jsonObject["restaurants"]!!.jsonArray
        val scenarioStr = File(scenarioFilePath).readText()
        val scenarioObject = Json.parseToJsonElement(scenarioStr).jsonObject
        val incidentJson = scenarioObject["incidents"]!!.jsonArray
        val customerJson = scenarioObject["customerGroups"]!!.jsonArray

        val foodData = foodParser.parse(ingredientArray, recipeArray)
        val stock = Stock(foodData!!.first)
        val restaurantData = restaurantParser.parseRestaurants(
            restaurantsArray,
            foodData.first,
            foodData.second,
            stock
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

        val simConfig = SimulationConfig(
            foodData = foodData,
            restaurantData = restaurantData,
            scenarioData
        )

        return simConfig
    }

/**    private fun crossValidateRestaurantFood(): Boolean {
     TODO("Not yet implemented")
     }

     private fun crossValidateFoodScenario(): Boolean {
     TODO("Not yet implemented")
     }

     private fun crossValidateRestaurantScenario(): Boolean {
     TODO("Not yet implemented")
     }

     private fun crossValidateIncidents(): Boolean {
     TODO("Not yet implemented")
     }

     private fun crossValidateCustomers(): Boolean {
     TODO("Not yet implemented")
     }

     private fun crossValidateRegularsToRestaurants(
     customerGroups: List<CustomerGroup>,
     restaurants: List<Restaurant>
     ): Boolean {
     TODO("Not yet implemented")
     }

     private fun getRestaurantbyId(id: Int): Boolean {
     TODO("Not yet implemented")
     }

     private fun crossValidateBasicDishforRestaurant(
     restaurants: List<Restaurant>
     ): Boolean {
     TODO("Not yet implemented")
     }

     private fun getRecipesByType(
     restaurantType: RestaurantType
     ): List<Recipe> {
     TODO("Not yet implemented")
     }

     private fun crossValidateDeliveries(): Boolean {
     TODO("Not yet implemented")
     }

     private fun getCustomerGroupsByDelivery(): List<CustomerGroup> {
     TODO("Not yet implemented")
     }

     private fun validateVisitingTick(): Boolean {
     TODO("Not yet implemented")
     }

     private fun crossvalidateIncidents(): Boolean {
     TODO("Not yet implemented")
     }

     private fun getIngredientbyName(name: String): Boolean {
     TODO("Not yet implemented")
     }
}**/
    private fun validateFilesWithSchema(filePath: String, schemaPath: String) {
        try {
            val jsonInstance = JsonParser(File(filePath).readText()).parse()
            val schema = SchemaLoader.forURL(schemaPath).load()
            val config = ValidatorConfig(FormatValidationPolicy.ALWAYS)
            val validator = Validator.create(schema, config)

            val failure = validator.validate(jsonInstance) ?: return
            // Logger.logInitialization(false, filePath)
            System.err.println(failure)
        } catch (e: IOException) {
            System.err.println("Could not read file: ${e.message}")
        } catch (e: JsonParseException) {
            System.err.println("Invalid JSON: ${e.message}")
        }
    }
    private fun crossvalidateIncidents(): Boolean {
        var incidents = simConfig.incidents
        var restaurants = simConfig.restaurants
        val seenIds = mutableSetOf<Int>()
        for (incident in incidents) {
            require(!seenIds.add(incident.getId()))
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
