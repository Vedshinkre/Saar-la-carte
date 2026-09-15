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

class SimulationConfig {

    var restaurants: List<Restaurant> = listOf<Restaurant>()
    var customers: List<CustomerGroup> = listOf<CustomerGroup>()
    var incidents: List<Incident> = listOf<Incident>()
    var ingredients: List<Ingredient> = listOf<Ingredient>()
    var recipes: List<Recipe> = listOf<Recipe>()
    var restaurantStats: List<RestaurantStats> = listOf<RestaurantStats>()
}
