package de.unisaarland.cs.se.selab.system

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.Restaurant

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
fun runSimulation() {

}

private fun simulateEvening() {}

private fun executeIncidents() {}

private fun executePreparationPhase() {}

private fun executeServingPhase() {}

private fun executeSingleTick(List<CasualGroup>) {}

private fun getRestaurantById(id: Id) : Restaurant {
}

