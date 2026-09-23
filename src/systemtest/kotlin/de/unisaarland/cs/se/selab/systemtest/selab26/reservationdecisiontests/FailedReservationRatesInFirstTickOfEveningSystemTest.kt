package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "reservationdecisionjson/failedreservationratingtiming"

/**
 * Probe for the mandatory "ReservedForMe" (RRR) test. The spec (Rating, p.22) has a failed
 * reservation rated "in the first tick of the evening", and the reference did not agree with our
 * earlier reading that the rating waits for the restaurant's own `openingTickStart`. Restaurant 1
 * opens at tick 5 and its only table is BAR while the REGULAR group wants COMMON, so the reservation
 * fails in preparation. The NEGATIVE rating is expected in tick 1, before the restaurant opens.
 */
class FailedReservationRatesInFirstTickOfEveningSystemTest : ExampleSystemTestExtension() {
    override val name = "FailedReservationRatesInFirstTickOfEveningSystemTest"
    override val description = "A failed reservation's NEGATIVE rating is logged in tick 1, not at the opening tick"
    override val logLevel = "DEBUG"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"
    override val maxTicks = 5

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 1))

        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohServiceTestLogs.rating(restId = 1, groupId = 1, rating = "NEGATIVE", pos = 0, neg = 1))
        assertNextLine(FohServiceTestLogs.ratingStatus(restId = 1, groups = 1))
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
    }
}
