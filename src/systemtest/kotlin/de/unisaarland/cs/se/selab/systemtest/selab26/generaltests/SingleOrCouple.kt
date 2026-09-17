package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * if 75 capacity rule can be preserved, we preserve it.
 */
class SingleOrCouple : ExampleSystemTestExtension() {
    override val name = "RegularTableMergeFallbackTest"
    override val description = "Checks table merging priority rules for reservations."

    // Using your custom files
    override val restaurants = "regularmergejason/restaurants.json"
    override val scenario = "regularmergejason/scenario.json"
    override val food = "regularmergejason/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 24

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))

        // At Tick 2, the REGULAR group arrives.
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))

        // We assert they merge Tables 2 (size 2) and 3 (size 4) into a size 6 table, rejecting Table 1 (size 12).
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 1, listOf(2, 3), 2))

        // Assert they are successfully seated at the merged Table 2
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))
    }
}
