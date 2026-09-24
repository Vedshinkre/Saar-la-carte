package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "potato soup"

/**
 * for F15
 * casual groups, common tables 1 (8), 2 (6), 3 (3), 4 (3)
 *
 * group 1 (6) -> table 2, exact fit beats the lower id
 * group 2 (6) -> table 1, exactly 3/4 full is enough so no merging 3 + 4
 * group 3 (5) -> merges 3 + 4 into 3, gets served, eats and leaves
 */
class CasualTablesSystemTest : ExampleSystemTestExtension() {
    override val name = "CasualTableChoiceOrder"
    override val description = "CASUAL tables: exact fit, then three quarters, then merging"
    override val food = "anshtests/casualtables/food.json"
    override val restaurants = "anshtests/casualtables/restaurants.json"
    override val scenario = "anshtests/casualtables/scenario.json"
    override val logLevel = "INFO"
    override val maxTicks = 5

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))

        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 1, listOf(1)))

        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 3, listOf(3, 4), 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(2)))
        skipUntilString(FohServiceTestLogs.serving(1, 2, mapOf(SOUP to 5), 3, 0))

        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 5, 3, 3))
        assertNextLine(FohServiceTestLogs.escorting(1, 2, 5, 3, 3))
    }
}
