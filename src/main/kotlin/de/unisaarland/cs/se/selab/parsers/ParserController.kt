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
import java.io.PrintWriter


class ParserController {

    // private var foodParser: FoodParser = FoodParser()
    // private var restaurantParser: RestaurantParser = RestaurantParser()
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
        val scenarioStr = File(scenarioFilePath).readText()
        val scenarioObject = Json.parseToJsonElement(scenarioStr).jsonObject
        val incidentJson = scenarioObject["incidents"]!!.jsonArray
        val customerJson = scenarioObject["customerGroups"]!!.jsonArray

        // foodParser = foodParser(resolvePath(foodFilePath))
        // restaurantParser = restaurantParser(resolvePath(restaurantFilePath))
        // scenarioParser = scenarioParser(File(scenarioFilePath).readText())

        // val foodData = foodParser.parseFood()
        // val restaurantData = restaurantParser.parseRestaurants()

        val scenarioData = scenarioParser.parseScenario(incidentJson, customerJson)

        val simConfig = SimulationConfig(
            0,
            0,
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
            val jsonString = File(filePath).readText()

            val schema = SchemaLoader.forURL(schemaPath).load()

            val schemaValidator = Validator.create(
                schema,
                ValidatorConfig(FormatValidationPolicy.ALWAYS)
            )
            val jsonInstance: JsonValue = JsonParser(jsonString).parse()
            val failure: ValidationFailure? = schemaValidator.validate(jsonInstance)

            if (failure != null) {
                // simData.isValid = false
                // Logger.logInitialization(false, filePath)
            }
        } catch (e: IOException) {
            PrintWriter(System.err, true).println(e.message)
        } catch (e: JsonParseException) {
            PrintWriter(System.err, true).println(e.message)
        }
    }
}
