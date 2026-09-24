package de.unisaarland.cs.se.selab.systemtest.selab26.customerparsertests

import de.unisaarland.cs.se.selab.systemtest.selab26.SystemTestSELab26
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs

private const val RESOURCE_DIR = "customerparsertests"
private const val FOOD_FILE = "food.json"
private const val RESTAURANTS_FILE = "restaurants.json"

/**
 * All customer parsing system tests (F05: shared customer group fields and food preferences), in one
 * list so the registration only needs a single loop. Written on Sep 17 as the tester of F05.
 *
 * Every rule of the customer section gets a valid and an invalid case, and every numeric range gets
 * both sides of its boundary (e.g. visitingTick 1 and 21 accepted, 0 and 22 rejected). Each rejected
 * scenario is a small file that breaks exactly one rule, so a failure names the rule. Food and
 * restaurants are the same valid files for every test.
 */
fun customerParserSystemTests(): List<SystemTestSELab26> = listOf(
    CustomerMixedTypesAcceptedSystemTest(),
    CustomerNoGroupsAcceptedSystemTest(),
    CustomerVisitingTickBoundsAcceptedSystemTest(),
    CustomerIdZeroAcceptedSystemTest(),
    CustomerAllTableTypesAcceptedSystemTest(),
    PreferenceSizesEqualGroupAcceptedSystemTest(),
    PreferenceOnlyExcludedAcceptedSystemTest(),
    PreferenceOnlyPreferredAcceptedSystemTest(),
    PreferenceOnlyFavoriteAcceptedSystemTest(),
    PreferenceTwoPropertiesAcceptedSystemTest(),
    PreferenceUnlistedFavoriteDishAcceptedSystemTest(),
    PreferenceAllMenuDishesFavoredAcceptedSystemTest(),
    PreferenceAllButOneIngredientExcludedAcceptedSystemTest(),
    CustomerDuplicateIdSameTypeRejectedSystemTest(),
    CustomerDuplicateIdAcrossTypesRejectedSystemTest(),
    CustomerNegativeIdRejectedSystemTest(),
    CustomerVisitingTickZeroRejectedSystemTest(),
    CustomerVisitingTickTooLateRejectedSystemTest(),
    CustomerUnknownGroupTypeRejectedSystemTest(),
    CustomerUnknownTableTypeRejectedSystemTest(),
    CustomerLowercaseTableTypeRejectedSystemTest(),
    CustomerMissingVisitingTickRejectedSystemTest(),
    CustomerMissingSizeRejectedSystemTest(),
    CustomerMissingFoodPreferencesRejectedSystemTest(),
    PreferenceSizesExceedGroupRejectedSystemTest(),
    PreferenceSizeZeroRejectedSystemTest(),
    PreferenceWithoutPropertiesRejectedSystemTest(),
    PreferenceUnknownExcludedIngredientRejectedSystemTest(),
    PreferenceUnknownPreferredIngredientRejectedSystemTest(),
    PreferenceUnknownFavoriteDishRejectedSystemTest(),
    PreferenceWrongCaseIngredientRejectedSystemTest(),
    PreferenceExcludedAndPreferredRejectedSystemTest(),
    PreferenceAllDishesFavoredRejectedSystemTest(),
    PreferenceAllIngredientsExcludedRejectedSystemTest(),
    PreferenceAllIngredientsPreferredRejectedSystemTest(),
    PreferenceDuplicateEntryRejectedSystemTest(),
    PreferenceEmptyListRejectedSystemTest(),
    PreferenceUnknownPropertyRejectedSystemTest(),
    PreferenceInvalidInLaterGroupRejectedSystemTest(),
)

/**
 * Shared setup: the same valid food and restaurants files for every test, only the scenario
 * changes. maxTicks is 0, so nothing after parsing depends on simulation behaviour.
 */
