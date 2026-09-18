package de.unisaarland.cs.se.selab.system

import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats

/**
 * Class that contains the data of the whole simulation
 * */

class SimulationConfig {

    var restaurants: List<Restaurant> = listOf()
    var customers: List<CustomerGroup> = listOf()
    var incidents: List<Incident> = listOf()
    var ingredients: List<Ingredient> = listOf()
    var recipes = listOf<Recipe>()
    var restaurantStats: List<RestaurantStats> = listOf()
    var stock: Stock = Stock(emptyList())
    var wasInvalidFile = false
}
