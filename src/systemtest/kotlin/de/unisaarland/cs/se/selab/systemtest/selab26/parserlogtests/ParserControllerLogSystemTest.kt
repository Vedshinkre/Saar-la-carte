package de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension

private const val RESOURCE_DIR = "ParserControllerLogTests"
private const val FOOD_FILE = "food.json"
private const val RESTAURANTS_FILE = "restaurants.json"

private fun successLine(file: String) = "[INFO] Initialization Info: $file successfully parsed and validated."

private fun failLine(file: String) = "[IMPORTANT] Initialization Info: $file is invalid."

/**
 * To Test the Parsers
 */
abstract class ParserControllerLogSystemTest : ExampleSystemTestExtension() {
    override val logLevel: String = "DEBUG"
    override val maxTicks: Int = 0
    override val food: String = "$RESOURCE_DIR/$FOOD_FILE"
    override val restaurants: String = "$RESOURCE_DIR/$RESTAURANTS_FILE"
    override val scenario: String = "$RESOURCE_DIR/scenario.json"
}

/**
 * All three files valid: expects three success logs, in order.
 */
class AllFilesValidSystemTest : ParserControllerLogSystemTest() {
    override val name = "All Files Valid System Test"
    override val description = "Food, restaurants and scenario are all valid: three success logs, in order."

    override suspend fun run() {
        assertNextLine(successLine(FOOD_FILE))
        assertNextLine(successLine(RESTAURANTS_FILE))
        assertNextLine(successLine("scenario.json"))
    }
}

/**
 * A customer group referencing a restaurant id that does not exist: food and restaurants
 * succeed, scenario fails.
 */
class CustomerGroupUnknownRestaurantSystemTest : ParserControllerLogSystemTest() {
    override val scenario: String = "$RESOURCE_DIR/scenarioBadCustomerGroupRestaurant.json"
    override val name = "Customer Group Unknown Restaurant System Test"
    override val description = "A REGULAR customer group referencing an unknown restaurant id is rejected."

    override suspend fun run() {
        assertNextLine(successLine(FOOD_FILE))
        assertNextLine(successLine(RESTAURANTS_FILE))
        assertNextLine(failLine("scenarioBadCustomerGroupRestaurant.json"))
    }
}

/**
 * A STAFF incident referencing a restaurant id that does not exist: food and restaurants
 * succeed, scenario fails.
 */
class StaffIncidentUnknownRestaurantSystemTest : ParserControllerLogSystemTest() {
    override val scenario: String = "$RESOURCE_DIR/scenarioBadIncidentRestaurant.json"
    override val name = "Staff Incident Unknown Restaurant System Test"
    override val description = "A STAFF incident referencing an unknown restaurant id is rejected."

    override suspend fun run() {
        assertNextLine(successLine(FOOD_FILE))
        assertNextLine(successLine(RESTAURANTS_FILE))
        assertNextLine(failLine("scenarioBadIncidentRestaurant.json"))
    }
}

/**
 * Distinct, non-overlapping incidents are accepted by crossvalidation.
 */
class ValidIncidentsAcceptedSystemTest : ParserControllerLogSystemTest() {
    override val scenario: String = "$RESOURCE_DIR/scenarioValidIncidents.json"
    override val name = "Valid Incidents Accepted System Test"
    override val description = "Distinct, non-overlapping incidents pass crossvalidation."

    override suspend fun run() {
        assertNextLine(successLine(FOOD_FILE))
        assertNextLine(successLine(RESTAURANTS_FILE))
        assertNextLine(successLine("scenarioValidIncidents.json"))
    }
}

/**
 * Two incidents sharing the same id are rejected by crossvalidation.
 */
class DuplicateIncidentIdRejectedSystemTest : ParserControllerLogSystemTest() {
    override val scenario: String = "$RESOURCE_DIR/scenarioDuplicateIncidentId.json"
    override val name = "Duplicate Incident Id Rejected System Test"
    override val description = "A duplicate incident id is rejected by crossvalidation."

    override suspend fun run() {
        assertNextLine(successLine(FOOD_FILE))
        assertNextLine(successLine(RESTAURANTS_FILE))
        assertNextLine(failLine("scenarioDuplicateIncidentId.json"))
    }
}

/**
 * Two overlapping UNAVAILABLE incidents on the same ingredient are rejected by crossvalidation.
 */
class OverlappingUnavailabilityRejectedSystemTest : ParserControllerLogSystemTest() {
    override val scenario: String = "$RESOURCE_DIR/scenarioOverlappingUnavailability.json"
    override val name = "Overlapping Unavailability Rejected System Test"
    override val description = "Overlapping UNAVAILABLE incidents on the same ingredient are rejected."

