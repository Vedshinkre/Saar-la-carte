package simulationtests

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
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val FOOD = "src/systemtest/resources/simplejson/food.json"
private const val RESTAURANTS = "src/systemtest/resources/simplejson/restaurants.json"
private const val SCENARIO = "src/systemtest/resources/comprehensive/scenario.json"

/**
 * End-to-end run of a full Simulation across two evenings, using the real parser and a real
 * Restaurant (no mocks). Only the evening structure is asserted here (incidents, preparation, serving
 * start/end, tick numbering); cooking and serving behaviour have their own tests.
 */
class SimulationIntegrationTest {
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
        Logger.setup(LogLevel.IMPORTANT)
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    @Test
    fun `runSimulation spans two evenings, resetting ticks and ending each serving phase`() {
        val lines = runTwoEvenings().filter { line -> STRUCTURE_MARKERS.any { line.contains(it) } }

        val expected = buildList {
            add("[IMPORTANT] Incident: Incident 1 of type STAFF occurred before evening 1.")
            add("[IMPORTANT] Incident: Incident 2 of type RECIPE occurred before evening 1.")
            add("[IMPORTANT] Incident: Incident 3 of type PACKAGING occurred before evening 1.")
            add("[IMPORTANT] Preparation: Preparation for evening 1 starts.")
            add("[IMPORTANT] Serving: Serving of evening 1 starts.")
            (1..TICKS_PER_EVENING).forEach { add("[IMPORTANT] Simulation: Tick $it (1) started.") }
            add("[IMPORTANT] Serving: Serving of evening 1 ends.")
            add("[IMPORTANT] Incident: Incident 4 of type UNAVAILABLE occurred before evening 2.")
            add("[IMPORTANT] Preparation: Preparation for evening 2 starts.")
            add("[IMPORTANT] Serving: Serving of evening 2 starts.")
            (1..EXTRA_TICKS_INTO_SECOND_EVENING - TICKS_PER_EVENING).forEach {
                add("[IMPORTANT] Simulation: Tick $it (2) started.")
            }
        }
        assertEquals(expected, lines)
    }

    @Test
    fun `runSimulation - order ids stay unique across evenings`() {
        val orderIds = runTwoEvenings()
            .mapNotNull { Regex("placed order (\\d+) of").find(it)?.groupValues?.get(1)?.toInt() }

        assertEquals(orderIds.distinct(), orderIds, "order ids must not repeat across evenings: $orderIds")
    }

    private fun runTwoEvenings(): List<String> {
        val config = ParserController().parseFiles(FOOD, RESTAURANTS, SCENARIO)
        assertFalse(config.wasInvalidFile)
        Time.setMaxTicks(EXTRA_TICKS_INTO_SECOND_EVENING)
        Simulation(config).runSimulation()
        return output.toString().trim().lines()
    }

    private companion object {
        const val EXTRA_TICKS_INTO_SECOND_EVENING = 30
        const val TICKS_PER_EVENING = 24
        val STRUCTURE_MARKERS = listOf("Incident:", "Preparation:", "Serving:", "Simulation:")
    }
}
