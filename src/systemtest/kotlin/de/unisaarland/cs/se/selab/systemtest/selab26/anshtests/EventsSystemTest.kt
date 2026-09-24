package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val NEGATIVE = "NEGATIVE"

/**
 * for P03, P04
 * event groups 1, 2 (4 each) both pick the restaurant on evening 1, 8 common seats fit both
 *
 * evening 4: group 1 merges tables 1 (3) + 3 (2), table 2 (3) too small for group 2
 * -> group 2 rates negative in tick 1 and never arrives
 *
 * regular group 3 orders stew in tick 1, keeps the only cook busy till tick 4
 * group 1 soup from tick 2 only starts in tick 5
 * -> leaves unserved in tick 6, rates negative
 */
class EventsSystemTest : ExampleSystemTestExtension() {
    override val name = "EventWithoutTableAndUnservedEvent"
    override val description = "An EVENT without a table rates in tick 1, an unserved EVENT leaves and rates"
    override val food = "anshtests/events/food.json"
    override val restaurants = "anshtests/events/restaurants.json"
    override val scenario = "anshtests/events/scenario.json"
    override val logLevel = "INFO"
    override val maxTicks = 78

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(4))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 2))

        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(FohArrivalTestLogs.ordering(1, 3, 1, mapOf("potato stew" to 2), 1))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "EXEC", 2, "potato stew", 1, listOf(1)))
        assertNextLine(FohServiceTestLogs.rating(1, 2, NEGATIVE, 0, 1))

        assertNextLine(TickStatusTestLogs.tickStart(2, 4))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 1, listOf(1, 3), 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 2, mapOf("potato soup" to 4), listOf(1)))
        assertNextLine(TickStatusTestLogs.tickStart(3, 4))

        skipUntilString(TickStatusTestLogs.tickStart(6, 4))
        assertNextLine(FohServiceTestLogs.noEating(1, 4, 1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 3, "POSITIVE", 1, 1))
        assertNextLine(FohServiceTestLogs.rating(1, 1, NEGATIVE, 1, 2))
    }
}
