package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * System test derived from the appendix sequence diagram for "Restaurant Fire" (id 1):
 * phase 1 -- initialization and preparation for evening 30.
 */
class AppendixScenarioTwoPreparationSystemTest : ExampleSystemTestExtension() {
    override val name = "AppendixScenarioTwoPreparationSystemTest"
    override val description = "Appendix scenario 2, phase 1: initialization and preparation for evening 30."
    override val food = "appendixScenario2/food.json"
    override val restaurants = "appendixScenario2/restaurants.json"
    override val scenario = "appendixScenario2/scenario.json"
    override val logLevel = "DEBUG"

    // 29 full evenings (24 ticks each, per restaurant 1's opening hours) plus tick 1 of
    // evening 30, the first evening tonight's diagram takes place on.
    override val maxTicks = 29 * 24 + 1

    private val gram = "g"

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
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 1000, gram, "pasta"))
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
    }
}
