package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "reservationdecisionjson/partialservenotfailedattempt"
private const val MEAL_A = "mealA"
private const val MEAL_B = "mealB"
private const val MEAL_C = "mealC"

/**
 * Probe for the mandatory "ReservedForMe" (RRR) test. Per the forum thread "Regular Groups visit
 * 'failed attempts'", a "failed attempt" only applies to a whole-group negative experience; a
 * partially-served group (some fed, some time out) rates NEGATIVE but is not a failed attempt.
 *
 * Timing mirrors the validated `CorrectPartialServing1` fixture: order at tick 1, mealA served at
 * tick 5 (extends patience), mealB served and the third customer times out at tick 7. Group 1
 * repeats this on evenings 1 and 2, and must still be visiting on evening 3.
 */
class RegularPartiallyServedGroupKeepsVisitingSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularPartiallyServedGroupKeepsVisitingSystemTest"
    override val description =
        "Two consecutive partially-served (but not fully-unserved) evenings do not count as failed attempts (RRR probe)"
    override val logLevel = "DEBUG"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"

    /** first tick of evening 3: (3 - 1) * 24 + 1 */
    override val maxTicks = 49

    private suspend fun assertPartialServeEvening(evening: Int, orderId: Int, negativeRatingsSoFar: Int) {
        skipUntilString(TickStatusTestLogs.tickStart(1, evening))
        skipUntilString(FohArrivalTestLogs.arrival(restId = 1, groupId = 1))
        assertNextLine(FohArrivalTestLogs.seating(restId = 1, groupId = 1, tableId = 1, waitstaffIds = listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = orderId,
                dishes = mapOf(MEAL_A to 1, MEAL_B to 1, MEAL_C to 1),
                waitstaffId = 1
            )
        )

        skipUntilString(TickStatusTestLogs.tickStart(5, evening))
        skipUntilString(
            FohServiceTestLogs.serving(restId = 1, waitstaffId = 1, dishes = mapOf(MEAL_A to 1), tableId = 1, ticks = 4)
        )

        skipUntilString(TickStatusTestLogs.tickStart(7, evening))
        skipUntilString(
            FohServiceTestLogs.serving(restId = 1, waitstaffId = 1, dishes = mapOf(MEAL_B to 1), tableId = 1, ticks = 6)
        )
        // one customer never gets mealC: a negative, but only partial, experience
        skipUntilString(FohServiceTestLogs.noEating(restId = 1, customers = 1, groupId = 1, tableId = 1))

        // rates NEGATIVE, but must not count as a "failed attempt"
        skipUntilString(
            FohServiceTestLogs.rating(restId = 1, groupId = 1, rating = "NEGATIVE", pos = 0, neg = negativeRatingsSoFar)
        )
    }

    override suspend fun run() {
        // evenings 1 and 2: same partial failure, back to back
        assertPartialServeEvening(evening = 1, orderId = 1, negativeRatingsSoFar = 1)
        assertPartialServeEvening(evening = 2, orderId = 2, negativeRatingsSoFar = 2)

        // evening 3: group is still visiting, so neither evening counted as a failed attempt
        skipUntilString(TickStatusTestLogs.tickStart(1, 3))
        skipUntilString(FohArrivalTestLogs.arrival(restId = 1, groupId = 1))
        assertNextLine(FohArrivalTestLogs.seating(restId = 1, groupId = 1, tableId = 1, waitstaffIds = listOf(1)))
    }
}
