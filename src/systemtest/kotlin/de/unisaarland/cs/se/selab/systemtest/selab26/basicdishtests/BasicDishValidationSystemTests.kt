package de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val DIR = "basicdishjson"
private const val SCENARIO_FILE = "scenarioEmpty.json"
private const val RESTAURANTS_PROBE = "restaurantsProbe.json"

private const val FOOD_MAIN = "food.json"
private const val RESTAURANTS_MAIN = "restaurants.json"

private fun inDir(file: String) = "$DIR/$file"

/**
 * Validation tests for the basic-dish rule of the food file, "there must exist exactly 1 recipe per
 * basic dish name" (specification page 29, line 27). Together they cover whether the recipes carry
 * `basicDishFor` (both, one, none) and whether the types are the same: two invalid and two valid
 * cases, plus a restaurant with no recipes of its own.
 *
 * Written on Sep 20 alongside the basic-dish incident tests, to find out which reading of the rule
 * the reference uses. All five pass on the reference.
 */
abstract class BasicDishValidationSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val maxTicks = 0
    override val restaurants = inDir(RESTAURANTS_PROBE)
    override val scenario = inDir(SCENARIO_FILE)
}

/**
 * Invalid: two recipes carry `basicDishFor` for the same dish name, for different restaurant types.
 * The rule holds per dish name, not per type (forum topic 78: only the default recipe carries the
 * property), so the food file is rejected and that is the first line of the log.
 */
class SameBasicDishNameTwoTypesRejectedSystemTest : BasicDishValidationSystemTest() {
    override val name = "SameBasicDishNameTwoTypesRejectedSystemTest"
    override val description = "A dish name that is a basic dish for two restaurant types is rejected"
    override val food = inDir(FOOD_FILE)

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initFail(FOOD_FILE))
    }

    private companion object {
        const val FOOD_FILE = "foodSameBasicNameTwoTypes.json"
    }
}

/**
 * Invalid: two recipes carry `basicDishFor` for the same dish name and the same type. This case is
 * invalid under every reading of the rule, so it is the control case for the test above.
 */
class SameBasicDishNameSameTypeRejectedSystemTest : BasicDishValidationSystemTest() {
    override val name = "SameBasicDishNameSameTypeRejectedSystemTest"
    override val description = "Two default recipes for the same basic dish of one type are rejected"
    override val food = inDir(FOOD_FILE)

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initFail(FOOD_FILE))
    }

    private companion object {
        const val FOOD_FILE = "foodSameBasicNameSameType.json"
    }
}

/**
 * Valid: a second recipe with the dish name of a basic dish but without `basicDishFor` is how a
 * restaurant adapts the basic dish (forum topic 78). All three files are accepted.
 */
class AdaptedBasicDishAcceptedSystemTest : BasicDishValidationSystemTest() {
    override val name = "AdaptedBasicDishAcceptedSystemTest"
    override val description = "A recipe adapting a basic dish is accepted"
    override val food = inDir(FOOD_MAIN)
    override val restaurants = inDir(RESTAURANTS_MAIN)

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess(FOOD_MAIN))
        assertNextLine(InitialAndPrepTestLogs.initSuccess(RESTAURANTS_MAIN))
        assertNextLine(InitialAndPrepTestLogs.initSuccess(SCENARIO_FILE))
    }
}

/**
 * Valid: two recipes share a dish name that is not a basic dish, and no restaurant lists both.
 * Dish names only have to be unique within a restaurant (forum topic 258), so the file is accepted.
 */
class DuplicateNonBasicDishNameAcceptedSystemTest : BasicDishValidationSystemTest() {
    override val name = "DuplicateNonBasicDishNameAcceptedSystemTest"
    override val description = "Two recipes sharing a non-basic dish name are accepted"
    override val food = inDir(FOOD_FILE)

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess(FOOD_FILE))
        assertNextLine(InitialAndPrepTestLogs.initSuccess(RESTAURANTS_PROBE))
        assertNextLine(InitialAndPrepTestLogs.initSuccess(SCENARIO_FILE))
    }

    private companion object {
        const val FOOD_FILE = "foodDuplicateNonBasicName.json"
    }
}

/**
 * Valid: a restaurant with an empty `recipes` array. The basic dishes of its type are always on its
 * menu and satisfy "per restaurant there must exist at least one recipe" (tutor correction in forum
 * topic 202). Only checks that the files are accepted.
 */
class EmptyRestaurantRecipesAcceptedSystemTest : BasicDishValidationSystemTest() {
    override val name = "EmptyRestaurantRecipesAcceptedSystemTest"
    override val description = "A restaurant without own recipes still offers the basic dishes of its type"
    override val food = inDir(FOOD_MAIN)
    override val restaurants = inDir(RESTAURANTS_FILE)

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess(FOOD_MAIN))
        assertNextLine(InitialAndPrepTestLogs.initSuccess(RESTAURANTS_FILE))
        assertNextLine(InitialAndPrepTestLogs.initSuccess(SCENARIO_FILE))
    }

    private companion object {
        const val RESTAURANTS_FILE = "restaurantsEmptyRecipes.json"
    }
}
