package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Two restaurants tied on positive-minus-negative rating (both id-eligible, same type,
 * same seats) - F25: the CASUAL group's browsing decision must break the tie by choosing
 * the LOWEST restaurant id (restaurant 2), not the highest. This is a regression test for
 * a sign bug in BrowsingService's tie-break comparator that picked the highest id instead.
 */
class CasualTiebreakLowestIdTest : ExampleSystemTestExtension() {
    override val name = "CasualTiebreakLowestIdTest"
    override val description = "A rating tie between restaurants is broken by the lowest id"
    override val restaurants = "casualtiebreaklowestidjson/restaurants.json"
    override val scenario = "casualtiebreaklowestidjson/scenario.json"
    override val food = "casualtiebreaklowestidjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 2))
    }
}
