package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.SystemTestSELab26
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * for F03
 * all food parsing tests of, one valid file and one test per rejected file
 */
object FoodParsingSystemTests {
    private const val DIR = "anshtests/foodparsing"
    private const val FOOD = "food.json"
    private const val RESTAURANTS = "restaurants.json"
    private const val SCENARIO = "scenario.json"

    // each file breaks one ingredient/recipe rule
    private val REJECTED_FOOD_FILES = listOf(
        "foodBestBeforeZero.json",
        "foodUnknownUnit.json",
        "foodMissingBestBefore.json",
        "foodEmptyRecipes.json",
        "foodNegativeRecipeId.json",
        "foodMissingDishName.json",
        "foodDurationTooLong.json",
        "foodEmptyCookType.json",
        "foodUnknownCookType.json",
        "foodEmptyRecipeIngredients.json",
        "foodIngredientAmountZero.json",
    )

    /** every food parsing test to register */
    fun all(): List<SystemTestSELab26> =
        listOf(FoodUnitsAndDurationBoundsAcceptedSystemTest()) + REJECTED_FOOD_FILES.map(::FoodRejectedSystemTest)

    // units g, X, mL parsed and bought in whole packages, durations 2 and 40 accepted
    // one table of 2 -> one meal per dish
    // pancake: 150 g flour, 2 eggs, 300 mL milk / crepe: 50 g, 1, 100 mL
    private class FoodUnitsAndDurationBoundsAcceptedSystemTest : ExampleSystemTestExtension() {
        override val name = "FoodUnitsAndDurationBoundsAccepted"
        override val description = "Units g, X and mL and durations 2 and 40 are parsed and procured"
        override val food = "$DIR/$FOOD"
        override val restaurants = "$DIR/$RESTAURANTS"
        override val scenario = "$DIR/$SCENARIO"
        override val logLevel = "DEBUG"
        override val maxTicks = 1

        override suspend fun run() {
            assertNextLine(InitialAndPrepTestLogs.initSuccess(FOOD))
            assertNextLine(InitialAndPrepTestLogs.initSuccess(RESTAURANTS))
            assertNextLine(InitialAndPrepTestLogs.initSuccess(SCENARIO))
            assertNextLine(InitialAndPrepTestLogs.SIM_START)
            assertNextLine(InitialAndPrepTestLogs.prepStart(1))
            assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 6, "X", "egg"))
            assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 200, "g", "flour"))
            assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 500, "mL", "milk"))
            assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        }
    }

    // bad food file -> only the fail log, restaurants and scenario never parsed
    private class FoodRejectedSystemTest(private val foodFile: String) : ExampleSystemTestExtension() {
        override val name = "FoodRejected-${foodFile.removeSuffix(".json")}"
        override val description = "$foodFile breaks one food rule and stops the initialization"
        override val food = "$DIR/$foodFile"
        override val restaurants = "$DIR/$RESTAURANTS"
        override val scenario = "$DIR/$SCENARIO"
        override val logLevel = "DEBUG"
        override val maxTicks = 1

        override suspend fun run() {
            assertNextLine(InitialAndPrepTestLogs.initFail(foodFile))
            assertEnd()
        }
    }
}
