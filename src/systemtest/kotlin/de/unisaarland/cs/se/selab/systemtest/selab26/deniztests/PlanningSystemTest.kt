package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val G = "g"
private const val CARROT = "carrot"
private const val LETTUCE = "lettuce"
private const val ONION = "onion"

/** Planning sums ingredients per seat estimate, buys whole packages and reuses pantry leftovers. */
class PlanningSystemTest : ExampleSystemTestExtension() {
    override val name = "PlanningSystemTest"
    override val description = "Seat estimate, summed ingredients, whole packages and pantry leftovers"
    override val food = "deniztests/planning/food.json"
    override val restaurants = "deniztests/planning/restaurants.json"
    override val scenario = "deniztests/planning/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 25

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 40, G, CARROT))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 60, G, LETTUCE))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 30, G, ONION))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 40, G, CARROT))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 60, G, ONION))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))

        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("soup" to 1, "salad" to 1), 1))

        skipUntilString(TickStatusTestLogs.servingEnd(1))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 40, G, CARROT))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 40, G, LETTUCE))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 30, G, ONION))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))
    }
}
