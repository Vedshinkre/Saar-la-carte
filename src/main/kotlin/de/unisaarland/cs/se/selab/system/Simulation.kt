package de.unisaarland.cs.se.selab.system

import customer.CustomerGroup
import incidents.Incident
import restaurant.BrowsingService
import restaurant.Restaurant

/**
 * Class that runs the simulation in the correct order of events
 */

class Simulation(simdata: SimulationConfig) {
    var restaurants: MutableList<Restaurant> = simdata.restaurants
    var browser: BrowsingService = BrowsingService(simdata.restaurantStats)
    var incidents: MutableList<Incident> = simdata.incidents
    var customers: MutableList<CustomerGroup> = simdata.customers
}

/**
 * The function that runs the simulation, gets called from main
 */
fun runSimulation() { "weee" }
