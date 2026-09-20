package de.unisaarland.cs.se.selab.systemtest.selab26.recipeincidenttests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val DIR = "recipeprobejson"
private const val ONE_TABLE = "$DIR/restaurantsOneTable.json"
private const val UNIT = "g"
private const val RECIPE = "RECIPE"
private const val TWO_EVENINGS = 48

private fun inDir(file: String) = "$DIR/$file"

/**
 * Probes for the RECIPE incident (specification page 26, lines 12-21 and forum topic 188), one
 * spec statement per test, all observed through the amounts procured in the preparation phase.
 *
 * Every ingredient is sold in 1 g packages, so the procured amount equals the planned amount
 * exactly, and every restaurant has 10 seats that are not reserved, so the per-dish estimate
 * (ceil(otherSeats / 10), specification page 12) is 1.
 */
abstract class RecipeChangeProbeSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val restaurants = ONE_TABLE
}

/**
 * Two RECIPE incidents on the same ingredient in the same evening are applied in ascending order
 * of incident id, and each one rounds down on its own.
 *
 * Cumin starts at 7 g. In id order: +50% gives 10.5 -> 10, then -30% gives 7.0 -> 7. In the other
 * order the flooring lands elsewhere: -30% gives 4.9 -> 4, then +50% gives 6.0 -> 6.
 */
class RecipeChangeIncidentOrderSystemTest : RecipeChangeProbeSystemTest() {
    override val name = "RecipeChangeIncidentOrderSystemTest"
    override val description = "Two RECIPE incidents of one evening are applied in ascending id order"
    override val food = inDir("orderFood.json")
    override val scenario = inDir("orderScenario.json")
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.incident(1, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.incident(2, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, IN_ID_ORDER, UNIT, "cumin"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }

    private companion object {
        /** 7 -> 10 -> 7; the reverse order would give 6 */
        const val IN_ID_ORDER = 7
    }
}

/**
 * A RECIPE incident changes the recipe permanently, so a second incident on a later evening
 * compounds on the already changed amount rather than on the original one.
 *
 * Cocoa starts at 100 g. Evening 1: +10% gives 110, procured into an empty pantry. Evening 2:
 * +10% on 110 gives 121, and the pantry still holds the 110 from evening 1, so only 11 are bought.
 * Without persistence the evening 2 requirement would still be 110 and nothing would be procured.
 */
class RecipeChangePersistsAcrossEveningsSystemTest : RecipeChangeProbeSystemTest() {
    override val name = "RecipeChangePersistsAcrossEveningsSystemTest"
    override val description = "A second RECIPE incident compounds on the already changed amount"
    override val food = inDir("persistFood.json")
    override val scenario = inDir("persistScenario.json")
    override val maxTicks = TWO_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING, UNIT, "cocoa"))

        skipUntilString(InitialAndPrepTestLogs.incident(2, RECIPE, 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SECOND_EVENING, UNIT, "cocoa"))
    }

    private companion object {
        /** 100 g + 10% */
        const val FIRST_EVENING = 110

        /** 121 required, 110 already in the pantry */
        const val SECOND_EVENING = 11
    }
}

/**
 * An incident scheduled for a later evening is neither logged nor applied before that evening.
 *
 * Basil starts at 100 g and the incident belongs to evening 2, so evening 1 procures the original
 * 100 g and no incident line may appear before its preparation. Evening 2 raises the requirement
 * to 150 g and buys the missing 50 g.
 */
class RecipeChangeLaterEveningSystemTest : RecipeChangeProbeSystemTest() {
    override val name = "RecipeChangeLaterEveningSystemTest"
    override val description = "A RECIPE incident of evening 2 leaves evening 1 untouched"
    override val food = inDir("laterFood.json")
    override val scenario = inDir("laterScenario.json")
    override val maxTicks = TWO_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.SIM_START)
        // no incident line in between: the incident belongs to evening 2
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, UNCHANGED, UNIT, "basil"))

        skipUntilString(InitialAndPrepTestLogs.incident(1, RECIPE, 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, TOP_UP, UNIT, "basil"))
    }

    private companion object {
        const val UNCHANGED = 100

        /** 150 required after the incident, 100 already in the pantry */
        const val TOP_UP = 50
    }
}

/**
 * One RECIPE incident changes the named ingredient in every recipe that uses it, each recipe
 * rounding on its own, and leaves every other ingredient alone.
 *
 * Alpha at -30%: 40 g -> 28 in the first recipe and 7 g -> 4.9 -> 4 in the second, so 32 g are
 * planned in total. Beta is not named by the incident and stays at 100 g.
 */
class RecipeChangeScopeSystemTest : RecipeChangeProbeSystemTest() {
    override val name = "RecipeChangeScopeSystemTest"
    override val description = "A RECIPE incident changes every recipe using the ingredient and no other"
    override val food = inDir("scopeFood.json")
    override val restaurants = inDir("scopeRestaurants.json")
    override val scenario = inDir("scopeScenario.json")
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, RECIPE, 1))
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, ALPHA_TOTAL, UNIT, "alpha"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, BETA_TOTAL, UNIT, "beta"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }

    private companion object {
        /** 40 -> 28 plus 7 -> 4 */
        const val ALPHA_TOTAL = 32

        /** untouched by the incident */
        const val BETA_TOTAL = 100
    }
}
