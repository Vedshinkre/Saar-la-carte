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

        // Preparation phase starts once for evening 1
        skipUntilString("[IMPORTANT] Preparation: Preparation for evening 1 starts.")

        // Restaurant 1: 105g rounded up to packaging volume (10) = 110g
        assertNextLine("[DEBUG] Pantry (R 1): Procured 110 g of chicken from the supplier.")
        assertNextLine("[INFO] Pantry (R 1): Restocked ingredients.")

        // Restaurant 2: 210g rounded up to packaging volume (10) = 210g
        assertNextLine("[DEBUG] Pantry (R 2): Procured 210 g of chicken from the supplier.")
        assertNextLine("[INFO] Pantry (R 2): Restocked ingredients.")
    }
}
