package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val MEAL_A = "mealA"
private const val MEAL_B = "mealB"

/**
 * F27: a group of 3 orders three different dishes on a single TOURNANT cook, so they finish one after
 * another and the table is never complete. Reuses the "officehourjson/partialserving" fixture.
 *
 * mealA cooks in tick 3, serves in tick 5 (blocked for the cooking tick and the next). mealB cooks and
 * serves in tick 7 -- the same tick the third customer's patience runs out (base 4 ticks, +2 once mealA
 * was served in time), so that customer leaves unserved as mealB is served. mealA's customer finishes
 * eating tick 7, mealB's tick 9; both are escorted together in tick 9, and the group rates negative since
 * one customer never got food.
 */
class PartialLeaverEscortedSystemTest : ExampleSystemTestExtension() {
    override val name = "PartialLeaverEscortedSystemTest"
    override val description = "The customers that were served are escorted after another customer of the group left"

    override val restaurants = "officehourjson/partialserving/restaurants.json"
    override val food = "officehourjson/partialserving/food.json"
    override val scenario = "officehourjson/partialserving/scenario.json"

    override val logLevel = "INFO"
    override val maxTicks = 12

    override suspend fun run() {
        // mealA is served alone in tick 5
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(MEAL_A to 1), 1, 4))

        // mealB is served and the third customer (mealC, still uncooked) leaves, both in tick 7
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(MEAL_B to 1), 1, 6))
        skipUntilString(FohServiceTestLogs.noEating(1, 1, 1, 1))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 1, 1, 1))

        // the second served customer finishes eating: everybody still at the table is escorted and rates
        skipUntilString(TickStatusTestLogs.tickStart(9, 1))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 1, 1, 1))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 2, 1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 1, "NEGATIVE", 5, 1))
    }
}
