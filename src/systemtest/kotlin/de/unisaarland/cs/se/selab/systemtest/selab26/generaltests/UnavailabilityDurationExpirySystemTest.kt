package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Verifies that an ingredient marked unavailable by an incident becomes procurable
 * again exactly once its duration has elapsed -- regression test for the bug where
 * [de.unisaarland.cs.se.selab.food.Stock.applyUnavailableDurations] was never called,
 * which left unavailable ingredients unavailable for the rest of the simulation.
 * Also exercises Tomato's independent best-before expiry in the same run, to catch
 * Pantry mutants that could make the two mechanisms interfere with each other.
 */
class UnavailabilityDurationExpirySystemTest : ExampleSystemTestExtension() {
    override val name = "UnavailabilityDurationExpirySystemTest"
    override val description = "Verifies an unavailable ingredient becomes procurable again after its duration"
    override val restaurants = "simplejson/restaurants.json"
    override val scenario = "simplejsonsupplierunavailable/scenario.json"
    override val food = "simplejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = THREE_EVENINGS

    override suspend fun run() {
        // Evening 1: the incident (duration 2, starting evening 1) makes Chicken unavailable
        // for this evening and the next; Tomato is unaffected and bought fresh (expiryDate 3).
        skipUntilString(InitialAndPrepTestLogs.incident(1, "UNAVAILABLE", 1))
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        // Chicken is unavailable, so nothing is procured for it -- no log line at all,
        // per spec: the log only describes ingredients that *were* procured.
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, UNIT, TOMATO))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        // Evening 2: one evening has passed -> duration is now 1, still unavailable.
        // Tomato's evening-1 package hasn't expired yet, so nothing is bought for either ingredient.
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        // Evening 3: duration has now reached 0 -> Chicken is procurable again, buying a full
        // package for the still-unmet requirement from evenings 1-2. Independently, Tomato's
        // evening-1 package has now reached its best-before date and is thrown away and rebought.
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 100, UNIT, TOMATO))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 500, UNIT, CHICKEN))
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
