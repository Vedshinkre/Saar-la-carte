package de.unisaarland.cs.se.selab.parsers

/**
 * Skeleton implementation for IncidentParser.
 * Handles validation and parsing of incident objects from scenario configuration files.
 */
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.StaffType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.incidents.PackagingChangeIncident
import de.unisaarland.cs.se.selab.incidents.RecipeChangeIncident
import de.unisaarland.cs.se.selab.incidents.StaffChangeIncident
import de.unisaarland.cs.se.selab.incidents.UnavailabilityIncident
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import kotlinx.serialization.json.*

class IncidentParser {

    fun parseIncidentFile(
        incidentArray: JsonArray,
        ingredients: List<Ingredient>,
        restaurants: List<Restaurant>
    ): List<Incident> {
        return incidentArray.map { element ->
            try {
                parseIncident(element.jsonObject, ingredients, restaurants)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid incident: $element", e)
            }
        }
    }

    private fun parseIncident(
        json: JsonObject,
        ingredients: List<Ingredient>,
        restaurants: List<Restaurant>
    ): Incident {
        val id = json.requiredInt("id")
        val evening = json.requiredInt("evening")
        return when (json.requiredString("type")) {
            "STAFF" -> {
                val staffType =
                    json.requiredEnum<StaffType>("staffType")

                StaffChangeIncident(
                    id = id,
                    evening = evening,
                    restaurantId = json.requiredInt("restaurant"),
                    number = json.requiredInt("number"),
                    staffType = staffType,
                    cookType =
                    (
                        if (CookType!! == StaffType.COOK) {
                            json.requiredEnum<CookType>("cookType")
                        } else {
                            null
                        }
                        )!!
                )
            }

            "RECIPE" -> RecipeChangeIncident(
                id = id,
                evening = evening,
                ingredient = json.requiredString("ingredient"),
                adaptation = json.requiredInt("adaptation")
            )

            "PACKAGING" -> PackagingChangeIncident(
                id = id,
                evening = evening,
                ingredient = json.requiredString("ingredient"),
                packagingVolume = json.requiredDouble("packagingVolume")
            )

            "UNAVAILABLE" -> UnavailabilityIncident(
                id = id,
                evening = evening,
                ingredient = json.requiredString("ingredient"),
                duration = json.requiredInt("duration")
            )

            else -> error("Unknown incident type")
        }
    }
    private fun JsonObject.requiredString(key: String): String =
        this[key]?.jsonPrimitive?.content
            ?: error("Missing string property: $key")

    private fun JsonObject.requiredInt(key: String): Int =
        this[key]?.jsonPrimitive?.int
            ?: error("Missing integer property: $key")

    private fun JsonObject.requiredDouble(key: String): Double =
        this[key]?.jsonPrimitive?.double
            ?: error("Missing decimal property: $key")

    private inline fun <reified T : Enum<T>> JsonObject.requiredEnum(key: String): T =
        enumValueOf(requiredString(key))
}
