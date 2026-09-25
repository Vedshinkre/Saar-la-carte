package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Verifies the "must remain positive" half of the RECIPE incident rule (specification page 26,
 * lines 19-21, forum topic 188): the scaled amount is rounded down, but an amount that would land
 * on 0 stays at 1. Written to cover the lower boundary, since [RecipeChangeRoundingSystemTest]
 * only covers drops that stay well above 1.
 *
 * The one dish of the restaurant uses four ingredients, each hit by its own incident, and all of
 * them are sold in 1 g packages. The restaurant has 10 free seats, so the estimate multiplier is 1
 * and the amount procured for the empty pantry is exactly the amount left in the recipe:
 *  - Barley 1 g at -50%: 0.5 rounds down to 0, so the amount has to be held at 1.
 *  - Cocoa 4 g at -99%: 0.04 rounds down to 0, held at 1 as well.
 *  - Durum 100 g at -100%: the whole amount is taken away, still held at 1.
 *  - Emmer 100 g at -50%: 50, the contrast case that is not held at anything.
 *
 * Without the lower bound the first three amounts would be 0, which procures nothing at all and
 * drops their Pantry lines from the log entirely.
 */
class RecipeChangeMinimumAmountSystemTest : ExampleSystemTestExtension() {
    override val name = "RecipeChangeMinimumAmountSystemTest"
    override val description = "Verifies a RECIPE incident never takes an ingredient amount below 1"
    override val restaurants = "recipechangeclampjson/restaurants.json"
    override val scenario = "recipechangeclampjson/scenario.json"
    override val food = "recipechangeclampjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.incident(2, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.incident(3, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.incident(4, RECIPE, 1))

        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, HELD_AT_MINIMUM, UNIT, "Barley"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, HELD_AT_MINIMUM, UNIT, "Cocoa"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, HELD_AT_MINIMUM, UNIT, "Durum"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, EMMER_PROCURED, UNIT, "Emmer"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }

    private companion object {
        const val RECIPE = "RECIPE"
        const val UNIT = "g"

        /** an amount that would be scaled to 0 stays at 1 */
        const val HELD_AT_MINIMUM = 1

        /** 100 g at -50%, the case that needs no lower bound */
        const val EMMER_PROCURED = 50
    }
}
