package de.unisaarland.cs.se.selab.parsers

import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Skeleton implementation for IncidentParser.
 * Handles validation and parsing of incident objects from scenario configuration files.
 */
class IncidentParser {
    fun parseIncidentFile(incidentArrays: JsonArray): IntArray {

     println(incidentArrays)
        return intArrayOf()
    }
    private fun parseIncident(jsonObject: JsonObject, restaurants: List<Restaurant>, stock: Stock): Unit {
        return
    }
    private fun validateIncident(incident: Incident):Unit {
        return
    }
    private fun checkUniquenessOfIncident(incidents: List<Incident>): Boolean {
        return true
    }
    private fun checkDurationOverLapOfExistingIncidents(incidents: List<Incident>): Boolean {
        return true
    }
}
