package de.unisaarland.cs.se.selab.parsers

import com.github.erosb.jsonsKema.*
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.system.SimulationConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException

class ParserController {

    private var foodParser: FoodParser = FoodParser()
    private var restaurantParser: RestaurantParser = RestaurantParser()
    private var scenarioParser: ScenarioParser = ScenarioParser()
    private val simConfig: SimulationConfig = SimulationConfig()

    fun parseFiles(
        foodFilePath: String,
        restaurantFilePath: String,
        scenarioFilePath: String
    ): SimulationConfig {
        validateFilesWithSchema(foodFilePath, "src/main/resources/schema/food.schema")
        validateFilesWithSchema(restaurantFilePath, "classpath:///src/main/resources/schema/restaurants.schema")
        validateFilesWithSchema(scenarioFilePath, "src/main/resources/schema/scenario.schema")
        val foodStr = File(foodFilePath).readText()
        val foodObject = Json.parseToJsonElement(foodStr).jsonObject
        val ingredientJson = foodObject["ingredients"]!!.jsonArray
        val recipeJson = foodObject["recipe"]!!.jsonArray
        val restaurantStr = File(restaurantFilePath).readText()
        val restaurantObject = Json.parseToJsonElement(restaurantStr).jsonObject
        val scenarioStr = File(scenarioFilePath).readText()
        val scenarioObject = Json.parseToJsonElement(scenarioStr).jsonObject
        val incidentJson = scenarioObject["incidents"]!!.jsonArray
        val customerJson = scenarioObject["customerGroups"]!!.jsonArray

        val foodData = foodParser.parse(incidentJson, recipeJson)
        val restaurantData = restaurantParser.parse(restaurantObject)

        val scenarioData = scenarioParser.parseScenario(
            incidentJson,
            customerJson,
            restaurantData[1],
            foodData[1],
            foodData[0]
        )

        val simConfig = SimulationConfig(
            foodData = foodData,
            restaurantData = restaurantData,
            scenarioData
        )

        return simConfig
    }

    private fun crossValidateRestaurantFood(): Boolean {
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

    private fun validateStaffChangeIncidents(): Boolean {
        TODO("Not yet implemented")
    }

    private fun validatePackagingChangeIncidents(): Boolean {
        TODO("Not yet implemented")
    }

    private fun validateRecipeChangeIncidents(): Boolean {
        TODO("Not yet implemented")
    }

    private fun getIngredientbyName(name: String): Boolean {
        TODO("Not yet implemented")
    }
    private fun resolvePath(path: String): String {
        val file = File(path)
        return if (file.exists()) {
            path
        } else {
            // fallback to systemtest resources
            "src/systemtest/resources/$path"
        }
    }
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
}
