package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val POSITIVE = "POSITIVE"
private const val NEGATIVE = "NEGATIVE"

/**
 * for F23
 * one exec cook
 *
 * tick 1: group 1 (some) orders stew, served 3 ticks later -> positive
 * cook is busy so the tea of groups 2 (always) and 3 (some) takes 4 ticks -> neutral
 *
 * tick 2: groups 4 (never) and 5 (some), 1 customer each, only a table of 4 -> sent away, negative
 * group 2 only visits on evenings 1 and 3
 */
class CasualVisitsSystemTest : ExampleSystemTestExtension() {
    override val name = "CasualRatingLikelihoodAndVisitingEvenings"
    override val description = "CASUAL groups rate by likelihood and experience and visit only on their evenings"
    override val food = "anshtests/casualvisits/food.json"
    override val restaurants = "anshtests/casualvisits/restaurants.json"
    override val scenario = "anshtests/casualvisits/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 49

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(FohArrivalTestLogs.noSeatingNoTable(1, 1, 4))
        assertNextLine(FohArrivalTestLogs.arrival(1, 5))
        assertNextLine(FohArrivalTestLogs.noSeatingNoTable(1, 1, 5))
        skipUntilString(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.rating(1, 5, NEGATIVE, 0, 1))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))

        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf("stew" to 4), 1, 3))
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf("tea" to 3), 2, 4))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf("tea" to 3), 3, 4))

        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(FohServiceTestLogs.escortingStatus(1, 1, 4))
        assertNextLine(FohServiceTestLogs.rating(1, 1, POSITIVE, 1, 1))

        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(FohServiceTestLogs.escortingStatus(1, 1, 6))
        assertNextLine(FohServiceTestLogs.rating(1, 2, POSITIVE, 2, 1))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))

        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        assertNextLine(TickStatusTestLogs.restStart(1))
        skipUntilString(TickStatusTestLogs.tickStart(1, 3))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
    }
}
