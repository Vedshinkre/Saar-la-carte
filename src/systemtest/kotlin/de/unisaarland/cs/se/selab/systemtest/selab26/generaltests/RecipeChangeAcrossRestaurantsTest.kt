package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension

/**
 * Tests that restaurants correctly scope only their assigned recipes
 * and that a global RecipeChangeIncident scales them independently.
 */
class RecipeChangeAcrossRestaurantsTest : ExampleSystemTestExtension() {
    override val name = "RecipeChangeAcrossRestaurantsTest"
    override val description = "Tests recipe changes per restaurant and independent incident adaptation"
    override val restaurants = "recipeincidentjson/restaurants.json"
    override val scenario = "recipeincidentjson/scenario.json"
    override val food = "recipeincidentjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        // Incident occurs before evening 1 starts
        skipUntilString("[IMPORTANT] Incident: Incident 1 of type RECIPE occurred before evening 1.")

        // Restaurant 1 Preparation: Uses Recipe 1 (100g -> 105g with 5% adaptation)
        skipUntilString("[IMPORTANT] Preparation: Preparation for evening 1 starts.")
        assertNextLine("[DEBUG] Pantry (R 1): Procured 105 g of chicken from the supplier.")
        assertNextLine("[INFO] Pantry (R 1): Restocked ingredients.")

        // Restaurant 2 Preparation: Uses Recipe 2 (200g -> 210g with 5% adaptation)
        skipUntilString("[IMPORTANT] Preparation: Preparation for evening 1 starts.")
        skipUntilString("[DEBUG] Pantry (R 2): Procured 210 g of chicken from the supplier.")
        assertNextLine("[INFO] Pantry (R 2): Restocked ingredients.")
    }
}