    override suspend fun run() {
        assertNextLine(successLine(FOOD_FILE))
        assertNextLine(successLine(RESTAURANTS_FILE))
        assertNextLine(failLine("scenarioOverlappingUnavailability.json"))
    }
}

/**
 * Shared shape for "this restaurants fixture must be rejected" system tests: food succeeds,
 * then the restaurants file fails, and scenario is never attempted.
 */
open class RestaurantFixtureRejectedSystemTest(
    private val fixtureFileName: String,
    override val name: String,
    override val description: String
) : ParserControllerLogSystemTest() {
    override val restaurants: String = "$RESOURCE_DIR/$fixtureFileName"

    override suspend fun run() {
        assertNextLine(successLine(FOOD_FILE))
        assertNextLine(failLine(fixtureFileName))
    }
}

/**
 * Two restaurants sharing the same id are rejected.
 */
class RestaurantsDifferentNameSameIdRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsDifferentNameSameId.json",
    name = "restaurantsDifferentNameSameId Is Rejected System Test",
    description = "Two restaurants sharing the same id are rejected."
)

/**
 * Two restaurants sharing the same name are rejected.
 */
class RestaurantsSameNameDifferentIdRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsSameNameDifferentId.json",
    name = "restaurantsSameNameDifferentId Is Rejected System Test",
    description = "Two restaurants sharing the same name are rejected."
)

/**
 * Duplicate table ids within a restaurant are rejected.
 */
class RestaurantsNotUniqueTablesRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNotUniqueTables.json",
    name = "restaurantsNotUniqueTables Is Rejected System Test",
    description = "Duplicate table ids within a restaurant are rejected."
)

/**
 * A restaurant with zero cooks is rejected.
 */
class RestaurantsNoCookExistsRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNoCookExists.json",
    name = "restaurantsNoCookExists Is Rejected System Test",
    description = "A restaurant with zero cooks is rejected."
)

/**
 * A restaurant with zero waitstaff is rejected.
 */
class RestaurantsNoWaiterExistsRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNoWaiterExists.json",
    name = "restaurantsNoWaiterExists Is Rejected System Test",
    description = "A restaurant with zero waitstaff is rejected."
)

/**
 * A restaurant with zero tables is rejected.
 */
class RestaurantsNoTableExistsRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNoTableExists.json",
    name = "restaurantsNoTableExists Is Rejected System Test",
    description = "A restaurant with zero tables is rejected."
)

/**
 * A restaurant with an empty menu is rejected.
 */
class RestaurantsNoRecipieExistsRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNoRecipieExists.json",
    name = "restaurantsNoRecipieExists Is Rejected System Test",
    description = "A restaurant with an empty menu is rejected."
)

/**
 * A restaurant with more than one EXEC cook is rejected.
 */
class RestaurantsNoMultipleHeadCooksieEXECRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNoMultipleHeadCooksieEXEC.json",
    name = "restaurantsNoMultipleHeadCooksieEXEC Is Rejected System Test",
    description = "A restaurant with more than one EXEC cook is rejected."
)

/**
 * A table size outside the valid range is rejected.
 */
class RestaurantsNoInvalidTableSizeRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNoInvalidTableSize.json",
    name = "restaurantsNoInvalidTableSize Is Rejected System Test",
    description = "A table size outside the valid range is rejected."
)

/**
 * A table size at the other invalid boundary is rejected.
 */
class RestaurantsNoInvalidTableSize1RejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNoInvalidTableSize1.json",
    name = "restaurantsNoInvalidTableSize1 Is Rejected System Test",
    description = "A table size at the other invalid boundary is rejected."
)

/**
 * An opening end tick smaller than the start tick is rejected.
 */
class RestaurantsOpeningTickEndLessThanOpeningTickStartRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsOpeningTickEndLessThanOpeningTickStart.json",
    name = "restaurantsOpeningTickEndLessThanOpeningTickStart Is Rejected System Test",
    description = "An opening end tick smaller than the start tick is rejected."
)

/**
 * Opening ticks outside 1..24 are rejected.
 */
class RestaurantsTickNotIn1to24RejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsTickNotIn1to24.json",
    name = "restaurantsTickNotIn1to24 Is Rejected System Test",
    description = "Opening ticks outside 1..24 are rejected."
)

/**
 * An invalid positive ratings value is rejected.
 */
class RestaurantsPositiveRatingsG0RejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsPositiveRatingsG0.json",
    name = "restaurantsPositiveRatingsG0 Is Rejected System Test",
    description = "An invalid positive-ratings value is rejected."
)

