package de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val DIR = "basicdishjson"
private const val FOOD = "$DIR/food.json"
private const val UNIT = "g"
private const val FLOUR = "Flour"
private const val RECIPE = "RECIPE"

/**
 * Shared setup for the basic-dish tests.
 *
 * The food file holds one EUROPEAN basic dish "Rice Bread" (recipe 1, 100 g of Flour), an adapted
 * recipe for that same dish name (recipe 2, 200 g, no `basicDishFor` of its own, per forum topic 78)
 * and an unrelated dish "Side Salad" (recipe 3, 300 g).
 *
 * Flour is sold in packages of 1 g, so the procured amount in the preparation phase is exactly the
 * planned amount. Every restaurant has 10 free seats, so the estimate multiplier is 1 and the
 * planned amount of an evening equals the sum of the Flour amounts of the restaurant's menu.
 */
abstract class BasicDishSystemTest : ExampleSystemTestExtension() {
    override val food = FOOD
    override val logLevel = "DEBUG"
    override val maxTicks = 1
}

/**
 * The core test for the shared-ingredient-map defect.
 *
 * Restaurant 1 is EUROPEAN and owns only recipe 2, the adapted "Rice Bread". Its menu is therefore
 * that single recipe, and a RECIPE incident of +10% has to raise its Flour amount from 200 g to
 * 220 g exactly once.
 *
 * Our implementation builds the menu with `Recipe.copy(basicDishFor = type)`. `copy()` is shallow,
 * so the menu entry shares the ingredient map of recipe 2, while the incident is handed both
 * objects by `IncidentParser.collectAllActiveRecipes`. The +10% is then applied twice
 * (200 -> 220 -> 242) and the restaurant procures 242 g instead of 220 g.
 */
class AdaptedBasicDishRecipeChangeSystemTest : BasicDishSystemTest() {
    override val name = "AdaptedBasicDishRecipeChangeSystemTest"
    override val description = "A RECIPE incident is applied exactly once to an adapted basic dish"
    override val restaurants = "$DIR/restaurants.json"
    override val scenario = "$DIR/scenarioRecipeIncident.json"

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, ADAPTED_ONCE, UNIT, FLOUR))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }

    private companion object {
        /** 200 g raised by 10% exactly once (a second application would give 242 g) */
        const val ADAPTED_ONCE = 220
    }
}

/**
 * Isolates the defect to the restaurant that adapts a basic dish.
 *
 * Restaurant 1 (EUROPEAN) owns the adapted "Rice Bread" (recipe 2, 200 g), so its menu is that
 * recipe alone and the incident has to leave it at 220 g. Restaurant 2 (EUROPEAN) owns only
 * "Side Salad" (recipe 3, 300 g), so it additionally offers the default basic dish it did not
 * override (recipe 1, 100 g), giving 330 + 110 = 440 g.
 *
 * Only restaurant 1 passes a recipe through `Recipe.copy()`, so in our implementation only its
 * amount is wrong (242 instead of 220) while restaurant 2 already matches.
 */
class AdaptedBasicDishOnlyAdapterAffectedSystemTest : BasicDishSystemTest() {
    override val name = "AdaptedBasicDishOnlyAdapterAffectedSystemTest"
    override val description = "A RECIPE incident only mis-scales the restaurant that adapts a basic dish"
    override val restaurants = "$DIR/restaurantsTwo.json"
    override val scenario = "$DIR/scenarioRecipeIncident.json"

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, ADAPTER_PROCURED, UNIT, FLOUR))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, PLAIN_PROCURED, UNIT, FLOUR))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))
    }

    private companion object {
        /** the adapted "Rice Bread" alone: 200 g + 10% */
        const val ADAPTER_PROCURED = 220

        /** "Side Salad" plus the default "Rice Bread" this restaurant did not override: 330 + 110 */
        const val PLAIN_PROCURED = 440
    }
}

/**
 * The menu semantics that have to survive any fix of the defect above, checked without an incident
 * so that the numbers are the plain recipe amounts.
 *
 * Restaurant 1 adapted "Rice Bread", so it serves the adapted recipe (200 g) and not the 100 g
 * default. Restaurant 2 did not, so it serves "Side Salad" (300 g) plus the default "Rice Bread"
 * (100 g). Both our implementation and the reference are expected to pass this one today; it is
 * here to catch a fix that drops the adaptation or stops adding the default basic dishes.
 */
class BasicDishMenuCompositionSystemTest : BasicDishSystemTest() {
    override val name = "BasicDishMenuCompositionSystemTest"
    override val description = "An adapted basic dish replaces the default one, other basic dishes are still offered"
    override val restaurants = "$DIR/restaurantsTwo.json"
    override val scenario = "$DIR/scenarioEmpty.json"

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, ADAPTER_PROCURED, UNIT, FLOUR))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, PLAIN_PROCURED, UNIT, FLOUR))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))
    }

    private companion object {
        /** the adapted "Rice Bread" alone */
        const val ADAPTER_PROCURED = 200

        /** "Side Salad" plus the default "Rice Bread" */
        const val PLAIN_PROCURED = 400
    }
}
