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
 * A RECIPE incident of +10% is applied exactly once to an adapted basic dish: restaurant 1's only
 * menu entry, the adapted "Rice Bread" (recipe 2, 200 g), must procure 220 g.
 *
 * Written on Sep 20 as the regression test for a defect it found in our implementation. The menu
 * entry was a shallow `Recipe.copy()` that shared its ingredient map with recipe 2, and the incident
 * was given both objects, so the +10% was applied twice (200 -> 220 -> 242 g). The defect was fixed
 * the same day. 242 is the value a double application gives.
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
 * The same +10% incident with a second restaurant that does not adapt the basic dish, to show the
 * double application was limited to the adapting restaurant.
 *
 * Restaurant 1 owns the adapted "Rice Bread" (200 g), so it must procure 220 g. Restaurant 2 owns
 * only "Side Salad" (300 g) and also offers the default "Rice Bread" (100 g) it did not override,
 * so it must procure 330 + 110 = 440 g. While the defect existed, only restaurant 1 was wrong
 * (242 g).
 */
class AdaptedBasicDishOnlyAdapterAffectedSystemTest : BasicDishSystemTest() {
    override val name = "AdaptedBasicDishOnlyAdapterAffectedSystemTest"
    override val description = "A RECIPE incident is applied once in the adapting and in the other restaurant"
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
 * The menu rules for basic dishes, checked without an incident so the numbers are the plain
 * recipe amounts (forum topic 78).
 *
 * Restaurant 1 adapted "Rice Bread", so it offers the adapted recipe (200 g) instead of the 100 g
 * default. Restaurant 2 did not, so it offers "Side Salad" (300 g) plus the default "Rice Bread"
 * (100 g), 400 g in total. Written with the two tests above to catch a fix of the defect that
 * drops the adaptation or stops adding the default basic dishes.
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
