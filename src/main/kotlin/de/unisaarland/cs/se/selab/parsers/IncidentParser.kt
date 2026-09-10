package de.saar.la.carte.parsers

import com.github.erosb.jsonsKema.FormatValidationPolicy
import com.github.erosb.jsonsKema.JsonParser
import com.github.erosb.jsonsKema.JsonValue
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.ValidationFailure
import com.github.erosb.jsonsKema.Validator
import com.github.erosb.jsonsKema.ValidatorConfig
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter

/**
 * Skeleton implementation for IncidentParser.
 * Handles validation and parsing of incident objects from scenario configuration files.
 */
class IncidentParser {
   fun  parseIncidentFile(filePath: String, stock: Stock, restaurants: List<Restaurant>): List<Incident> {#
           val objectFilePath = File(filePath).readText()
           val schema = SchemaLoader.forURL("src/main/resources/schema/incident.schema").load()
           val schemaValidator = Validator.create(
               schema,
               ValidatorConfig(FormatValidationPolicy.ALWAYS)
           )
           val jsonInstance: JsonValue = JsonParser(objectFilePath).parse()
           val failure: ValidationFailure? = schemaValidator.validate(jsonInstance)
           if (failure != null) {
               PrintWriter(System.err, true).println("Schema validation failed for file: $filePath")
               //simConfig : IsValid set to false
               Logger.logInitialization(false, filePath)
           }




       val scenarioJson = JSONObject(objectFilePath)
       val incidents = scenarioJson.getJSONArray(JsonFields.INCIDENTS).map{
           parseIncident(
               it as JSONObject,
               restaurants,
               stock
           )
       }
       val validated_incidents = incidents.map {validateIncident(it)}
       checkUniquenessOfIncident(validated_incidents)
       checkDurationOverLapOfExistingIncidents(validated_incidents)


       return validated_incidents
   }
    private fun parseIncident(jsonObject: JSONObject, restaurants: List<Restaurant>,  stock: Stock) : Incident{

    }
    private fun validateIncident(incident: Incident) : Incident {

    }
    private fun checkUniquenessOfIncident(incidents : List<Incident>): Boolean{

    }
    private fun checkDurationOverLapOfExistingIncidents(incidents :List<Incident>): Boolean{

    }



}