abstract class CustomerParserSystemTest(protected val scenarioFile: String) : ExampleSystemTestExtension() {
    override val food: String = "$RESOURCE_DIR/$FOOD_FILE"
    override val restaurants: String = "$RESOURCE_DIR/$RESTAURANTS_FILE"
    override val scenario: String = "$RESOURCE_DIR/$scenarioFile"
    override val logLevel: String = "INFO"
    override val maxTicks: Int = 0
}

/**
 * The scenario is valid: all three files are accepted and the simulation starts, then goes
 * straight to the statistics because maxTicks is 0.
 */
open class CustomerScenarioAcceptedSystemTest(
    scenarioFile: String,
    override val name: String,
    override val description: String
) : CustomerParserSystemTest(scenarioFile) {
    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess(FOOD_FILE))
        assertNextLine(InitialAndPrepTestLogs.initSuccess(RESTAURANTS_FILE))
        assertNextLine(InitialAndPrepTestLogs.initSuccess(scenarioFile))
        assertNextLine(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(StatisticsTestLogs.STATS_CALCULATED)
    }
}

/**
 * The scenario is invalid: food and restaurants are accepted, the scenario is reported as
 * invalid, and nothing else is logged.
 */
open class CustomerScenarioRejectedSystemTest(
    scenarioFile: String,
    override val name: String,
    override val description: String
) : CustomerParserSystemTest(scenarioFile) {
    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess(FOOD_FILE))
        assertNextLine(InitialAndPrepTestLogs.initSuccess(RESTAURANTS_FILE))
        assertNextLine(InitialAndPrepTestLogs.initFail(scenarioFile))
        assertEnd()
    }
}

// ---- Valid scenarios ----

/** One REGULAR, CASUAL and EVENT group, each with a food preference, is accepted. */
class CustomerMixedTypesAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioMixedTypes.json",
    name = "Customer Mixed Types Accepted",
    description = "A REGULAR, a CASUAL (BAR table) and an EVENT group with food preferences are valid."
)

/** A scenario without any customer groups is accepted. */
class CustomerNoGroupsAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioNoCustomerGroups.json",
    name = "Customer No Groups Accepted",
    description = "An empty customerGroups array is valid."
)

/** visitingTick 1 and 21, the lowest and highest allowed values, are accepted. */
class CustomerVisitingTickBoundsAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioVisitingTickBounds.json",
    name = "Customer VisitingTick Bounds Accepted",
    description = "visitingTick 1 and visitingTick 21 are both valid."
)

/** Group id 0 is accepted. */
class CustomerIdZeroAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioIdZero.json",
    name = "Customer Id Zero Accepted",
    description = "A customer group with id 0 is valid."
)

/** COMMON, BAR, SEPARATED and an omitted tableType are accepted. */
class CustomerAllTableTypesAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioAllTableTypes.json",
    name = "Customer All Table Types Accepted",
    description = "COMMON, BAR, SEPARATED and a missing tableType (default COMMON) are valid."
)

/** Preference sizes that add up exactly to the group size are accepted. */
class PreferenceSizesEqualGroupAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioPreferenceSizesEqualGroup.json",
    name = "Preference Sizes Equal Group Accepted",
    description = "Two food preferences whose sizes sum exactly to the group size are valid."
)

/** A preference with only excludedIngredients is accepted. */
class PreferenceOnlyExcludedAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioPreferenceOnlyExcluded.json",
    name = "Preference Only Excluded Accepted",
    description = "A food preference that only uses excludedIngredients is valid."
)

/** A preference with only preferredIngredients is accepted. */
class PreferenceOnlyPreferredAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioPreferenceOnlyPreferred.json",
    name = "Preference Only Preferred Accepted",
    description = "A food preference that only uses preferredIngredients is valid."
)

/** A preference with only favoriteDishes is accepted. */
class PreferenceOnlyFavoriteAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioPreferenceOnlyFavorite.json",
    name = "Preference Only Favorite Accepted",
    description = "A food preference that only uses favoriteDishes is valid."
)

