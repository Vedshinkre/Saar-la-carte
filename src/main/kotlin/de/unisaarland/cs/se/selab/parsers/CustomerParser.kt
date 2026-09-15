package de.unisaarland.cs.se.selab.parsers

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.ceil

private const val DRIVER_SPEED = 5

/** Parses and validates customer groups. */
class CustomerParser {
    private fun <T> JsonArray.parseListOf(itemParser: (JsonElement) -> T): List<T> = this.map(itemParser)

    private fun stringToRestaurantType(restaurantType: String): RestaurantType {
        return when (restaurantType) {
            "EUROPEAN" -> RestaurantType.EUROPEAN
            "ASIAN" -> RestaurantType.ASIAN
            "AFRICAN" -> RestaurantType.AFRICAN
            "AMERICAN" -> RestaurantType.AMERICAN
            else -> throw IllegalArgumentException("RestaurantType \"$restaurantType\" does not exist.")
        }
    }

    /** Call with jsonArray containing CustomerGroups, full list of Recipes, Ingredients and RestaurantStats.
     * Returns list of CustomerGroups or throws exception. */
    @Throws
    fun parseCustomers(
        jsonArray: JsonArray,
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        restaurantStats: List<RestaurantStats>
    ): List<CustomerGroup> {
        val customerGroups: List<CustomerGroup> = jsonArray.parseListOf { customerGroup ->
            parseCustomer(
                customerGroup.jsonObject,
                recipes,
                ingredients,
                restaurantStats
            )
        }
        validateCustomerGroupUniquenessById(customerGroups)
        return jsonArray.parseListOf { customerGroup ->
            parseCustomer(
                customerGroup.jsonObject,
                recipes,
                ingredients,
                restaurantStats
            )
        }
    }

    private fun validateCustomerGroupUniquenessById(customerGroups: List<CustomerGroup>) =
        require(customerGroups.distinctBy { it.id }.size == customerGroups.size)

    @Throws
    private fun parseCustomer(
        jsonObject: JsonObject,
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        restaurantStats: List<RestaurantStats>
    ): CustomerGroup {
        val id: Id = jsonObject.getValue("id").jsonPrimitive.int
        val size: Int = jsonObject.getValue("size").jsonPrimitive.int
        val tableType: TableType = when (jsonObject["tableType"]?.jsonPrimitive?.content) {
            "COMMON" -> TableType.COMMON
            "BAR" -> TableType.BAR
            "SEPARATED" -> TableType.SEPARATED
            else -> TableType.COMMON
        }
        val visitingAt: Tick = jsonObject.getValue("visitingTick").jsonPrimitive.int
        val foodPreferences: List<FoodPreference> =
            FoodPreferenceParser().parseFoodPreferences(
                jsonObject.getValue("foodPreferences").jsonArray,
                recipes,
                ingredients,
                size
            )
        return when (val customerGroupType = jsonObject.getValue("type").jsonPrimitive.content) {
            "REGULAR" -> parseRegularGroup(
                jsonObject,
                restaurantStats,
                id,
                size,
                tableType,
                visitingAt,
                foodPreferences
            )
            "CASUAL" -> parseCasualGroup(jsonObject, id, size, tableType, visitingAt, foodPreferences)
            "EVENT" -> parseEventGroup(jsonObject, recipes, id, size, tableType, visitingAt, foodPreferences)
            else -> throw IllegalArgumentException("CustomerGroup type \"$customerGroupType\" does not exist.")
        }
    }

    @Throws
    private fun parseRegularGroup(
        jsonObject: JsonObject,
        restaurantStats: List<RestaurantStats>,
        id: Id,
        size: Int,
        tableType: TableType,
        visitingAt: Tick,
        foodPreferences: List<FoodPreference>
    ): RegularGroup {
        val visitingStart: Evening = jsonObject.getValue("visitingStart").jsonPrimitive.int
        val visitingPeriod: Tick = jsonObject.getValue("visitingPeriod").jsonPrimitive.int
        val restaurantId: Id = jsonObject.getValue("restaurant").jsonPrimitive.int

        require(validateRegularGroupRestaurantIdExistence(restaurantId, restaurantStats))
        return RegularGroup(
            id,
            size,
            tableType,
            visitingAt,
            foodPreferences,
            visitingStart,
            visitingPeriod,
            restaurantId
        )
    }

    private fun validateRegularGroupRestaurantIdExistence(
        restaurantId: Id,
        restaurantStats: List<RestaurantStats>
    ): Boolean {
        return restaurantStats.map { it.restaurantId }.contains(restaurantId)
    }

    @Throws
    private fun parseCasualGroup(
        jsonObject: JsonObject,
        id: Id,
        size: Int,
        tableType: TableType,
        visitingAt: Tick,
        foodPreferences: List<FoodPreference>
    ): CasualGroup {
        val restaurantTypes: List<RestaurantType> =
            jsonObject.getValue("restaurantTypes").jsonArray
                .parseListOf { stringToRestaurantType(it.jsonPrimitive.content) }
        val visitingEvenings: List<Evening> =
            jsonObject.getValue("visitingEvenings").jsonArray.parseListOf { it.jsonPrimitive.int }
        val deliveryDistance: Int = jsonObject["deliveryDistance"]?.jsonPrimitive?.int ?: 0
        if (deliveryDistance > 0) {
            require(visitingAt - (ceil((deliveryDistance / DRIVER_SPEED).toDouble()) + 3) > 0)
        }

        val ratingLikelihood: RatingLikelihood =
            when (val ratingLikelihood = jsonObject.getValue("ratingLikelihood").jsonPrimitive.content) {
                "NEVER" -> RatingLikelihood.NEVER
                "SOME" -> RatingLikelihood.SOME
                "ALWAYS" -> RatingLikelihood.ALWAYS
                else -> throw IllegalArgumentException("RatingLikelihood \"$ratingLikelihood\" does not exist")
            }
        return CasualGroup(
            id, size, tableType, visitingAt, foodPreferences, restaurantTypes,
            visitingEvenings, deliveryDistance, ratingLikelihood
        )
    }

    @Throws
    private fun parseEventGroup(
        jsonObject: JsonObject,
        recipes: List<Recipe>,
        id: Id,
        size: Int,
        tableType: TableType,
        visitingAt: Tick,
        foodPreferences: List<FoodPreference>
    ): EventGroup {
        val restaurantTypes: List<RestaurantType> =
            jsonObject.getValue("restaurantTypes").jsonArray
                .parseListOf { stringToRestaurantType(it.jsonPrimitive.content) }
        val eventEvening: Evening = jsonObject.getValue("eventEvening").jsonPrimitive.int
        val eventDishes: Map<RestaurantType, String> = Json.decodeFromJsonElement(jsonObject.getValue("favoriteDishes"))

        validateEventGroupFavoriteDishForRestaurantTypeExistence(restaurantTypes, eventDishes)
        validateEventGroupFavoriteDishExistence(recipes, eventDishes)
        return EventGroup(id, size, tableType, visitingAt, foodPreferences, restaurantTypes, eventEvening, eventDishes)
    }

    private fun validateEventGroupFavoriteDishForRestaurantTypeExistence(
        restaurantTypes: List<RestaurantType>,
        eventDishes: Map<RestaurantType, String>
    ) {
        restaurantTypes.forEach { require(eventDishes.containsKey(it)) }
    }

    private fun validateEventGroupFavoriteDishExistence(
        recipes: List<Recipe>,
        eventDishes: Map<RestaurantType, String>
    ) {
        require(recipes.map { it.name }.containsAll(eventDishes.values))
    }
}
