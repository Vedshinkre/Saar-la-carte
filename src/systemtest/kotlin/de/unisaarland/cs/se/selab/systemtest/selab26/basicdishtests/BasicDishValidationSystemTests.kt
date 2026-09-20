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
 * Probes for the basic-dish rules of the food file, so that the reference implementation tells us
 * which reading of "there must exist exactly 1 recipe per basic dish name" (specification page 29,
 * line 27) it uses. Each test states the reading we implement today; a failure against the
 * reference tells us that reading is wrong.
 */
abstract class BasicDishValidationSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val maxTicks = 0
    override val restaurants = inDir(RESTAURANTS_PROBE)
    override val scenario = inDir(SCENARIO_FILE)
}

/**
 * Two recipes carry `basicDishFor` for the same dish name but for different restaurant types.
 * We read "exactly 1 recipe per basic dish name" as global in the dish name (forum topic 78: only
 * the default recipe carries the property) and reject the file.
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
 * Two recipes carry `basicDishFor` for the same dish name and the same restaurant type. This is
 * the unambiguous case of the rule and has to be rejected under every reading.
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
 * The adaptation case of forum topic 78: a second recipe with the dish name of a basic dish, but
 * without `basicDishFor` of its own, is accepted. Restaurant 1 owns the adapted recipe alone.
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
 * Two recipes share a dish name and neither of them is a basic dish. Forum topic 287 states that
 * several recipe objects may share a dish name and that `basicDishFor` is only required when the
 * dish name is a basic dish, so the file is accepted.
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
 * A restaurant with an empty `recipes` array. Forum topic 202 carries a tutor correction saying
 * the list may be empty, because the restaurant offers the basic dishes of its type anyway and
 * those satisfy "per restaurant there must exist at least one recipe".
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
