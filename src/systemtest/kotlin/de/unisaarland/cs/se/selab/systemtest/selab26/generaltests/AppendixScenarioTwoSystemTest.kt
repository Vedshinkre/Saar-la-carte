package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 */
class AppendixScenarioTwoSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioTwoSystemTest"
    override val description = "Appendix scenario 2: preparation and tick 1 of evening 30."
    override val food = "appendixScenario2/food.json"
    override val restaurants = "appendixScenario2/restaurants.json"
    override val scenario = "appendixScenario2/scenario.json"
    override val logLevel = "DEBUG"

    // 29 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 1 of
    // evening 30, the first evening tonight's diagram takes place on.
    override val maxTicks = 29 * 24 + 1

    private val gram = "g"
    private val potatoSoup = "potato soup"

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("scenario.json"))
        assertNextLine(InitialAndPrepTestLogs.SIM_START)

        // Skip past the 29 uneventful evenings (including rc3's earlier periodic visit on
        // evening 10) straight to the evening the diagram documents.
        skipUntilString(InitialAndPrepTestLogs.prepStart(30))

        // rc3's previous order history (chicken rice, beef pasta, potato soup) drives the
        // shopping list: the pantry first drops what's left from evening 10's stock, then
        // procures fresh ingredients for the 14 free seats available tonight.
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 500, gram, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 1000, gram, "chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 100, gram, "garlic"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 500, "mL", "oil"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 10, "X", "onion"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 1000, gram, "potato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 1000, gram, "rice"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 300, gram, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, gram, "chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, gram, "garlic"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 500, "mL", "oil"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 10, "X", "onion"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, gram, "potato"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, gram, "rice"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        assertNextLine(TickStatusTestLogs.servingStart(30))
        assertNextLine(TickStatusTestLogs.tickStart(1, 30))

        // cc1 (EUROPEAN/AFRICAN, dine-in) and cc2 (EUROPEAN/ASIAN, delivery) both match
        // restaurant 1's EUROPEAN menu; cc5 (AMERICAN only) has no eligible restaurant.
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertNextLine(TickStatusTestLogs.restNoDecision(5))

        assertNextLine(TickStatusTestLogs.restStart(1))

        // Only cc1 has visitingTick 1 -- cc2 and cc5 both wait until tick 6, so they don't
        // arrive, seat, or order yet this tick.
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(
                1,
                1,
                2,
                mapOf("beef pasta" to 2, "chicken rice" to 3, potatoSoup to 3),
                1
            )
        )
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 8, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 1))

        // Only one TOURNANT cook is on staff, so the potato soup (basic dish for EUROPEAN)
        // is picked and cooked first, ahead of the beef pasta and chicken rice in the order.
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "TOURNANT", 3, potatoSoup, 2, listOf(2)))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 3, potatoSoup, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 3, 3, 3))

        // The dishes just finished cooking this same tick, so the waiter hasn't served them yet.
        assertNextLine(FohServiceTestLogs.noServing(1, 1, 3, 2))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))

        assertNextLine(TickStatusTestLogs.restEnd(1))

        assertNextLine(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, 13))
        assertNextLine(StatisticsTestLogs.statsServed(1, 0))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 2))
    }
}