/** A preference with two of the three properties is accepted. */
class PreferenceTwoPropertiesAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioPreferenceTwoProperties.json",
    name = "Preference Two Properties Accepted",
    description = "A food preference using excludedIngredients and favoriteDishes is valid."
)

/** A favorite dish that exists as a recipe but is on no restaurant's menu is accepted. */
class PreferenceUnlistedFavoriteDishAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioPreferenceUnlistedFavoriteDish.json",
    name = "Preference Unlisted Favorite Dish Accepted",
    description = "favoriteDishes may name a recipe that no restaurant offers."
)

/** Favoring every dish on the menu is accepted while another recipe exists. */
class PreferenceAllMenuDishesFavoredAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioPreferenceAllMenuDishesFavored.json",
    name = "Preference All Menu Dishes Favored Accepted",
    description = "Favoring all menu dishes is valid when the food file has a further recipe."
)

/** Excluding all ingredients but one is accepted. */
class PreferenceAllButOneIngredientExcludedAcceptedSystemTest : CustomerScenarioAcceptedSystemTest(
    "scenarioPreferenceAllButOneIngredientExcluded.json",
    name = "Preference All But One Ingredient Excluded Accepted",
    description = "Excluding every ingredient except one is valid."
)

// ---- Invalid shared fields ----

/** Two CASUAL groups with the same id are rejected. */
class CustomerDuplicateIdSameTypeRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioDuplicateIdSameType.json",
    name = "Customer Duplicate Id Same Type Rejected",
    description = "Two CASUAL groups sharing an id, with another group in between, are invalid."
)

/** A REGULAR and an EVENT group with the same id are rejected. */
class CustomerDuplicateIdAcrossTypesRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioDuplicateIdAcrossTypes.json",
    name = "Customer Duplicate Id Across Types Rejected",
    description = "A REGULAR and an EVENT group sharing an id are invalid."
)

/** A negative group id is rejected. */
class CustomerNegativeIdRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioNegativeId.json",
    name = "Customer Negative Id Rejected",
    description = "A customer group with id -1 is invalid."
)

/** visitingTick 0 is rejected. */
class CustomerVisitingTickZeroRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioVisitingTickZero.json",
    name = "Customer VisitingTick Zero Rejected",
    description = "visitingTick 0 is below the allowed range and invalid."
)

/** visitingTick 22 is rejected. */
class CustomerVisitingTickTooLateRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioVisitingTickTooLate.json",
    name = "Customer VisitingTick Too Late Rejected",
    description = "visitingTick 22 is above the allowed range and invalid."
)

/** An unknown customer group type is rejected. */
class CustomerUnknownGroupTypeRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioUnknownGroupType.json",
    name = "Customer Unknown Group Type Rejected",
    description = "A customer group of type VIP is invalid."
)

/** An unknown table type is rejected. */
class CustomerUnknownTableTypeRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioUnknownTableType.json",
    name = "Customer Unknown Table Type Rejected",
    description = "tableType ROOFTOP is invalid."
)

/** A table type written in lowercase is rejected. */
class CustomerLowercaseTableTypeRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioLowercaseTableType.json",
    name = "Customer Lowercase Table Type Rejected",
    description = "tableType bar (lowercase) is invalid."
)

/** A customer group without visitingTick is rejected. */
class CustomerMissingVisitingTickRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioMissingVisitingTick.json",
    name = "Customer Missing VisitingTick Rejected",
    description = "A REGULAR group without visitingTick is invalid."
)

/** A customer group without size is rejected. */
class CustomerMissingSizeRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioMissingSize.json",
    name = "Customer Missing Size Rejected",
    description = "A CASUAL group without size is invalid."
)

/** A customer group without foodPreferences is rejected. */
class CustomerMissingFoodPreferencesRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioMissingFoodPreferences.json",
    name = "Customer Missing Food Preferences Rejected",
    description = "A REGULAR group without the foodPreferences array is invalid."
)

// ---- Invalid food preferences ----

