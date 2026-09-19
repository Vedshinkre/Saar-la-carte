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

        // Ingredients with bestBefore 1 bought the evening before expire, and so does the rice
        // bought on evening 27 (bestBefore 3). The pasta bought on evening 28 is still good.
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 500, gram, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 1000, gram, "chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 100, gram, "garlic"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 500, "mL", "oil"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 10, "X", "onion"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 1000, gram, "potato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 1000, gram, "rice"))
        // Needed tonight: rc3's last order (2 beef pasta, 5 chicken rice, 3 potato soup) plus
        // ceil(14 free seats / 10) = 2 of every dish -> 4 beef pasta, 7 chicken rice, 5 potato
        // soup. The 1000 g of pasta still in stock covers the 800 g of pasta, so none is bought.
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 600, gram, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, gram, "chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 200, gram, "garlic"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 500, "mL", "oil"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 20, "X", "onion"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 2000, gram, "potato"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 2000, gram, "rice"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }
}
