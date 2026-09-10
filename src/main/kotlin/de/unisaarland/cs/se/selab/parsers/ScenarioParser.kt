package de.saar.la.carte.parsers

import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import org.json.JSONObject
import java.io.File

/**
 * Skeleton implementation for ScenarioParser.
 * High-level coordinator delegating to IncidentParser and CustomerParser.
 */
class ScenarioParser(
    private val incidentParser: IncidentParser = IncidentParser(),
    private val customerParser: CustomerParser = CustomerParser()
) {

    /**
     * Reads and parses a scenario JSON file, extracting incidents and customer groups.
     *
     * @param filePath Path to the scenario configuration JSON file.
     * @return Pair containing list of parsed Incidents and list of parsed CustomerGroups,
     *         or null if parsing/validation fails.
     */
    fun parseScenarioFile(filePath: String, restaurants: List<Restaurant>, recipes: List<Recipe>, stock: Stock): Pair<List<Incident>, List<CustomerGroup>> {

        incidentParser.parseIncidentFile(filePath: String, stock: Stock, restaurants: List<Restaurant>)
        customerParser.parseCustomerFile(filePath: String)

        return Pair(incidents, customerGroups)
    }
}
