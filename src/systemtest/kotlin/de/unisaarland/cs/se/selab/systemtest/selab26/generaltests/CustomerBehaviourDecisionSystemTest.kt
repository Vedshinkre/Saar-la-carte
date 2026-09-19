package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Customer behaviour: the restaurant decisions of EVENT and CASUAL groups (dine-in and delivery).
 * Every decision below is chosen so that a different browsing rule (rating, tie-break, seat
 * bookkeeping, type filter, opening window, dietary filter, driver count, decision tick, decision
 * order) gives a different log line.
 *
 * Restaurants: 1 EUROPEAN (rating 0), 2 EUROPEAN (rating 4, 1 driver), 3 ASIAN (event, only dish uses
 * peanuts), 4 AMERICAN (open 1-6, so it accepts decisions only up to tick 3).
 */
class CustomerBehaviourDecisionSystemTest : ExampleSystemTestExtension() {
    override val name = "CustomerBehaviourDecisionSystemTest"
    override val description = "Restaurant decisions of EVENT and CASUAL groups follow the browsing rules"
    override val restaurants = "customerbehaviourjson/restaurants.json"
    override val scenario = "customerbehaviourjson/scenario.json"
    override val food = "customerbehaviourjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 6

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        // EVENT groups decide first, three evenings ahead
        assertNextLine(TickStatusTestLogs.restDecision(1, 3))
        // best rating wins and its only table is now booked
        assertNextLine(TickStatusTestLogs.restDecision(2, 2))
        // restaurant 2 is full, so the next group falls back to the remaining one
        assertNextLine(TickStatusTestLogs.restDecision(3, 1))
        // both EUROPEAN restaurants are full
        assertNextLine(TickStatusTestLogs.restNoDecision(4))
        // restaurant type filter
        assertNextLine(TickStatusTestLogs.restDecision(5, 3))
        // every dish of the only ASIAN restaurant contains an excluded ingredient
        assertNextLine(TickStatusTestLogs.restNoDecision(6))

        // last tick a restaurant with openingTickEnd 6 is still open for decisions
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(7, 4))
        // one tick later it is within the last 3 ticks and excluded
        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        assertNextLine(TickStatusTestLogs.restNoDecision(8))

        // delivery for tick 10 and distance 7 is decided at 10 - ceil(7 / 5) - 3 = tick 5
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        assertNextLine(TickStatusTestLogs.restDecision(9, 2))
        // the only driver is taken and the other restaurant has none
        assertNextLine(TickStatusTestLogs.restNoDecision(10))
    }
}
