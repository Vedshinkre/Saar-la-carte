package de.saar.la.carte.parsers

import org.json.JSONArray
import org.json.JSONObject

/**
 * Skeleton implementation for IncidentParser.
 * Handles validation and parsing of incident objects from scenario configuration files.
 */
class IncidentParser {
   fun  parseIncidentFile(filePath: String, stock: Stock, restaurants: List<Restaurant>): List<Incident> {
       objectFilePath = File(filePath).readText()

       val scenarioJson = JSONObject(filePath)
       val parsedIncidents = scenarioJson.getJSONArray(JsonFields.INCIDENTS)
       for pi in parsedIncidents {

       }
   }
    private fun parseIncident(jsonObject: JSONObject, restaurants: List<Restaurant>, recipes: List<Recipe>, stock: Stock) : Incident{

    }
    private fun validateIncident(incident: Incident) : Boolean{

    }
    private fun checkUniquenessOfIncident(id: Int): Boolean{

    }
    private fun checkDurationOverLapOfExistingIncidents(duration: Int, evening: Int, ingredient: String): Boolean{

    }



}
