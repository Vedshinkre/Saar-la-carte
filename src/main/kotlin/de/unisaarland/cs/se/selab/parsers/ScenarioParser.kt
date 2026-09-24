package de.unisaarland.cs.se.selab.parsers

import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.restaurant.Restaurant

/**
 * High-level coordinator delegating to IncidentParser and CustomerParser.
 *
 * @property incidentParser parses the scenario's `incidents` array
 * @property customerParser parses the scenario's `customerGroups` array
 */
class ScenarioParser(
    private val incidentParser: IncidentParser = IncidentParser(),
    private val customerParser: CustomerParser = CustomerParser()
) {

    /**
     * Reads and parses a scenario file's incidents and customer groups.
     *
     * @param incident the raw `incidents` JSON array from the scenario file
     * @param customer the raw `customerGroups` JSON array from the scenario file
     * @param restaurants all parsed restaurants, used to resolve incidents and seat customer groups
     * @param recipes the global recipes, used to resolve incidents and customer food preferences
     * @param ingredients all known ingredients, used to resolve incidents by ingredient name
     * @param stock the restaurant's stock, wired into unavailability incidents
     * @return the parsed incidents and the parsed customer groups
     */
    fun parseScenario(
        incident: kotlinx.serialization.json.JsonArray,
        customer: kotlinx.serialization.json.JsonArray,
        restaurants: List<Restaurant>,
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        stock: Stock
    ): Pair<List<Incident>, List<CustomerGroup>> {
        val incidents = incidentParser.parseIncidentFile(incident, ingredients, stock, recipes, restaurants,)
        val restaurantStats = restaurants.map { it.getRestaurantStats() }
        val customerGroups = customerParser.parseCustomers(customer, recipes, ingredients, restaurantStats,)

        return Pair(incidents, customerGroups)
    }
}
