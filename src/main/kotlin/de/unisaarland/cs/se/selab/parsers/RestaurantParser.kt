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

// most per-field range checks (ids, sizes, opening ticks, staff counts) live in restaurant.schema,
// only what the schema can't express is validated here

/** parses and validates restaurants */
class RestaurantParser {
    private var recipes: List<Recipe> = emptyList()
    private val restaurantIds = mutableSetOf<Id>()
    private val restaurantNames = mutableSetOf<String>()
    private val tableIds = mutableSetOf<Id>()
    private val parsedStats = mutableListOf<RestaurantStats>()
    private val restaurantTypes = mutableSetOf<RestaurantType>()

    /**
     * parses and validates all restaurants of the restaurants file
     *
     * @param restaurantArray the top-level restaurants array
     * @param recipes all recipes parsed from the food file
     * @param stock the shared stock every restaurant draws from
     * @return the parsed stats and restaurants, both in file order
     * @throws IllegalArgumentException if a restaurant fails validation
     */
    fun parseRestaurants(
        restaurantArray: JsonArray,
        recipes: List<Recipe>,
        stock: Stock
    ): Pair<List<RestaurantStats>, List<Restaurant>> {
        this.recipes = recipes
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
        require(checkUniquenessOfRestaurant(id, name)) { "Restaurant IDs and names must be unique" }
        tableIds.clear()

        val typeStr = "type"
        val type = enumValue<RestaurantType>(jsonObject.getValue(typeStr).jsonPrimitive.content, typeStr)
        restaurantTypes.add(type)
        val openingTickStart = jsonObject.getValue("openingTickStart").jsonPrimitive.int
        val openingTickEnd = jsonObject.getValue("openingTickEnd").jsonPrimitive.int
        require(checkStartEndTicks(openingTickStart, openingTickEnd)) { "openingTickStart must be < openingTickEnd" }

        val recipeArray = jsonObject.getValue("recipes").jsonArray
        val recipeIds = recipeArray.map { it.jsonPrimitive.int }
        require(
            checkRestaurantRecipesExist(recipeIds, recipes.map { it.id })
        ) { "Restaurant $id references a recipe that does not exist" }
        // recipes may be empty, the type's default basic dishes are always on the menu
        // https://forum.se.cs.uni-saarland.de:51443/t/basic-dish-per-restaurant-type-validation/202/5
        require(checkUniqueDishNamesInRestaurant(recipeIds, recipes)) { "R $id dup dish name" }

        val kitchenStaff = parseKitchenStaff(jsonObject.getValue("kitchenStaff").jsonObject)
        val waitstaffCount = jsonObject.getValue("waitstaff").jsonPrimitive.int

        val driverCount = jsonObject.getValue("deliveryDrivers").jsonPrimitive.int

        val positiveRatings = jsonObject.getValue("positiveRatings").jsonPrimitive.int
        val negativeRatings = jsonObject.getValue("negativeRatings").jsonPrimitive.int

        val tablesJson = jsonObject.getValue("tables").jsonArray
        val tables = tablesJson.map { element -> parseTable(element.jsonObject) }

        val menu = buildMenu(recipeIds, recipes, type)
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
        require(checkUniquenessOfTable(id)) { "Duplicate table ID: $id" }

        val size = jsonObject.getValue("size").jsonPrimitive.int
        val type = enumValue<TableType>(jsonObject.getValue("type").jsonPrimitive.content, "table type")

        return Table(id, size, type)
    }

    private fun parseKitchenStaff(jsonObject: JsonObject): List<Cook> {
        val cooks = mutableListOf<Cook>()
        for (cookType in CookType.entries) {
            val count = jsonObject.getValue(cookType.name).jsonPrimitive.int
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

    private fun checkRestaurantRecipesExist(recipeIdsInRestaurant: List<Int>, recipeIds: List<Int>): Boolean {
        return recipeIdsInRestaurant.all { it in recipeIds }
    }

    private fun checkUniqueDishNamesInRestaurant(recipeIdsInRestaurant: List<Int>, recipes: List<Recipe>): Boolean {
        val menu = recipeIdsInRestaurant.map { recipeId -> recipes.first { it.id == recipeId } }
        return menu.map { it.name }.distinct().size == menu.size
    }

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

    private fun checkBasicDishCoverageForRestaurantTypes(
        restaurantTypes: Set<RestaurantType>,
        recipes: List<Recipe>
    ): Boolean {
        return restaurantTypes.all { restaurantType ->
            recipes.any { it.basicDishFor == restaurantType }
        }
    }

    private fun checkStartEndTicks(openingTickStart: Tick, openingTickEnd: Tick): Boolean {
        return openingTickEnd > openingTickStart
    }

    private fun checkUniquenessOfTable(id: Id): Boolean {
        return tableIds.add(id)
    }

    // maps the parsed string to the enum constant with the same name
    private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Invalid $field: $value")
}
