package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val MEAL_A = "mealA"
private const val MEAL_B = "mealB"

/**
 * F27: a group of 3 orders three different dishes (mealA duration 30, mealB and mealC duration 40 each) on a
 * single TOURNANT cook, so they finish one after another and the table is never complete. This reuses the
 * shared "officehourjson/partialserving" fixture, whose exact timings are cross-checked by
 * [de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.PartialServingWaitsForTwoTicksSystemTest] and
 * [de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.ExtendedPatienceLeavesInTickSevenSystemTest]
 * (the only one of five candidate ticks that passes against our own implementation).
 *
 * This replaces the previous version of this test, which assumed the base patience window was still 5 ticks
 * (basePatience is actually 4: the ordering tick counts as the first of the five, per spec adjustment /
 * [de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.PatienceEndsFourTicksAfterOrderingSystemTest]).
 * Under the old assumption mealA (cooked in tick 3, served in tick 5) would have been served exactly when the
 * whole group's base patience ran out, at which point nobody had been served yet by the old counting and the
 * whole group would incorrectly walk out. Confirmed against our own jar:
 *
 * - mealA is cooked in tick 3 (2 ticks after ordering in tick 1) and served in tick 5 (4 ticks after ordering,
 *   blocked for the cooking tick and the following one).
 * - mealB is cooked and served in tick 7 (6 ticks after ordering): this is also the tick the third customer's
 *   patience runs out (base 4 ticks, extended by 2 more once mealA was served in time), so that customer
 *   leaves unserved in the same tick mealB is served.
 * - mealA's customer finishes eating in tick 7 (served tick 5, eats ticks 5 and 6). mealB's customer finishes
 *   eating in tick 9 (served tick 7, eats ticks 7 and 8). Both are then escorted together in tick 9, and the
 *   group rates negatively because one customer never received food.
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
