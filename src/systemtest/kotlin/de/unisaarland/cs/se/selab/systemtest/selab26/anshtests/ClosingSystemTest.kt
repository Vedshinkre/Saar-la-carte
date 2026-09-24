package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val NEGATIVE = "NEGATIVE"

/**
 * for F30
 * open ticks 3 to 8, one exec cook
 *
 * tick 5: regular groups 1 (stew, 2 ticks) and 2 (roast, 3 ticks) order
 * stew served tick 7, group 1 still eating at closing (tick 8)
 * roast only starts in tick 8 -> thrown away
 *
 * both leave without escorting and rate negative
 * 100 g rice from evening 1: -20 g stew, -20 g reserved for roast, 60 g left covers the 30 g for evening 2
 */
class ClosingSystemTest : ExampleSystemTestExtension() {
    override val name = "EndOfOpeningTime"
    override val description = "Logs outside the opening time, closing with guests inside, pantry kept"
    override val food = "anshtests/closing/food.json"
    override val restaurants = "anshtests/closing/restaurants.json"
    override val scenario = "anshtests/closing/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 25

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.servingStart(1))
        for (tick in 1..2) {
            assertNextLine(TickStatusTestLogs.tickStart(tick, 1))
            assertNextLine(TickStatusTestLogs.restStart(1))
            assertNextLine(TickStatusTestLogs.restEnd(1))
        }

        skipUntilString(TickStatusTestLogs.tickStart(8, 1))
        skipUntilString(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 2, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.rating(1, 1, NEGATIVE, 0, 1))
        assertNextLine(FohServiceTestLogs.rating(1, 2, NEGATIVE, 0, 2))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 2))
        assertNextLine(TickStatusTestLogs.restEnd(1))

        for (tick in 9..24) {
            assertNextLine(TickStatusTestLogs.tickStart(tick, 1))
            assertNextLine(TickStatusTestLogs.restStart(1))
            assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
            assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
            assertNextLine(TickStatusTestLogs.restEnd(1))
        }
        assertNextLine(TickStatusTestLogs.servingEnd(1))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        skipUntilString(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, 2))
    }
}
