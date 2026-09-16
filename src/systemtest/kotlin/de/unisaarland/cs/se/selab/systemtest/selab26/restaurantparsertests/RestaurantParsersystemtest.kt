package de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests

import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.parsers.FoodParser
import de.unisaarland.cs.se.selab.parsers.RestaurantParser
import de.unisaarland.cs.se.selab.systemtest.api.SystemTestAssertionError
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File

private const val RESOURCE_DIR = "src/systemtest/resources/RestaurantParserTests/"
private const val FOOD_FILE = "Food.json"

/**
 * Base class for restaurant parser system tests, asserting that an invalid
 * restaurants file is rejected while the accompanying valid food file parses successfully.
 */
abstract class RestaurantParserSystemTest : ExampleSystemTestExtension() {

    override val logLevel: String = "DEBUG"

    override val food: String = "RestaurantParserTests/Food.json"

    override val restaurants: String = "RestaurantParserTests/Restaurants.json"

    override val scenario: String = "RestaurantParserTests/scenario.json"

    override val maxTicks: Int = 10

    override val name: String = "Restaurant parser system test"

    override val description: String = "Base class for restaurant parser system tests."

    private val foodParser = FoodParser()

    private val restaurantParser = RestaurantParser()

    protected suspend fun assertInvalidRestaurantLog(
        restaurantFileName: String
    ) {
        assertNextLine(
            "[INFO] Initialization Info: " + "$FOOD_FILE successfully parsed and validated."
        )

        assertNextLine(
            "[IMPORTANT] Initialization Info: " + "$restaurantFileName is invalid."
        )

        assertRestaurantParsingRejected(restaurantFileName)
    }

    /**
     * Drives [foodParser] and [restaurantParser] directly over the same fixtures the
     * subprocess just parsed, so the rejection is also verified in-process rather than
     * only through the log line above.
     */
    private fun assertRestaurantParsingRejected(restaurantFileName: String) {
        val foodJson = Json.parseToJsonElement(File(RESOURCE_DIR + FOOD_FILE).readText()).jsonObject
        val (ingredients, recipes) = foodParser.parse(
            foodJson.getValue("ingredients").jsonArray,
            foodJson.getValue("recipes").jsonArray
        )
        val stock = Stock(ingredients)

        val restaurantsJson = Json.parseToJsonElement(File(RESOURCE_DIR + restaurantFileName).readText()).jsonObject

        val wasRejected = try {
            restaurantParser.parseRestaurants(
                restaurantsJson.getValue("restaurants").jsonArray,
                recipes,
                stock
            )
            false
        } catch (_: IllegalArgumentException) {
            true
        }

        if (!wasRejected) {
            throw SystemTestAssertionError(
                "Expected RestaurantParser to reject $restaurantFileName directly, but it did not."
            )
        }
    }
}

/**
 * System test asserting parsing fails when two restaurants share the same ID.
 */
class RestaurantsDifferentNameSameIdTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Different Name Same ID System Test"

    override val description: String = "System test asserting parsing fails when two restaurants share the same ID."

    override val restaurants: String = "RestaurantParserTests/restaurantsDifferentNameSameId.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsDifferentNameSameId.json"
        )
    }
}

/**
 * System test asserting parsing fails when restaurant menu lacks a basic dish.
 */
class RestaurantsNoBasicDishOfItsTypeTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants No Basic Dish Of Its Type System Test"

    override val description: String = "System test asserting parsing fails when restaurant menu lacks a basic dish."

    override val restaurants: String = "RestaurantParserTests/restaurantsNoBasicDishOfItsType.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoBasicDishOfItsType.json"
        )
    }
}

/**
 * System test asserting parsing fails when a restaurant has no cooks.
 */
class RestaurantsNoCookExistsTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants No Cook Exists System Test"

    override val description: String = "System test asserting parsing fails when a restaurant has no cooks."

    override val restaurants: String = "RestaurantParserTests/restaurantsNoCookExists.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoCookExists.json"
        )
    }
}

/**
 * System test asserting parsing fails when dish names are duplicated.
 */
class RestaurantsNoDuplicateDishNameTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants No Duplicate Dish Name System Test"

    override val description: String = "System test asserting parsing fails when dish names are duplicated."

    override val restaurants: String = "RestaurantParserTests/restaurantsNoDupliacteDishName.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoDupliacteDishName.json"
        )
    }
}

/**
 * System test asserting parsing fails for an invalid table size.
 */
class RestaurantsNoInvalidTableSizeTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Invalid Table Size System Test"

    override val description: String = "System test asserting parsing fails for an invalid table size."

    override val restaurants: String = "RestaurantParserTests/restaurantsNoInvalidTableSize.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoInvalidTableSize.json"
        )
    }
}

