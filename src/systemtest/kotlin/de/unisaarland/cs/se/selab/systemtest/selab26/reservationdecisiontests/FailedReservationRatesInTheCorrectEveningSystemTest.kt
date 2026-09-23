package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "reservationdecisionjson/failedreservationevening"

/**
 * Probe for the mandatory "ReservedForMe" (RRR) test, after office-hour feedback that our failed-
 * reservation rating logs at the right TICK but the wrong EVENING. [FailedReservationRatesInFirstTickOfEveningSystemTest]
 * only ever uses evening 1, so a mislabeled evening number cannot show up there even if the tick
 * itself is correct. This scenario forces the failure onto evening 2 specifically: group 1 (period
 * 2, evenings 2, 4, ...) only competes for the table from evening 2 onward, so group 2 (period 1)
 * reserves it alone on evening 1 and only loses it - and fails - starting evening 2. The rating is
 * due in tick 1 of that evening. If the evening label is wrong, the "Tick 1 (?)" marker before the
 * rating will show evening 1 or 3, not 2.
 */
class FailedReservationRatesInTheCorrectEveningSystemTest : ExampleSystemTestExtension() {
    override val name = "FailedReservationRatesInTheCorrectEveningSystemTest"
    override val description = "A failed reservation's rating on evening 2 is logged under evening 2, not 1 or 3"
    override val logLevel = "DEBUG"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"

    /** first tick of evening 3: (3 - 1) * 24 + 1 */
    override val maxTicks = 49

    override suspend fun run() {
        // evening 1: group 2 alone, reservation succeeds - nothing to assert, just skip past it
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))

        // evening 2: group 1 (lower id) now also competes and wins, group 2 fails
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 2))

        // the rating must appear tagged with evening 2, in tick 1 of the evening,
        // not evening 1 (the tick marker just skipped past) or evening 3 (the next one)
        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        skipUntilString(
            FohServiceTestLogs.rating(restId = 1, groupId = 2, rating = "NEGATIVE", pos = 1, neg = 1)
        )

        // confirms evening 2 has ended and evening 3 has not logged this rating a second time
        skipUntilString(TickStatusTestLogs.tickStart(1, 3))
    }
}
