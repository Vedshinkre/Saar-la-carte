package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Two restaurants tie on rating (3 positive, 1 negative each), type and free seats, so a CASUAL
 * group has to break the tie by the lowest restaurant id and choose restaurant 2.
 *
 * Restaurant 5 is listed first in the file, so "lowest id" and "first in the file" give different
 * answers. Added together with the fix of the tie-break comparator in BrowsingService, which had
 * picked the highest id (Sep 18), as its regression test.
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
