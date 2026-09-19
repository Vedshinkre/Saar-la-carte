package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 3 -- FOH arrival, seating, and ordering for tick 1 of evening 30.
 */
class AppendixScenarioTwoArrivalSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioTwoArrivalSystemTest"
    override val description = "Appendix scenario 2, phase 3: FOH arrival, seating, and ordering."
    override val food = "appendixScenario2/food.json"
    override val restaurants = "appendixScenario2/restaurants.json"
    override val scenario = "appendixScenario2/scenario.json"
    override val logLevel = "DEBUG"

    // 29 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 1 of
    // evening 30, the first evening tonight's diagram takes place on.
    override val maxTicks = 29 * 24 + 1

    private val potatoSoup = "potato soup"

    override suspend fun run() {
        // Skip past initialization, preparation, tick start, and restaurant decisions --
        // covered by earlier phases -- straight to restaurant 1's tick processing.
        skipUntilString(TickStatusTestLogs.tickStart(1, 30))
        skipUntilString(TickStatusTestLogs.restStart(1))

        // cc1 arrives in person at tick 1. cc2 is a delivery (visitingTick 6 - 3 cooking ticks -
        // ceil(10 / 5) driving ticks = tick 1), so it orders now without arriving or being seated.
        // cc5 found no restaurant. Orders 1 and 2 were placed on the earlier evenings 10 and 20.
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))
        // The two customers excluding beef and potato can only eat chicken rice; the other six
        // have no preferences and take the highest recipe id, potato soup.
        assertNextLine(
            FohArrivalTestLogs.ordering(1, 1, 3, mapOf("chicken rice" to 2, potatoSoup to 6), 1)
        )
        // Only 2000 g of potato was bought, so 200 g is left for cc2: no soup, so beef pasta
        // (highest id still available) and then chicken rice.
        assertNextLine(
            FohArrivalTestLogs.ordering(1, 2, 4, mapOf("beef pasta" to 4, "chicken rice" to 1), null)
        )
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 8, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 13, 1))
    }
}
