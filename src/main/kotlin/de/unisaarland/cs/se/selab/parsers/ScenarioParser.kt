package de.unisaarland.cs.se.selab.parsers

import com.github.erosb.jsonsKema.JsonArray
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.restaurant.Restaurant

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
    fun parseScenario(incident: JsonArray, customer: JsonArray, restaurants: List<Restaurant>, recipes: List<Recipe>, ingredients:List<Ingredient>): Pair<List<Incident>, List<CustomerGroup>> {

        incidentParser.parseIncidentFile(incident, stock, restaurants)
        customerParser.parseCustomerFile(filePath)

        return Pair(incidents, customerGroups)
    }
}
