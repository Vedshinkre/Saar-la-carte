package de.unisaarland.cs.se.selab.parsers

import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.system.SimulationConfig
import java.io.File


class ParserController {

     private val foodParser: FoodParser = FoodParser()
     private val restaurantParser: RestaurantParser = RestaurantParser()
     private val scenarioParser: ScenarioParser = ScenarioParser()
     private val simConfig: SimulationConfig = SimulationConfig()

    fun parseFiles(
        foodFilePath: String,
        restaurantFilePath: String,
        scenarioFilePath: String
    ): SimulationConfig {

        foodParser = foodParser(resolvePath(foodFilePath))
        restaurantParser = restaurantParser(resolvePath(restaurantFilePath))
        scenarioParser = scenarioParser(resolvePath(scenarioFilePath))

        val foodData = foodParser.parseFood()
        val restaurantData = restaurantParser.parseRestaurants()
        val scenarioData = scenarioParser.parseScenario()

        simConfig = SimulationConfig(
            foodData,
            restaurantData,
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
}