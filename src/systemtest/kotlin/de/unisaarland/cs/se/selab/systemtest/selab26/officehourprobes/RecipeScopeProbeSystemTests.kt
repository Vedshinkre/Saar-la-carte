package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val UNIT = "g"
private const val CHICKEN = "chicken"

/**
 * Narrowing probes (Sep 22) for RecipeChangeAcrossRestaurantsTest, which at the time passed on our
 * implementation and failed on the reference. That test asserts both restaurants' procurement in
 * one run, so its failure could not say which line differs. Here each restaurant's line is its own
 * A/B pair. The reference chose reading A for both (runs 6-12).
 *
 * Recipe 1 is the ASIAN basic dish (100 g of chicken). Recipe 2 has the same dish name without
 * being a basic dish (200 g). Restaurant 1 lists only recipe 1 and restaurant 2 only recipe 2. Both
 * are ASIAN with ten free seats (1 meal of each dish on the menu). A RECIPE incident raises chicken
 * by 5% before evening 1. Chicken comes in 10 g packages.
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
 * Premise: the files are accepted, so the procurement probes below mean something. A recipe that
 * reuses the name of its type's basic dish without `basicDishFor` is an adaptation (forum topic
 * 78). The simulation only starts once all three files are valid, so the first incident line after
 * the start is enough.
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
 * Restaurant 1, reading A (the reference's): 100 g raised to 105 g, bought as whole 10 g packages,
 * 110 g.
 */
class RecipeScopeRestaurantOneBuysWholePackagesSystemTest : RecipeScopeScenario() {
    override val name = "RecipeScopeRestaurantOneBuysWholePackagesSystemTest"
    override val description = "Restaurant 1 procures 110 g, the whole packages that cover 105 g"

    override suspend fun run() = assertProcured(1, 110)
}

/** Restaurant 1, reading B (rejected, fails by design): the changed 105 g are bought as they are. */
class RecipeScopeRestaurantOneBuysExactAmountSystemTest : RecipeScopeScenario() {
    override val name = "RecipeScopeRestaurantOneBuysExactAmountSystemTest"
    override val description = "Restaurant 1 procures exactly the 105 g the changed recipe needs"

    override suspend fun run() = assertProcured(1, 105)
}

/**
 * Restaurant 2, reading A (the reference's, as in forum topic 78): the adapted recipe replaces the
 * basic dish on the menu, so one dish of 200 g, raised to 210 g.
 */
class RecipeScopeAdaptedDishReplacesBasicSystemTest : RecipeScopeScenario() {
    override val name = "RecipeScopeAdaptedDishReplacesBasicSystemTest"
    override val description = "The adapted dish replaces the basic dish, so restaurant 2 plans one meal"

    override suspend fun run() = assertProcured(2, 210)
}

/**
 * Restaurant 2, reading B (rejected, fails by design): the basic dish stays on the menu next to
 * the adapted one, so 210 g + 105 g = 315 g, bought as 320 g.
 */
class RecipeScopeAdaptedDishAddsToBasicSystemTest : RecipeScopeScenario() {
    override val name = "RecipeScopeAdaptedDishAddsToBasicSystemTest"
    override val description = "The basic dish stays next to the adapted dish, so restaurant 2 plans two meals"

    override suspend fun run() = assertProcured(2, 320)
}