/** Preference sizes that add up to more than the group size are rejected. */
class PreferenceSizesExceedGroupRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceSizesExceedGroup.json",
    name = "Preference Sizes Exceed Group Rejected",
    description = "Food preference sizes 2 + 1 in a group of size 2 are invalid."
)

/** A preference of size 0 is rejected. */
class PreferenceSizeZeroRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceSizeZero.json",
    name = "Preference Size Zero Rejected",
    description = "A food preference with size 0 is invalid."
)

/** A preference with none of the three properties is rejected. */
class PreferenceWithoutPropertiesRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceWithoutProperties.json",
    name = "Preference Without Properties Rejected",
    description = "A food preference with only a size is invalid."
)

/** An excluded ingredient that doesn't exist is rejected. */
class PreferenceUnknownExcludedIngredientRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceUnknownExcludedIngredient.json",
    name = "Preference Unknown Excluded Ingredient Rejected",
    description = "Excluding an ingredient that is not in the food file is invalid."
)

/** A preferred ingredient that doesn't exist is rejected. */
class PreferenceUnknownPreferredIngredientRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceUnknownPreferredIngredient.json",
    name = "Preference Unknown Preferred Ingredient Rejected",
    description = "Preferring an ingredient that is not in the food file is invalid."
)

/** A favorite dish that isn't any recipe is rejected. */
class PreferenceUnknownFavoriteDishRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceUnknownFavoriteDish.json",
    name = "Preference Unknown Favorite Dish Rejected",
    description = "Favoring a dish name that no recipe has is invalid."
)

/** An ingredient name with different capitalisation is rejected. */
class PreferenceWrongCaseIngredientRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceWrongCaseIngredient.json",
    name = "Preference Wrong Case Ingredient Rejected",
    description = "Excluding Onion when the ingredient is called onion is invalid."
)

/** An ingredient that is both excluded and preferred is rejected. */
class PreferenceExcludedAndPreferredRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceExcludedAndPreferred.json",
    name = "Preference Excluded And Preferred Rejected",
    description = "An ingredient in both excludedIngredients and preferredIngredients is invalid."
)

/** Favoring every recipe in the food file is rejected. */
class PreferenceAllDishesFavoredRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceAllDishesFavored.json",
    name = "Preference All Dishes Favored Rejected",
    description = "favoriteDishes equal to the set of all dish names is invalid."
)

/** Excluding every ingredient is rejected. */
class PreferenceAllIngredientsExcludedRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceAllIngredientsExcluded.json",
    name = "Preference All Ingredients Excluded Rejected",
    description = "excludedIngredients equal to the set of all ingredients (in another order) is invalid."
)

/** Preferring every ingredient is rejected. */
class PreferenceAllIngredientsPreferredRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceAllIngredientsPreferred.json",
    name = "Preference All Ingredients Preferred Rejected",
    description = "preferredIngredients equal to the set of all ingredients (in another order) is invalid."
)

/** The same ingredient twice in one list is rejected. */
class PreferenceDuplicateEntryRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceDuplicateEntry.json",
    name = "Preference Duplicate Entry Rejected",
    description = "The same ingredient listed twice in excludedIngredients is invalid."
)

/** An explicitly empty preference list is rejected. */
class PreferenceEmptyListRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceEmptyList.json",
    name = "Preference Empty List Rejected",
    description = "An empty excludedIngredients array next to a non-empty one is invalid."
)

/** An unknown property inside a food preference is rejected. */
class PreferenceUnknownPropertyRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceUnknownProperty.json",
    name = "Preference Unknown Property Rejected",
    description = "A food preference with an extra dislikedDishes property is invalid."
)

/** An invalid preference in a later group is rejected even though the first group is valid. */
class PreferenceInvalidInLaterGroupRejectedSystemTest : CustomerScenarioRejectedSystemTest(
    "scenarioPreferenceInvalidInLaterGroup.json",
    name = "Preference Invalid In Later Group Rejected",
    description = "A valid first group does not hide an unknown favorite dish in the second group."
)
