package de.unisaarland.cs.se.selab.system

import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats

/**
 * Class that contains the data of the whole simulation
 * */

class SimulationConfig(
    foodData: Pair<List<Ingredient>, List<Recipe>>,
    restaurantData: Pair<List<RestaurantStats>, List<Restaurant>>,
    scenarioData: Pair<List<Incident>, List<CustomerGroup>>
) {

    var restaurants: MutableList<Restaurant> = restaurantData.second.toMutableList()
    var customers: MutableList<CustomerGroup> = scenarioData.second.toMutableList()
    var incidents: MutableList<Incident> = scenarioData.first.toMutableList()
    var ingredients: MutableList<Ingredient> = foodData.first.toMutableList()
    var recipes: MutableList<Recipe> = foodData.second.toMutableList()
    var restaurantStats: MutableList<RestaurantStats> = restaurantData.first.toMutableList()
}
