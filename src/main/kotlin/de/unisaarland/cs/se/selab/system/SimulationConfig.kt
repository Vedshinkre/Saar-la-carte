package de.unisaarland.cs.se.selab.system

import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe

/**
 * Class that contains the data of the whole simulation
 * */

class SimulationConfig() {
    constructor(foodData: Any, restaurantData: Any, scenarioData: Any) : this()

    var restaurants : MutableList<Restaurant> = mutableListOf<Restaurant>()
    var customers: MutableList<CustomerGroup> = mutableListOf<CustomerGroup>()
    var incidents: MutableList<Incident> = mutableListOf<Incident>()
    var ingredients: MutableList<Ingredient> = mutableListOf<Ingredient>()
    var recipes: MutableList<Recipe> = mutableListOf<Recipe>()
    var restaurantStats: MutableList<RestaurantStats> = mutableListOf<RestaurantStats>()

}