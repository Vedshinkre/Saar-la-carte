package unavailabilityincidenttests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.parsers.ParserController
import de.unisaarland.cs.se.selab.system.Simulation
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val FOOD = "src/test/kotlin/unavailabilityincidenttests/fixtures/food.json"
private const val RESTAURANTS = "src/test/kotlin/unavailabilityincidenttests/fixtures/restaurants.json"
private const val SCENARIO = "src/test/kotlin/unavailabilityincidenttests/fixtures/scenario.json"

/**
 * F34 - Incident: Ingredient Unavailability.
 *
 * restaurant has zero customer groups
 * kitchen still estimates demand from the free seats alone
 * with 2 free seats it always wants one "Grilled Chicken" independent of any customer behavior
 * chicken's `bestBefore` is 1, so whatever was bought the previous evening always expires before next evening planning
 * this forces another procurement attempt every single evening regardless of if anything was cooked
 * so only test the behavior there the supplier cannot give it due to unavailability
 */
class UnavailabilityIncidentIntegrationTest {
    private lateinit var output: StringWriter

    @BeforeTest
    fun setUp() {
        Order.resetIds()
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    @Test
    fun `ingredient becomes unprocurable for exactly the incident's duration, then recovers`() {
        val sections = runFourEveningsGroupedByEvening()

        assertTrue(procuredChicken(sections, evening = 1), "evening 1: Chicken should still be available")
        assertFalse(procuredChicken(sections, evening = 2), "evening 2: the incident should have blocked Chicken")
        assertFalse(procuredChicken(sections, evening = 3), "evening 3: still within the incident's duration")
        assertTrue(procuredChicken(sections, evening = 4), "evening 4: the incident's duration has expired")
    }

    @Test
    fun `the incident is logged exactly once, before evening 2's preparation`() {
        val lines = runFourEveningsGroupedByEvening().getValue(2)

        val incidentLineIndex = lines.indexOfFirst { it.contains("Incident: Incident 1 of type UNAVAILABLE") }
        val preparationLineIndex = lines.indexOfFirst { it.contains("Preparation for evening 2 starts.") }

        assertTrue(incidentLineIndex >= 0, "the UNAVAILABLE incident should be logged")
        assertTrue(
            incidentLineIndex < preparationLineIndex,
            "the incident must be applied before the evening's preparation phase plans purchases"
        )
        assertTrue(
            lines.first { it.contains("Incident: Incident 1") }
                .endsWith("Incident: Incident 1 of type UNAVAILABLE occurred before evening 2."),
            "the incident log line's evening number must match the evening it actually occurred before"
        )
    }

    /**
     * Runs the simulation through evening 4's preparation phase and groups the resulting log
     * lines by the evening they belong to. An evening's incidents are logged right *before* that
     * evening's own "Preparation ... starts." line (see [SimulationIntegrationTest]'s expected
     * ordering), so both "occurred before evening N" and "Preparation for evening N starts." count
     * as the start of evening N's section.
     */
    private fun runFourEveningsGroupedByEvening(): Map<Int, List<String>> {
        val config = ParserController().parseFiles(FOOD, RESTAURANTS, SCENARIO)
        assertFalse(config.wasInvalidFile)
        Time.setMaxTicks(TICKS_PER_EVENING * EVENINGS_FULLY_SIMULATED + 1)

        Simulation(config).runSimulation()

        val lines = output.toString().trim().lines()
        val sections = mutableMapOf<Int, MutableList<String>>()
        var currentEvening = 0
        for (line in lines) {
            val match = EVENING_BOUNDARY.find(line)
            if (match != null) {
                val eveningNumber = match.groupValues[1].ifBlank { match.groupValues[2] }
                currentEvening = eveningNumber.toInt()
            }
            if (currentEvening > 0) {
                sections.getOrPut(currentEvening) { mutableListOf() }.add(line)
            }
        }
        return sections
    }

    private fun procuredChicken(sections: Map<Int, List<String>>, evening: Int): Boolean =
        sections.getValue(evening).any { it.contains("Procured") && it.contains("Chicken") }

    private companion object {
        const val TICKS_PER_EVENING = 24
        const val EVENINGS_FULLY_SIMULATED = 3
        val EVENING_BOUNDARY = Regex("occurred before evening (\\d+)\\.|Preparation for evening (\\d+) starts\\.")
    }
}
