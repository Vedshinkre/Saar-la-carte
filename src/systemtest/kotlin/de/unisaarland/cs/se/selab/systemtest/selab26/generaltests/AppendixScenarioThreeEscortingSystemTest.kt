package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 4 -- FOH escorting, rating, and end of tick 6 of evening 35.
 */
class AppendixScenarioThreeEscortingSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioThreeEscortingSystemTest"
    override val description = "Appendix scenario 3, phase 4: FOH escorting, rating, and end of tick."
    override val food = "appendixScenario3/food.json"
    override val restaurants = "appendixScenario3/restaurants.json"
    override val scenario = "appendixScenario3/scenario.json"
    override val logLevel = "DEBUG"

    // 34 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 6 of
    // evening 35, the evening tonight's diagram takes place on.
    override val maxTicks = 34 * 24 + 6

    override suspend fun run() {
        // Skip past initialization, preparation, tick start, kitchen, serving, and eating --
        // covered by earlier phases -- straight to escorting.
        skipUntilString(TickStatusTestLogs.tickStart(6, 35))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // cc1's order is now fully eaten, so waiter w1 escorts all 8 of its customers from
        // their merged table (the minimum of the merged ids 3, 4, and 5 is reported as 3).
        skipUntilString(FohServiceTestLogs.eatingStatus(1, 0, 2))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 8, 1, 3))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 8))

        // With customersRemainingInRestaurant at 0, cc1 (a CasualGroup) has its merged table
        // silently dismantled and freed -- no log line covers that bookkeeping. ec6 still has
        // all 16 customers seated, so it is excluded from ratings this tick; only cc1 is
        // eligible. Its ratingLikelihood is ALWAYS and its experience was NEUTRAL (served and
        // fed on time), so it rates POSITIVE, pushing the restaurant's cumulative positive
        // ratings from 11 to 12 (negative stays at 5).
        assertNextLine(FohServiceTestLogs.rating(1, 1, "POSITIVE", 12, 5))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))

        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