/**
 * System test asserting parsing fails for an invalid table size boundary.
 */
class RestaurantsNoInvalidTableSize1Test : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Invalid Table Size 1 System Test"

    override val description: String = "System test asserting parsing fails for an invalid table size boundary."

    override val restaurants: String = "RestaurantParserTests/restaurantsNoInvalidTableSize1.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoInvalidTableSize1.json"
        )
    }
}

/**
 * System test asserting parsing fails when multiple head cooks exist.
 */
class RestaurantsNoMultipleHeadCooksEXECTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Multiple Head Cooks EXEC System Test"

    override val description: String = "System test asserting parsing fails when multiple head cooks exist."

    override val restaurants: String = "RestaurantParserTests/restaurantsNoMultipleHeadCooksieEXEC.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoMultipleHeadCooksieEXEC.json"
        )
    }
}

/**
 * System test asserting parsing fails when the restaurant menu is empty.
 */
class RestaurantsNoRecipeExistsTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants No Recipe Exists System Test"

    override val description: String = "System test asserting parsing fails when the restaurant menu is empty."

    override val restaurants: String = "RestaurantParserTests/restaurantsNoRecipieExists.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoRecipieExists.json"
        )
    }
}

/**
 * System test asserting parsing fails when a restaurant has no tables.
 */
class RestaurantsNoTableExistsTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants No Table Exists System Test"

    override val description: String = "System test asserting parsing fails when a restaurant has no tables."

    override val restaurants: String = "RestaurantParserTests/restaurantsNoTableExists.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoTableExists.json"
        )
    }
}

/**
 * System test asserting parsing fails when table IDs are duplicated.
 */
class RestaurantsNotUniqueTablesTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Not Unique Tables System Test"

    override val description: String = "System test asserting parsing fails when table IDs are duplicated."

    override val restaurants: String = "RestaurantParserTests/restaurantsNotUniqueTables.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNotUniqueTables.json"
        )
    }
}

/**
 * System test asserting parsing fails when no waiter exists.
 */
class RestaurantsNoWaiterExistsTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants No Waiter Exists System Test"

    override val description: String = "System test asserting parsing fails when no waiter exists."

    override val restaurants: String = "RestaurantParserTests/restaurantsNoWaiterExists.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoWaiterExists.json"
        )
    }
}

/**
 * System test asserting parsing fails when the opening end tick is smaller.
 */
class RestaurantsOpeningTickEndLessThanOpeningTickStartTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Opening Tick End Less Than Start System Test"

    override val description: String = "System test asserting parsing fails when the opening end tick is smaller."

    override val restaurants: String =
        "RestaurantParserTests/" + "restaurantsOpeningTickEndLessThanOpeningTickStart.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsOpeningTickEndLessThanOpeningTickStart.json"
        )
    }
}

/**
 * System test asserting parsing fails for invalid positive ratings.
 */
class RestaurantsPositiveRatingsG0Test : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Positive Ratings G0 System Test"

    override val description: String = "System test asserting parsing fails for invalid positive ratings."

    override val restaurants: String = "RestaurantParserTests/restaurantsPositiveRatingsG0.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsPositiveRatingsG0.json"
        )
    }
}

/**
 * System test asserting parsing fails for negative initial ratings.
 */
class RestaurantsPositiveRatingsNeg0Test : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Positive Ratings Negative System Test"

    override val description: String = "System test asserting parsing fails for negative initial ratings."

    override val restaurants: String = "RestaurantParserTests/restaurantsPositiveRatingsNeg0.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsPositiveRatingsNeg0.json"
        )
    }
}

/**
 * System test asserting parsing fails for a missing recipe reference.
 */
class RestaurantsRecipesNotExistTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Recipes Not Exist System Test"

    override val description: String = "System test asserting parsing fails for a missing recipe reference."

    override val restaurants: String = "RestaurantParserTests/restaurantsRecipiesNotExist.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsRecipiesNotExist.json"
        )
    }
}

/**
 * System test asserting parsing fails when restaurant names are duplicated.
 */
class RestaurantsSameNameDifferentIdTest : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Same Name Different ID System Test"

    override val description: String = "System test asserting parsing fails when restaurant names are duplicated."

    override val restaurants: String = "RestaurantParserTests/restaurantsSameNameDifferentId.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsSameNameDifferentId.json"
        )
    }
}

/**
 * System test asserting parsing fails for ticks outside the valid range.
 */
class RestaurantsTickNotIn1to24Test : RestaurantParserSystemTest() {

    override val name: String = "Restaurants Tick Not In 1 to 24 System Test"

    override val description: String = "System test asserting parsing fails for ticks outside the valid range."

    override val restaurants: String = "RestaurantParserTests/restaurantsTickNotIn1to24.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsTickNotIn1to24.json"
        )
    }
}
