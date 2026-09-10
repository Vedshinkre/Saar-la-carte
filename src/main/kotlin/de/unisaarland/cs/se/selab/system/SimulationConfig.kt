package de.unisaarland.cs.se.selab.system

import restaurant.Restaurant
import restaurant.RestaurantStats
import customer.CustomerGroup
import incidents.Incident
import food.Ingredient
import food.Recipe

/**
 * Class that contains the data of the whole simulation
 * */

class SimulationConfig() {
    var restaurants : MutableList<Restaurant> = mutableListOf<Restaurant>()
    var customers: MutableList<CustomerGroup> = mutableListOf<CustomerGroup>()
    var incidents: MutableList<Incident> = mutableListOf<Incident>()
    var ingredients: MutableList<Ingredient> = mutableListOf<Ingredient>()
    var recipes: MutableList<Recipe> = mutableListOf<Recipe>()
    var restaurantStats: MutableList<RestaurantStats> = mutableListOf<RestaurantStats>()

}