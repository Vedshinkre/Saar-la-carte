package de.unisaarland.cs.se.selab.systemtest.selab26.RestaurantParserTests
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension

abstract class AbstractRestaurantParserSystemTest :
    ExampleSystemTestExtension() {

    override val logLevel: String = "DEBUG"

    override val food: String =
        "RestaurantParserTests/Food.json"

    override val restaurants: String =
        "RestaurantParserTests/Restaurants.json"

    override val scenario: String =
        "RestaurantParserTests/scenario.json"

    override val maxTicks: Int = 10

    override val name: String =
        "Restaurant parser system test"

    override val description: String =
        "Base class for restaurant parser system tests."

    protected suspend fun assertInvalidRestaurantLog(
        restaurantFileName: String
    ) {
        assertNextLine(
            "[INFO] Initialization Info: " +
                "validFood.json successfully parsed and validated."
        )

        assertNextLine(
            "[IMPORTANT] Initialization Info: " +
                "$restaurantFileName is invalid."
        )
    }
}

/**
 * sake of detect
 */
class RestaurantsDifferentNameSameIdTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Different Name Same ID System Test"

    override val description: String =
        "System test asserting parsing fails when two restaurants share the same ID."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsDifferentNameSameId.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsDifferentNameSameId.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNoBasicDishOfItsTypeTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants No Basic Dish Of Its Type System Test"

    override val description: String =
        "System test asserting parsing fails when restaurant menu lacks a basic dish."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNoBasicDishOfItsType.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoBasicDishOfItsType.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNoCookExistsTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants No Cook Exists System Test"

    override val description: String =
        "System test asserting parsing fails when a restaurant has no cooks."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNoCookExists.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoCookExists.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNoDuplicateDishNameTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants No Duplicate Dish Name System Test"

    override val description: String =
        "System test asserting parsing fails when dish names are duplicated."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNoDupliacteDishName.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoDupliacteDishName.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNoInvalidTableSizeTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Invalid Table Size System Test"

    override val description: String =
        "System test asserting parsing fails for an invalid table size."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNoInvalidTableSize.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoInvalidTableSize.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNoInvalidTableSize1Test :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Invalid Table Size 1 System Test"

    override val description: String =
        "System test asserting parsing fails for an invalid table size boundary."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNoInvalidTableSize1.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoInvalidTableSize1.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNoMultipleHeadCooksEXECTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Multiple Head Cooks EXEC System Test"

    override val description: String =
        "System test asserting parsing fails when multiple head cooks exist."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNoMultipleHeadCooksieEXEC.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoMultipleHeadCooksieEXEC.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNoRecipeExistsTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants No Recipe Exists System Test"

    override val description: String =
        "System test asserting parsing fails when the restaurant menu is empty."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNoRecipieExists.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoRecipieExists.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNoTableExistsTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants No Table Exists System Test"

    override val description: String =
        "System test asserting parsing fails when a restaurant has no tables."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNoTableExists.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoTableExists.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNotUniqueTablesTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Not Unique Tables System Test"

    override val description: String =
        "System test asserting parsing fails when table IDs are duplicated."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNotUniqueTables.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNotUniqueTables.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsNoWaiterExistsTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants No Waiter Exists System Test"

    override val description: String =
        "System test asserting parsing fails when no waiter exists."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsNoWaiterExists.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsNoWaiterExists.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsOpeningTickEndLessThanOpeningTickStartTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Opening Tick End Less Than Start System Test"

    override val description: String =
        "System test asserting parsing fails when the opening end tick is smaller."

    override val restaurants: String =
        "RestaurantParserTests/" +
            "restaurantsOpeningTickEndLessThanOpeningTickStart.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsOpeningTickEndLessThanOpeningTickStart.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsPositiveRatingsG0Test :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Positive Ratings G0 System Test"

    override val description: String =
        "System test asserting parsing fails for invalid positive ratings."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsPositiveRatingsG0.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsPositiveRatingsG0.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsPositiveRatingsNeg0Test :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Positive Ratings Negative System Test"

    override val description: String =
        "System test asserting parsing fails for negative initial ratings."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsPositiveRatingsNeg0.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsPositiveRatingsNeg0.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsRecipesNotExistTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Recipes Not Exist System Test"

    override val description: String =
        "System test asserting parsing fails for a missing recipe reference."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsRecipiesNotExist.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsRecipiesNotExist.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsSameNameDifferentIdTest :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Same Name Different ID System Test"

    override val description: String =
        "System test asserting parsing fails when restaurant names are duplicated."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsSameNameDifferentId.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsSameNameDifferentId.json"
        )
    }
}
/**
 * sake of detect
 */
class RestaurantsTickNotIn1to24Test :
    AbstractRestaurantParserSystemTest() {

    override val name: String =
        "Restaurants Tick Not In 1 to 24 System Test"

    override val description: String =
        "System test asserting parsing fails for ticks outside the valid range."

    override val restaurants: String =
        "RestaurantParserTests/restaurantsTickNotIn1to24.json"

    override suspend fun run() {
        assertInvalidRestaurantLog(
            "restaurantsTickNotIn1to24.json"
        )
    }
}
