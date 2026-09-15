package de.unisaarland.cs.se.selab.parsers

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.StaffType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.incidents.PackagingChangeIncident
import de.unisaarland.cs.se.selab.incidents.RecipeChangeIncident
import de.unisaarland.cs.se.selab.incidents.StaffChangeIncident
import de.unisaarland.cs.se.selab.incidents.UnavailabilityIncident
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val ID = "id"
private const val EVENING = "evening"
private const val TYPE = "type"
private const val INGREDIENT = "ingredient"
private const val ADAPTATION = "adaptation"
private const val PACKAGING_VOLUME = "packagingVolume"
private const val DURATION = "duration"
private const val STAFF_TYPE = "staffType"
private const val RESTAURANT_ID = "restaurant"
private const val COOK_TYPE = "cookType"
private const val NUMBER = "number"

/**
 * sake of detekt
 */
class IncidentParser {
    /**
     * sake of detect
     */
    fun parseIncidentFile(
        incidentArray: JsonArray,
        ingredients: List<Ingredient>,
        stock: Stock,
        recipes: List<Recipe>,
        restaurants: List<Restaurant>
    ): List<Incident> {
        return incidentArray.map { element ->
            val json = element as? JsonObject ?: throw IllegalArgumentException(
                "Invalid incident: expected a JSON object, got: $element"
            )

            try {
                parseIncident(
                    json = json,
                    restaurants = restaurants,
                    ingredients = ingredients,
                    stock = stock,
                    recipes = recipes
                )
            } catch (exception: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid incident: $element",
                    exception
                )
            }
        }
    }

    private fun parseIncident(
        json: JsonObject,
        restaurants: List<Restaurant>,
        ingredients: List<Ingredient>,
        stock: Stock,
        recipes: List<Recipe>
    ): Incident {
        val id = json.requiredInt(ID)
        val evening = json.requiredInt(EVENING)
        return when (val type = json.requiredString(TYPE)) {
            "STAFF" -> parseStaffIncident(
                json = json,
                id = id,
                evening = evening,
                restaurants = restaurants
            )

            "RECIPE" -> parseRecipeChangeIncident(
                id = id,
                evening = evening,
                ingredientName = json.requiredString(INGREDIENT),
                adaptation = json.requiredInt(ADAPTATION),
                ingredients = ingredients,
                recipes = recipes
            )

            "PACKAGING" -> parsePackagingChangeIncident(
                id = id,
                evening = evening,
                ingredientName = json.requiredString(INGREDIENT),
                ingredients = ingredients,
                packagingVolume = json.requiredInt(PACKAGING_VOLUME)
            )

            "UNAVAILABLE" -> parseUnavailabilityIncident(
                id = id,
                evening = evening,
                ingredientName = json.requiredString(INGREDIENT),
                ingredients = ingredients,
                duration = json.requiredInt(DURATION),
                stock = stock
            )

            else -> throw IllegalArgumentException(
                "Unknown incident type: $type"
            )
        }
    }

    private fun parseUnavailabilityIncident(
        id: Int,
        evening: Int,
        ingredientName: String,
        ingredients: List<Ingredient>,
        duration: Int,
        stock: Stock
    ): UnavailabilityIncident {
        val ingredient = ingredients.requiredIngredient(ingredientName)

        return UnavailabilityIncident(
            id = id,
            evening = evening,
            ingredient = ingredient,
            stock = stock,
            duration = duration
        )
    }

    private fun parsePackagingChangeIncident(
        id: Int,
        evening: Int,
        ingredientName: String,
        ingredients: List<Ingredient>,
        packagingVolume: Int
    ): PackagingChangeIncident {
        val ingredient = ingredients.requiredIngredient(ingredientName)

        return PackagingChangeIncident(
            id = id,
            evening = evening,
            ingredient = ingredient,
            packagingVolume = packagingVolume
        )
    }

    private fun parseRecipeChangeIncident(
        id: Int,
        evening: Int,
        ingredientName: String,
        adaptation: Int,
        ingredients: List<Ingredient>,
        recipes: List<Recipe>
    ): RecipeChangeIncident {
        val ingredient = ingredients.requiredIngredient(ingredientName)

        return RecipeChangeIncident(
            id = id,
            evening = evening,
            ingredient = ingredient,
            adaptation = adaptation,
            recipes = recipes
        )
    }

    private fun parseStaffIncident(
        json: JsonObject,
        id: Int,
        evening: Int,
        restaurants: List<Restaurant>
    ): StaffChangeIncident {
        val staffType = json.requiredEnum<StaffType>(STAFF_TYPE)
        val restaurantId = json.requiredInt(RESTAURANT_ID)

        val restaurant = restaurants.firstOrNull { currentRestaurant ->
            currentRestaurant.getRestaurantStats().restaurantId == restaurantId
        } ?: throw IllegalArgumentException(
            "Restaurant with ID '$restaurantId' does not exist."
        )

        val cookType: CookType? = if (staffType == StaffType.COOK) {
            json.requiredEnum<CookType>(COOK_TYPE)
        } else {
            null
        }

        return StaffChangeIncident(
            id = id,
            evening = evening,
            number = json.requiredInt(NUMBER),
            staffType = staffType,
            cookType = cookType, /*if not cook this will be null*/
            restaurantStaff = restaurant.getRestaurantStaff()
        )
    }

    private fun List<Ingredient>.requiredIngredient(
        ingredientName: String
    ): Ingredient {
        return firstOrNull { ingredient ->
            ingredient.name == ingredientName
        } ?: throw IllegalArgumentException(
            "Ingredient '$ingredientName' does not exist."
        )
    }

    private fun JsonObject.requiredString(key: String): String {
        return this[key]?.jsonPrimitive?.content ?: throw IllegalArgumentException(
            "Missing string property: $key"
        )
    }

    private fun JsonObject.requiredInt(key: String): Int {
        return this[key]?.jsonPrimitive?.content?.toIntOrNull() ?: throw IllegalArgumentException(
            "Missing or invalid integer property: $key"
        )
    }

    private inline fun <reified T : Enum<T>> JsonObject.requiredEnum(
        key: String
    ): T {
        val value = requiredString(key)

        return try {
            enumValueOf<T>(value)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid value '$value' for enum property '$key'.",
                exception
            )
        }
    }
}
