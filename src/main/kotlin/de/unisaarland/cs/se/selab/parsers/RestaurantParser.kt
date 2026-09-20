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
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// MIN_TABLE_SIZE/MAX_TABLE_SIZE and MIN_OPENING_TICK/MAX_OPENING_TICK are already enforced by restaurant.schema

/** Parses and validates restaurants */
class RestaurantParser {
    private var recipes: List<Recipe> = emptyList()
    private val restaurantIds = mutableSetOf<Id>()
    private val restaurantNames = mutableSetOf<String>()
    private val tableIds = mutableSetOf<Id>()
    private val parsedStats = mutableListOf<RestaurantStats>()
    private val restaurantTypes = mutableSetOf<RestaurantType>()

    /** Parses and validates restaurants from a JSONArray */
    fun parseRestaurants(
        restaurantArray: JsonArray,
        recipes: List<Recipe>,
        stock: Stock
    ): Pair<List<RestaurantStats>, List<Restaurant>> {
        // already enforced by restaurants.schema (minItems: 1)
        // require(restaurantArray.isNotEmpty()) { "Restaurant file must contain at least one restaurant" }

        this.recipes = recipes // not mutable
        restaurantIds.clear()
        restaurantNames.clear()
        tableIds.clear()
        parsedStats.clear()
        restaurantTypes.clear()

        val parsedRestaurants = restaurantArray.map { element ->
            parseRestaurant(element.jsonObject, recipes, stock)
        }

        require(checkBasicDishCoverageForRestaurantTypes(restaurantTypes, recipes)) {
            "Every restaurant type present must have at least one basic dish default recipe"
        }

        return Pair(parsedStats.toList(), parsedRestaurants)
    }

    private fun parseRestaurant(
        jsonObject: JsonObject,
        recipes: List<Recipe>,
        stock: Stock
    ): Restaurant {
        val id = jsonObject.getValue("id").jsonPrimitive.int
        val name = jsonObject.getValue("name").jsonPrimitive.content
        // already enforced by restaurant.schema (id minimum: 0, name minLength: 1)
        // require(id >= 0) { "Restaurant ID must be non-negative" }
        // require(name.isNotEmpty()) { "Restaurant name must not be empty" }
        require(checkUniquenessOfRestaurant(id, name)) { "Restaurant IDs and names must be unique" }
        // require(
        //     checkRestaurantHasAtLeastOneRecipe(jsonObject)
        // ) { "Restaurant must define at least one recipe" }
        // https://forum.se.cs.uni-saarland.de:51443/t/basic-dish-per-restaurant-type-validation/202/5
        tableIds.clear()

        val typeStr = "type"
        val type = enumValue<RestaurantType>(jsonObject.getValue(typeStr).jsonPrimitive.content, typeStr)
        restaurantTypes.add(type)
        val openingTickStart = jsonObject.getValue("openingTickStart").jsonPrimitive.int
        val openingTickEnd = jsonObject.getValue("openingTickEnd").jsonPrimitive.int
        // checkStartEndTicks now only checks openingTickEnd > openingTickStart
        // 1..24 per-field range is already enforced by restaurant.schema (minimum: 1, maximum: 24)
        require(checkStartEndTicks(openingTickStart, openingTickEnd)) { "openingTickStart must be < openingTickEnd" }

        val recipeArray = jsonObject.getValue("recipes").jsonArray
        val recipeIds = recipeArray.map { it.jsonPrimitive.int }
        // already enforced by restaurant.schema (recipes items minimum: 0)
        // require(recipeIds.all { it >= 0 }) { "Restaurant recipe IDs must be non-negative" }
        require(
            checkRestaurantRecipesExist(recipeIds, recipes.map { it.id })
        ) { "Restaurant $id references a recipe that does not exist" }
        // recipes may be empty: a restaurant's basic dishes are auto-offered from its type's
        // default recipes, which already satisfy "at least one recipe per restaurant"
        // (forum: "Basic dish per restaurant type validation", staff correction, Sep 18 2026)
        require(checkUniqueDishNamesInRestaurant(recipeIds, recipes)) { "R $id dup dish name" }

        val kitchenStaff = parseKitchenStaff(jsonObject.getValue("kitchenStaff").jsonObject)
        val waitstaffCount = jsonObject.getValue("waitstaff").jsonPrimitive.int
        // already enforced by restaurant.schema (waitstaff exclusiveMinimum: 0)
        // require(waitstaffCount > 0) { "Restaurant $id must employ waitstaff" }

        val driverCount = jsonObject.getValue("deliveryDrivers").jsonPrimitive.int
        // already enforced by restaurant.schema (deliveryDrivers minimum: 0)
        // require(driverCount >= 0) { "Restaurant $id has a negative driver count" }

        val positiveRatings = jsonObject.getValue("positiveRatings").jsonPrimitive.int
        val negativeRatings = jsonObject.getValue("negativeRatings").jsonPrimitive.int
        // already enforced by restaurant.schema (both minimum: 0)
        // require(positiveRatings >= 0 && negativeRatings >= 0) { "Restaurant ratings must be non-negative" }

        val tablesJson = jsonObject.getValue("tables").jsonArray
        // already enforced by restaurant.schema (tables minItems: 1)
        // require(tablesJson.isNotEmpty()) { "Restaurant $id has no tables" }
        val tables = tablesJson.map { element -> parseTable(element.jsonObject) }

        val menu = buildMenu(recipeIds, recipes, type)
        // val menu = recipeIds.map { recipeId -> recipes.first { it.id == recipeId } }
        val event = jsonObject.getValue("event").jsonPrimitive.boolean
        val stats = RestaurantStats(
            id,
            type,
            openingTickStart,
            openingTickEnd,
            event,
            positiveRatings,
            negativeRatings,
            menu
        )
        val staff = RestaurantStaff(
            kitchenStaff.toMutableList(),
            List(waitstaffCount) { Waiter() }.toMutableList(),
            List(driverCount) { Driver() }.toMutableList()
        )
        val restaurant = Restaurant(stats, name, staff, tables, stock)
        parsedStats += stats

        return restaurant
    }

