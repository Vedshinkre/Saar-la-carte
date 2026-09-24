package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val MEAL_C = "mealC"

/**
 * A/B pair (Sep 22): what happens to a meal that is not yet cooked when its customer walks out.
 * Specification page 13 says such a meal can be aborted when the customer "leaves the restaurant".
 * Specification adjustment #21 says the kitchen keeps cooking for customers who left. Written after
 * CorrectPartialServing2 (mealC still cooked in tick 11) passed on the reference and failed on our
 * implementation, which aborted the meal. The reference chose reading A (runs 7-12), and our
 * implementation was changed to match.
 *
 * Same table of three as the partial-serving probes: the customer waiting for mealC walks out in
 * tick 7, before a cook has started mealC.
 */
abstract class AbortedMealScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/partialserving/restaurants.json"
    override val scenario = "officehourjson/partialserving/scenario.json"
    override val food = "officehourjson/partialserving/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    /** the walk-out both readings agree on, so a failure here is not about the meal */
    protected suspend fun skipToWalkOut() {
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 1, 1, 1))
    }
}

/**
 * Reading A (the reference's): mealC stays in the kitchen queue, is cooked ten ticks after the
 * order, and counts in the statistics (3 meals cooked).
 */
class WalkedOutMealIsStillCookedSystemTest : AbortedMealScenario() {
    override val name = "WalkedOutMealIsStillCookedSystemTest"
    override val description = "The meal of a customer who walked out is still cooked and counted"

    override suspend fun run() {
        skipToWalkOut()
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 1, 1, MEAL_C, 10))
        skipUntilString(StatisticsTestLogs.statsCooked(1, 3))
    }
}

/**
 * Reading B (rejected by the reference, fails by design): mealC is aborted with its customer, so
 * only 2 meals are cooked. This is the literal reading of page 13 that adjustment #21 overrides.
 */
class WalkedOutMealIsAbortedSystemTest : AbortedMealScenario() {
    override val name = "WalkedOutMealIsAbortedSystemTest"
    override val description = "The meal of a customer who walked out is aborted and never cooked"

    override suspend fun run() {
        skipToWalkOut()
        skipUntilString(StatisticsTestLogs.statsCooked(1, 2))
    }
}
