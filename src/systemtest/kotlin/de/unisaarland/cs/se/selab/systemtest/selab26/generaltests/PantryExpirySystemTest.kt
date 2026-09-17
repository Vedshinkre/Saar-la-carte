package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Verifies that a package is thrown away exactly on its best-before evening (not one
 * evening early or late), and only then re-procured -- catches Pantry mutants around
 * [throwExpiredIngredients][de.unisaarland.cs.se.selab.restaurant.Pantry.throwExpiredIngredients]
 * and its ordering relative to procurement.
 */
class PantryExpirySystemTest : ExampleSystemTestExtension() {
    override val name = "PantryExpirySystemTest"
    override val description = "Verifies an expired package is thrown away and re-procured on the right evening"
    override val restaurants = "simplejson/restaurants.json"
    override val scenario = "pantryexpiry/scenario.json"
    override val food = "simplejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = THREE_EVENINGS

    override suspend fun run() {
        // Evening 1: pantry starts empty, both ingredients bought fresh.
        // Tomato's bestBefore is 2, so this package's expiryDate is evening 3.
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 500, UNIT, CHICKEN))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, UNIT, TOMATO))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        // Evening 2: the evening-1 package hasn't expired yet (evening 2 < expiryDate 3) and
        // still comfortably covers the 50g/250g requirements, so nothing new is bought.
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        // Evening 3: the evening-1 Tomato package has now reached its best-before date and
        // must be thrown away *before* a fresh package is bought to cover the same requirement.
        // Chicken's bestBefore is 3, so that package survives untouched.
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 100, UNIT, TOMATO))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, UNIT, TOMATO))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }

    private companion object {
        const val THREE_EVENINGS = 3 * 24
        const val UNIT = "g"
        const val CHICKEN = "Chicken"
        const val TOMATO = "Tomato"
    }
}