    private fun parseTable(jsonObject: JsonObject): Table {
        val id = jsonObject.getValue("id").jsonPrimitive.int
        // already enforced by restaurant.schema (table id minimum: 0)
        // require(id >= 0) { "Table ID must be non-negative" }
        require(checkUniquenessOfTable(id)) { "Duplicate table ID: $id" }

        val size = jsonObject.getValue("size").jsonPrimitive.int
        // already enforced by restaurant.schema (table size minimum: 2, maximum: 30)
        // require(size in MIN_TABLE_SIZE..MAX_TABLE_SIZE) { "Table $id size $MIN_TABLE_SIZE and $MAX_TABLE_SIZE" }

        val type = enumValue<TableType>(jsonObject.getValue("type").jsonPrimitive.content, "table type")

        return Table(id, size, type)
    }

    private fun parseKitchenStaff(jsonObject: JsonObject): List<Cook> {
        val cooks = mutableListOf<Cook>()
        for (cookType in CookType.entries) {
            val count = jsonObject.getValue(cookType.name).jsonPrimitive.int
            // already enforced by restaurant.schema (each cook type minimum: 0, EXEC maximum: 1)
            // require(count >= 0) { "Negative ${cookType.name} staff count" }
            // if (cookType == CookType.EXEC) {
            //     require(count <= 1) { "There can be at most one EXEC cook" }
            // }
            repeat(count) { cooks.add(Cook(cookType)) }
        }
        // Not covered by the schema (no aggregate check across the 8 cook types)
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

    // private fun checkRestaurantHasAtLeastOneRecipe(jsonObject: JsonObject): Boolean {
    //     return jsonObject.getValue("recipes").jsonArray.isNotEmpty()
    // }

    private fun checkRestaurantRecipesExist(recipeIdsInRestaurant: List<Int>, recipeIds: List<Int>): Boolean {
        // uniqueness of recipe ids within a restaurant is already enforced by restaurant.schema
        // (recipes uniqueItems: true, and items are plain integers so this is value-level unique).
        // cross-file existence against food.json's recipes needs checking
        return recipeIdsInRestaurant.all { it in recipeIds }
    }

    private fun checkUniqueDishNamesInRestaurant(recipeIdsInRestaurant: List<Int>, recipes: List<Recipe>): Boolean {
        val menu = recipeIdsInRestaurant.map { recipeId -> recipes.first { it.id == recipeId } }
        return menu.map { it.name }.distinct().size == menu.size
    }

    // DOTO: ensure this is the correct interpretation of the spec
    /** add basic dishes for restaurant type unless overridden by another recipe with same dish name */
    private fun buildMenu(recipeIdsInRestaurant: List<Int>, recipes: List<Recipe>, type: RestaurantType): List<Recipe> {
        val existingRecipes = recipeIdsInRestaurant.map { recipeId -> recipes.first { it.id == recipeId } }
        val existingDishNames = existingRecipes.map { it.name }.toSet()
        val basicDishNames = recipes.filter { it.basicDishFor == type }.map { it.name }.toSet()
        // adapted recipe doesn't carry basicDishFor of its own (only default recipe does which is unique by name)
        // the dish it replaces is still a basic dish of this restaurant, on the menu it keeps its basic status
        val menuRecipes = existingRecipes.map { recipe ->
            if (recipe.basicDishFor == null && recipe.name in basicDishNames) {
                recipe.copy(basicDishFor = type)
            } else {
                recipe
            }
        }
        val defaultBasicDishes = recipes.filter { it.basicDishFor == type && it.name !in existingDishNames }
        return (menuRecipes + defaultBasicDishes).sortedBy { it.id }
    }

    // cross validation
    private fun checkBasicDishCoverageForRestaurantTypes(
        restaurantTypes: Set<RestaurantType>,
        recipes: List<Recipe>
    ): Boolean {
        return restaurantTypes.all { restaurantType ->
            recipes.any { it.basicDishFor == restaurantType }
        }
    }

    private fun checkStartEndTicks(openingTickStart: Tick, openingTickEnd: Tick): Boolean {
        // 1..24 range of each field is already enforced by restaurant.schema
        // (openingTickStart/openingTickEnd minimum: 1, maximum: 24); only the relational
        // constraint "openingTickStart must be less than openingTickEnd" needs checking
        return openingTickEnd > openingTickStart
    }

    private fun checkUniquenessOfTable(id: Id): Boolean {
        return tableIds.add(id)
        // returns true if added to set, false if already contained in set
    }

    // AI generated solution to try to map the parsed string to an enum (given an enum type) based on the enum's name
    // DOIT: either ensure enum names correspond to JSON data, use a big switch case, or find a better way
    private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Invalid $field: $value")
}
