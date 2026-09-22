package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val UNIT = "g"
private const val CHICKEN = "chicken"

/**
 * Narrowing probes for RecipeChangeAcrossRestaurantsTest, which passes on our implementation but
 * fails against the reference, so one of its two procurement lines is not what the reference logs.
 * That test asserts both restaurants in one run, which cannot say which of the two diverges.
 *
 * The scenario: recipe 1 is the basic dish of ASIAN and needs 100 g of chicken, recipe 2 carries the
 * same dish name without being a basic dish and needs 200 g. Restaurant 1 lists only recipe 1,
 * restaurant 2 only recipe 2, both are ASIAN with ten seats, and a RECIPE incident raises chicken by
 * 5 % before evening 1. Chicken is sold in packages of 10 g.
 *
 * Ten free seats make the estimate ceil(10 / 10) = 1 meal of every dish on the menu.
 */
abstract class RecipeScopeScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/recipescope/restaurants.json"
    override val scenario = "officehourjson/recipescope/scenario.json"
    override val food = "officehourjson/recipescope/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    protected suspend fun assertProcured(restId: Int, amount: Int) {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(restId, amount, UNIT, CHICKEN))
    }
}

/**
 * First question: are the files accepted at all? A recipe that repeats the dish name of the basic
 * dish of its type without being one is how a restaurant adapts that basic dish, so the reference
 * should take the food file. If this probe fails, the whole scenario is rejected there and the
 * amounts below say nothing.
 */
class RecipeScopeFilesAcceptedSystemTest : RecipeScopeScenario() {
    override val name = "RecipeScopeFilesAcceptedSystemTest"
    override val description = "An adapted basic dish sharing the dish name of the basic dish is accepted"

    override suspend fun run() {
        // the simulation only starts once all three files validated, so this line is the signal
        skipUntilString(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.incident(1, "RECIPE", 1))
    }
}

/**
 * Restaurant 1, reading A: 100 g raised to 105 g, bought as 11 packages of 10 g. This is what we
 * procure, and the reading every other procurement test of ours agrees with.
 */
class RecipeScopeRestaurantOneBuysWholePackagesSystemTest : RecipeScopeScenario() {
    override val name = "RecipeScopeRestaurantOneBuysWholePackagesSystemTest"
    override val description = "Restaurant 1 procures 110 g, the whole packages that cover 105 g"

    override suspend fun run() = assertProcured(1, 110)
}

/** Restaurant 1, reading B: the changed amount is procured as it is, without whole packages. */
class RecipeScopeRestaurantOneBuysExactAmountSystemTest : RecipeScopeScenario() {
    override val name = "RecipeScopeRestaurantOneBuysExactAmountSystemTest"
    override val description = "Restaurant 1 procures exactly the 105 g the changed recipe needs"

    override suspend fun run() = assertProcured(1, 105)
}

/**
 * Restaurant 2, reading A: the adapted recipe 2 replaces the basic dish on the menu, so the menu is
 * one dish of 200 g, raised to 210 g, which is already a whole number of packages. This is ours.
 */
class RecipeScopeAdaptedDishReplacesBasicSystemTest : RecipeScopeScenario() {
    override val name = "RecipeScopeAdaptedDishReplacesBasicSystemTest"
    override val description = "The adapted dish replaces the basic dish, so restaurant 2 plans one meal"

    override suspend fun run() = assertProcured(2, 210)
}

/**
 * Restaurant 2, reading B: the basic dish of the type stays on the menu next to the adapted recipe,
 * so the menu is two dishes and the estimate plans one meal of each: 210 g plus 105 g is 315 g,
 * bought as 32 packages.
 */
class RecipeScopeAdaptedDishAddsToBasicSystemTest : RecipeScopeScenario() {
    override val name = "RecipeScopeAdaptedDishAddsToBasicSystemTest"
    override val description = "The basic dish stays next to the adapted dish, so restaurant 2 plans two meals"

    override suspend fun run() = assertProcured(2, 320)
}
