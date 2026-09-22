package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val MEAL_C = "mealC"

/**
 * A/B probe for what happens to a meal that was never cooked when its customer walks out.
 *
 * Specification page 13 says such a meal is aborted and its ingredients go back to the pantry:
 * "It can happen that a meal in an order is aborted, e.g., when a customer has been waiting for too
 * long and leaves the restaurant". We implement exactly that, and drop the meal from the kitchen
 * queue. The reference appears not to: CorrectPartialServing2 asserts that mealC is still assigned
 * to a cook in tick 8, finished in tick 11 and counted in the statistics, and it passes there while
 * failing here.
 *
 * Same three customer group as the extended patience probes: mealA is served in tick 5, mealB in
 * tick 7, and the customer waiting for mealC walks out in tick 7 before the cook ever starts it.
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
 * Reading A: the meal stays in the kitchen queue and is cooked anyway, ten ticks after the order,
 * and counts towards the meals the restaurant cooked. This is what the reference seems to do.
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
 * Reading B: the meal is aborted with the customer, never reaches a cook, and the restaurant ends
 * the evening having cooked only the two meals it served. This is what we implement and what the
 * specification describes.
 */
class WalkedOutMealIsAbortedSystemTest : AbortedMealScenario() {
    override val name = "WalkedOutMealIsAbortedSystemTest"
    override val description = "The meal of a customer who walked out is aborted and never cooked"

    override suspend fun run() {
        skipToWalkOut()
        skipUntilString(StatisticsTestLogs.statsCooked(1, 2))
    }
}
