package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Tests that a failed reservation's NEGATIVE rating waits for the restaurant's own opening tick.
 */
class FailedReservationRatesAtFirstOpeningTickSystemTest : ExampleSystemTestExtension() {
    override val name = "FailedReservationRatesAtFirstOpeningTickSystemTest"
    override val description = "A failed reservation's NEGATIVE rating waits for the restaurant's own opening tick"
    override val logLevel = "DEBUG"
    override val restaurants = "reservationdecisionjson/failedreservationratingtiming/restaurants.json"
    override val food = "reservationdecisionjson/failedreservationratingtiming/food.json"
    override val scenario = "reservationdecisionjson/failedreservationratingtiming/scenario.json"
    override val maxTicks = 5

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 1))

        // ticks 1-4: before openingTickStart, nothing may log between Start and End -
        // in particular, no rating for the already-failed reservation
        for (tick in 1..4) {
            skipUntilString(TickStatusTestLogs.tickStart(tick, 1))
            assertNextLine(TickStatusTestLogs.restStart(1))
            assertNextLine(TickStatusTestLogs.restEnd(1))
        }

        // tick 5, the restaurant's first opening tick: the pending rating is finally processed
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        skipUntilString(FohServiceTestLogs.rating(restId = 1, groupId = 1, rating = "NEGATIVE", pos = 0, neg = 1))
        assertNextLine(FohServiceTestLogs.ratingStatus(restId = 1, groups = 1))
    }
}
