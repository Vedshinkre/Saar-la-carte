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
        skipUntilString(TickStatusTestLogs.restStart(1))

        // Only cc1 has visitingTick 1 -- cc2 and cc5 both wait until tick 6, so they don't
        // arrive, seat, or order yet this tick.
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                1, 1, 2, mapOf("beef pasta" to 2, "chicken rice" to 3, potatoSoup to 3), 1
            )
        )
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 8, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 1))
    }
}
