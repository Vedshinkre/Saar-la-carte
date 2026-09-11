package de.unisaarland.cs.se.selab.parsers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import kotlinx.serialization.json.*

// TODO: make recipe id public
// TODO: either throw IllegalArgumentException with require() and catch or return a bool

class RestaurantParser {
    private var recipes: List<Recipe> = emptyList()
    private val restaurantIds = mutableSetOf<Id>()
    private val restaurantNames = mutableSetOf<String>()
    private val tableIds = mutableSetOf<Id>()
    private val parsedStats = mutableListOf<RestaurantStats>()

    fun parseRestaurants(
        restaurantArray: JsonArray,
        ingredients: List<Ingredient>,
        recipes: List<Recipe>,
        stock: Stock
    ): Pair<List<RestaurantStats>, List<Restaurant>> {
        require(restaurantArray.isNotEmpty()) { "Restaurant file must contain at least one restaurant" }

        this.recipes = recipes // not mutable
        restaurantIds.clear()
        restaurantNames.clear()
        tableIds.clear()
        parsedStats.clear()

        val parsedRestaurants = restaurantArray.map { element ->
            parseRestaurant(element.jsonObject, ingredients, recipes, stock)
        }

        return Pair(parsedStats.toList(), parsedRestaurants)
    }

    fun getRecipes(): List<Recipe> {
        return recipes
    }

    private fun parseRestaurant(
        jsonObject: JsonObject,
        ingredients: List<Ingredient>,
        recipes: List<Recipe>,
        stock: Stock
    ): Restaurant {
        val id = jsonObject.getValue("id").jsonPrimitive.int
        val name = jsonObject.getValue("name").jsonPrimitive.content
        require(id >= 0) { "Restaurant ID must be non-negative" }
        require(name.isNotEmpty()) { "Restaurant name must not be empty" }
        require(checkUniquenessOfRestaurant(id, name)) { "Restaurant IDs and names must be unique" }
        require(checkPresenceOfSingleCookWaiterTableRecipe(jsonObject)) { "Restaurant must define recipes, waitstaff, and tables" }

        tableIds.clear()

        val type = enumValue<RestaurantType>(jsonObject.getValue("type").jsonPrimitive.content, "type")
        val openingTickStart = jsonObject.getValue("openingTickStart").jsonPrimitive.int
        val openingTickEnd = jsonObject.getValue("openingTickEnd").jsonPrimitive.int
        require(checkStartEndTicks(openingTickStart, openingTickEnd)) { "Restaurant opening ticks must satisfy 1 <= start < end <= 24" }

        val recipeArray = jsonObject.getValue("recipes").jsonArray
        val recipeIds = recipeArray.map { it.jsonPrimitive.int }
        require(recipeIds.all { it >= 0 }) { "Restaurant recipe IDs must be non-negative" }
        require(checkRestaurantRecipesExsit(recipeIds, recipes.map { it.id })) { "Restaurant $id references a recipe that does not exist" }
        require(recipeIds.isNotEmpty()) { "Restaurant $id has no recipes" }

        val kitchenStaff = parseKitchenStaff(jsonObject.getValue("kitchenStaff").jsonObject)
        val waitstaffCount = jsonObject.getValue("waitstaff").jsonPrimitive.int
        require(waitstaffCount > 0) { "Restaurant $id must employ waitstaff" }

        val driverCount = jsonObject.getValue("deliveryDrivers").jsonPrimitive.int
        require(driverCount >= 0) { "Restaurant $id has a negative driver count" }

        val positiveRatings = jsonObject.getValue("positiveRatings").jsonPrimitive.int
        val negativeRatings = jsonObject.getValue("negativeRatings").jsonPrimitive.int
        require(positiveRatings >= 0 && negativeRatings >= 0) { "Restaurant ratings must be non-negative" }

        val tablesJson = jsonObject.getValue("tables").jsonArray
        require(tablesJson.isNotEmpty()) { "Restaurant $id has no tables" }
        val tables = tablesJson.map { element -> parseTable(element.jsonObject) }

        val menu = recipeIds.map { recipeId -> recipes.first { it.id == recipeId } }
        val event = jsonObject.getValue("event").jsonPrimitive.boolean
        // TODO: consider renaming to isEvent / acceptsEvents
        val stats = RestaurantStats(
            restaurantId = id,
            restaurantType = type,
            openingTickStart = openingTickStart,
            openingTickEnd = openingTickEnd,
            event = event,
            positiveRatings = positiveRatings,
            negativeRatings = negativeRatings,
            menu = menu
        )
        val staff = RestaurantStaff(
            cooks = kitchenStaff.toMutableList(),
            waiters = List(waitstaffCount) { Waiter() }.toMutableList(),
            drivers = List(driverCount) { Driver() }.toMutableList()
        )
        val restaurant = Restaurant(stats, name, staff, tables, stock)
        parsedStats += stats

        return restaurant
    }

    private fun parseTable(jsonObject: JsonObject): Table {
        val id = jsonObject.getValue("id").jsonPrimitive.int
        require(id >= 0) { "Table ID must be non-negative" }
        require(checkUniquenessOfTable(id)) { "Duplicate table ID: $id" }

        val size = jsonObject.getValue("size").jsonPrimitive.int
        require(size in 2..30) { "Table $id size must be between 2 and 30" }

        val type = enumValue<TableType>(jsonObject.getValue("type").jsonPrimitive.content, "table type")

        return Table(id, size, type)
    }

    private fun parseKitchenStaff(jsonObject: JsonObject): List<Cook> {
        val cooks = mutableListOf<Cook>()
        for (cookType in CookType.entries) {
            val count = jsonObject.getValue(cookType.name).jsonPrimitive.int
            require(count >= 0) { "Negative ${cookType.name} staff count" }

            if (cookType == CookType.EXEC) {
                require(count <= 1) { "There can be at most one EXEC cook" }
            }
            repeat(count) { cooks.add(Cook(cookType)) }
        }
        require(cooks.isNotEmpty()) { "Restaurant must employ at least one cook" }

        return cooks
    }

    private fun checkUniquenessOfRestaurant(id: Id, name: String): Boolean {
        if (!restaurantIds.add(id)) return false
        if (!restaurantNames.add(name)) {
            restaurantIds.remove(id)
            return false
        }

        return true
    }

    private fun checkPresenceOfSingleCookWaiterTableRecipe(jsonObject: JsonObject): Boolean {
        return jsonObject.getValue("recipes").jsonArray.isNotEmpty() &&
                jsonObject.getValue("tables").jsonArray.isNotEmpty() &&
                jsonObject.getValue("waitstaff").jsonPrimitive.int > 0
    }

    private fun checkRestaurantRecipesExsit(recepieIdsInRestaurant: List<Int>, recepieIds: List<Int>): Boolean {
        return recepieIdsInRestaurant.distinct().size == recepieIdsInRestaurant.size &&
                recepieIdsInRestaurant.all { it in recepieIds }
    }

    private fun checkStartEndTicks(openingTickStart: Tick, openingTickEnd: Tick): Boolean {
        return openingTickStart in 1..24 &&
                openingTickEnd in 1..24 &&
                openingTickEnd > openingTickStart
    }

    private fun checkUniquenessOfTable(id: Id): Boolean {
        return tableIds.add(id)
        // returns true if added to set, false if already contained in set
    }

    // AI generated solution to try to map the parsed string to an enum (given an enum type) based on the enum's name
    // TODO: either ensure enum names correspond to JSON data, use a big switch case, or find a better way
    private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Invalid $field: $value")
}