/**
 * A negative initial rating is rejected.
 */
class RestaurantsPositiveRatingsNeg0RejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsPositiveRatingsNeg0.json",
    name = "restaurantsPositiveRatingsNeg0 Is Rejected System Test",
    description = "A negative initial rating is rejected."
)

/**
 * A restaurant referencing a recipe id that does not exist is rejected.
 */
class RestaurantsRecipiesNotExistRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsRecipiesNotExist.json",
    name = "restaurantsRecipiesNotExist Is Rejected System Test",
    description = "A restaurant referencing a recipe id that does not exist is rejected."
)

/**
 * Duplicate dish names on one restaurant's menu are rejected.
 */
class RestaurantsNoDupliacteDishNameRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNoDupliacteDishName.json",
    name = "restaurantsNoDupliacteDishName Is Rejected System Test",
    description = "Duplicate dish names on one restaurant's menu are rejected."
)

/**
 * A restaurant type with no basic-dish recipe covering it is rejected.
 */
class RestaurantsNoBasicDishOfItsTypeRejectedSystemTest : RestaurantFixtureRejectedSystemTest(
    "restaurantsNoBasicDishOfItsType.json",
    name = "restaurantsNoBasicDishOfItsType Is Rejected System Test",
    description = "A restaurant type with no basic-dish recipe covering it is rejected."
)

/**
 * Shared shape for  system tests: food fails immediately,
 * before restaurants or scenario are ever attempted.
 */
open class FoodFixtureRejectedSystemTest(
    private val fixtureFileName: String,
    override val name: String,
    override val description: String
) : ParserControllerLogSystemTest() {
    override val food: String = "$RESOURCE_DIR/$fixtureFileName"

    override suspend fun run() {
        assertNextLine(failLine(fixtureFileName))
    }
}

/**
 * An empty ingredients list is rejected, short-circuiting before restaurants/scenario.
 */
class FoodEmptyIngredientsRejectedSystemTest : FoodFixtureRejectedSystemTest(
    "foodEmptyIngredients.json",
    name = "foodEmptyIngredients Is Rejected System Test",
    description = "An empty ingredients list is rejected, short-circuiting before restaurants/scenario."
)

/**
 * A duplicate ingredient name is rejected.
 */
class FoodDuplicateIngredientRejectedSystemTest : FoodFixtureRejectedSystemTest(
    "foodDuplicateIngredient.json",
    name = "foodDuplicateIngredient Is Rejected System Test",
    description = "A duplicate ingredient name is rejected."
)

/**
 * A non-positive packaging volume is rejected.
 */
class FoodInvalidPackagingVolumeRejectedSystemTest : FoodFixtureRejectedSystemTest(
    "foodInvalidPackagingVolume.json",
    name = "foodInvalidPackagingVolume Is Rejected System Test",
    description = "A non-positive packaging volume is rejected."
)

/**
 * A recipe duration below the minimum is rejected.
 */
class FoodRecipeDurationTooShortRejectedSystemTest : FoodFixtureRejectedSystemTest(
    "foodRecipeDurationTooShort.json",
    name = "foodRecipeDurationTooShort Is Rejected System Test",
    description = "A recipe duration below the minimum is rejected."
)

/**
 * A recipe referencing an unknown ingredient is rejected.
 */
class FoodRecipeMissingIngredientRefRejectedSystemTest : FoodFixtureRejectedSystemTest(
    "foodRecipeMissingIngredientRef.json",
    name = "foodRecipeMissingIngredientRef Is Rejected System Test",
    description = "A recipe referencing an unknown ingredient is rejected."
)

/**
 * A duplicate recipe id is rejected.
 */
class FoodDuplicateRecipeIdRejectedSystemTest : FoodFixtureRejectedSystemTest(
    "foodDuplicateRecipeId.json",
    name = "foodDuplicateRecipeId Is Rejected System Test",
    description = "A duplicate recipe id is rejected."
)

/**
 * dish name reused by a non-basic recipe after being claimed as a basic dish is rejected
 */
class FoodDuplicateNameRejectedSystemTest : FoodFixtureRejectedSystemTest(
    "foodDuplicateRecipeId.json",
    name = "foodDuplicateRecipeId Is Rejected System Test",
    description = "A duplicate recipe id is rejected."
)

/**
 * dish name same but not basic dish
 */
class DishDuplicateNameDiffrenntIDRejectedSystemTest : FoodFixtureRejectedSystemTest(
    "foodDuplicateDishNameMixedBasic.json",
    name = "foodDuplicateDishNameMixedBasic.json Is Rejected System Test",
    description = "Only  duplicate name  is rejected."
)
