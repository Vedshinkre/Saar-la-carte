package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Verifies that a RECIPE incident scales the recipe amounts exactly and rounds down, and leaves
 * other ingredients alone. The effect is observed through the amounts procured for an empty pantry
 * on evening 1 (both recipes use every ingredient, so the requirement is the sum over both recipes).
 *
 * Packaging volumes are chosen so that being off by one unit per recipe changes the number of
 * packages bought:
 *  - Flour +15%: 100 -> 115 (a double-based 1.15 * 100 gives 114.99..., i.e. 114). Need 230 with
 *    229 g packages -> 2 packages = 458 (a 114-per-recipe result needs 228 -> 1 package = 229).
 *  - Salt -93%: 100 -> 7 (double-based gives 6). Need 14 with 13 g packages -> 2 packages = 26
 *    (6 per recipe needs 12 -> 1 package = 13).
 *  - Rice +50%: 3 -> 4 (4.5 is rounded down, not up). Need 8 with 9 g packages -> 1 package = 9
 *    (rounding up would need 10 -> 2 packages = 18).
 *  - Sugar is not touched by any incident: need 200 with 150 g packages -> 2 packages = 300.
 */
class RecipeChangeRoundingSystemTest : ExampleSystemTestExtension() {
    override val name = "RecipeChangeRoundingSystemTest"
    override val description = "Verifies recipe change incidents adapt amounts exactly and round down"
    override val restaurants = "simplejson/restaurants.json"
    override val scenario = "recipechangeincident/scenario.json"
    override val food = "recipechangeincident/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.incident(2, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.incident(3, RECIPE, 1))

        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FLOUR_PROCURED, UNIT, "Flour"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, RICE_PROCURED, UNIT, "Rice"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SALT_PROCURED, UNIT, "Salt"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SUGAR_PROCURED, UNIT, "Sugar"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }

    private companion object {
        const val RECIPE = "RECIPE"
        const val UNIT = "g"
        const val FLOUR_PROCURED = 458
        const val RICE_PROCURED = 9
        const val SALT_PROCURED = 26
        const val SUGAR_PROCURED = 300
    }
}
