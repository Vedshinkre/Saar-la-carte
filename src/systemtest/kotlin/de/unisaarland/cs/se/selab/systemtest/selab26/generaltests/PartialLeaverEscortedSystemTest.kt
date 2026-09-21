package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A group of 3 orders three different dishes, which the only cook makes one after the other: the first is served in
 * tick 6, the second in tick 8 and the third is not ready when the group's patience (7 ticks after ordering, as one
 * meal was served) ends in tick 8, so that customer leaves. The two customers that were served finish eating in
 * ticks 8 and 10, and the waiter escorts them in tick 10, so the group has left completely and rates negatively (one
 * customer never got food). A group with a customer who left must not stay at its table for the rest of the evening.
 */
class PartialLeaverEscortedSystemTest : ExampleSystemTestExtension() {
    override val name = "PartialLeaverEscortedSystemTest"
    override val description = "The customers that were served are escorted after another customer of the group left"

    override val restaurants = "partialleaverescortjson/restaurants.json"
    override val food = "partialleaverescortjson/food.json"
    override val scenario = "partialleaverescortjson/scenario.json"

    override val logLevel = "INFO"
    override val maxTicks = 11

    override suspend fun run() {
        // Slow Roast is served alone in tick 6, Long Stew follows in tick 8 and Deep Pot is still not cooked
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf("Slow Roast" to 1), 1, 5))
        skipUntilString(TickStatusTestLogs.tickStart(8, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf("Long Stew" to 1), 1, 7))
        skipUntilString(FohServiceTestLogs.noEating(1, 1, 1, 1))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 1, 1, 1))

        // the second served customer finishes eating: everybody who is still at the table is escorted and rates
        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 1, 1, 1))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 2, 1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 1, "NEGATIVE", 10, 1))
    }
}
