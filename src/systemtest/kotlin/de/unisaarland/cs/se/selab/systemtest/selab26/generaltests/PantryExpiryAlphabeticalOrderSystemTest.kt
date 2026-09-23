package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * expired-ingredient removal logs must be alphabetic. [PantryExpirySystemTest] only
 * expires one ingredient at a time, so it can't distinguish that from insertion order; here three
 * expire together, using three ingredients (Apple, Mango, Zucchini) whose declaration order in
 * food.json ("Zucchini", "Apple", "Mango"), recipe-id order (Mango=1, Zucchini=2, Apple=3), and
 * packagingVolume order (Apple 700 > Zucchini 300 > Mango 150) are each a *different* permutation
 * from alphabetic and from each other. With only two ingredients a single coin-flip could
 * coincidentally reproduce alphabetic order (e.g. by matching declaration or id order instead);
 * with three mutually-scrambled orderings, only genuine alphabetic sorting satisfies all evenings.
 *
 * Caveat: an unconfirmed forum thread claims the real reference uses insertion order instead.
 */
class PantryExpiryAlphabeticalOrderSystemTest : ExampleSystemTestExtension() {
    override val name = "PantryExpiryAlphabeticalOrderSystemTest"
    override val description = "Three ingredients expiring in the same tick are removed in ascending alphabetic order"
    override val restaurants = "pantryexpiryalphabetical/restaurants.json"
    override val scenario = "pantryexpiryalphabetical/scenario.json"
    override val food = "pantryexpiryalphabetical/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = THREE_EVENINGS

    override suspend fun run() {
        // Evening 1: all three bought fresh (procurement is already, separately, alphabetical)
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, APPLE_AMOUNT, UNIT, APPLE))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, MANGO_AMOUNT, UNIT, MANGO))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, ZUCCHINI_AMOUNT, UNIT, ZUCCHINI))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        // Evening 2: none of the packages have expired yet (bestBefore 2 -> expiryDate evening 3)
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        // Evening 3: all three expire in the same step - removal must list Apple, Mango, then
        // Zucchini, matching none of the declaration, recipe-id, or packagingVolume orderings.
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, APPLE_AMOUNT, UNIT, APPLE))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, MANGO_AMOUNT, UNIT, MANGO))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, ZUCCHINI_AMOUNT, UNIT, ZUCCHINI))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, APPLE_AMOUNT, UNIT, APPLE))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, MANGO_AMOUNT, UNIT, MANGO))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, ZUCCHINI_AMOUNT, UNIT, ZUCCHINI))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }

    private companion object {
        const val THREE_EVENINGS = 3 * 24
        const val UNIT = "g"
        const val APPLE = "Apple"
        const val MANGO = "Mango"
        const val ZUCCHINI = "Zucchini"
        const val APPLE_AMOUNT = 700
        const val MANGO_AMOUNT = 150
        const val ZUCCHINI_AMOUNT = 300
    }
}
