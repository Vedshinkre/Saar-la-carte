package simulationtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.main
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SIMPLE = "src/systemtest/resources/simplejson/"
private const val THREE_RESTAURANTS = "src/systemtest/resources/statisticsordering/"
private const val TICKS_PER_EVENING = 24
private const val STATISTICS_CALCULATED = "[IMPORTANT] Simulation Info: Simulation statistics are calculated."

/**
 * F01 end to end: the real `main` runs a simulation from the configuration files and the framing of the log is
 * checked against the spec (2.3.2 and 2.3.3): what surrounds the evenings and ticks, how the run ends for every kind
 * of `maxTicks`, which lines each log level keeps, and the order of the restaurants. What happens inside the ticks
 * has its own tests. The argument checks of `main` are in `MainIntegrationTest`.
 */
class SimulationRunTest {
    // the logger keeps its output file open, so the files stay in the build directory
    private val outputDirectory = File("build/simulation-run-test").also { it.mkdirs() }

    @BeforeEach
    fun setUp() = resetTime()

    @AfterEach
    fun tearDown() = resetTime()

    private fun resetTime() {
        Order.resetIds()
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    private fun run(directory: String, maxTicks: Int, logLevel: String = "DEBUG"): List<String> {
        resetTime()
        val out = File(outputDirectory, "out-$maxTicks-$logLevel-${directory.hashCode()}.log")
        main(
            arrayOf(
                "--food", directory + "food.json",
                "--restaurants", directory + "restaurants.json",
                "--scenario", directory + "scenario.json",
                "--maxTicks", maxTicks.toString(),
                "--logLevel", logLevel,
                "--out", out.path
            )
        )
        return out.readLines().filter { it.isNotBlank() }
    }

    private fun List<String>.count(text: String) = count { it.contains(text) }

    private fun List<String>.ticks() = filter { it.startsWith("[IMPORTANT] Simulation: Tick ") }

    // ---- how the run ends

    @Test
    fun `without ticks the simulation starts and directly calculates the statistics`() {
        val log = run(SIMPLE, 0)

        val start = log.indexOf("[INFO] Simulation Info: Simulation started.")
        assertEquals(3, start, "the three configuration files are logged first")
        assertEquals(STATISTICS_CALCULATED, log[start + 1])
        assertEquals(0, log.count("Preparation"))
        assertTrue(log.ticks().isEmpty())
        assertTrue(log.drop(start + 2).all { it.startsWith("[IMPORTANT] Simulation Statistics: Restaurant ") })
    }

    @Test
    fun `a multiple of 24 ticks ends right after the serving phase and never prepares the next evening`() {
        val log = run(SIMPLE, 2 * TICKS_PER_EVENING)

        val end = log.indexOf("[IMPORTANT] Serving: Serving of evening 2 ends.")
        assertEquals(STATISTICS_CALCULATED, log[end + 1])
        assertEquals(2, log.count("Preparation for evening"))
        assertEquals(0, log.count("Preparation for evening 3"))
        assertEquals(2 * TICKS_PER_EVENING, log.ticks().size)
    }

    @Test
    fun `other tick counts end in the middle of the serving phase right after a restaurant finished its tick`() {
        val log = run(SIMPLE, TICKS_PER_EVENING + 6)

        val statistics = log.indexOf(STATISTICS_CALCULATED)
        assertEquals("[DEBUG] Restaurant End (R 1): Restaurant 1 finished simulating the tick.", log[statistics - 1])
        assertEquals("[IMPORTANT] Simulation: Tick 6 (2) started.", log.ticks().last())
        assertEquals(1, log.count("Serving of evening 1 ends."))
        assertEquals(0, log.count("Serving of evening 2 ends."))
        assertEquals(1, log.count("Serving of evening 2 starts."))
    }

    @Test
    fun `a single tick is a whole run`() {
        val log = run(SIMPLE, 1)

        assertEquals(listOf("[IMPORTANT] Simulation: Tick 1 (1) started."), log.ticks())
        assertEquals(1, log.count("Preparation for evening 1 starts."))
        assertEquals(0, log.count("Serving of evening 1 ends."))
    }

    @Test
    fun `the maximum of 1000 ticks is accepted and ends in the middle of evening 42`() {
        val log = run(SIMPLE, 1000, "IMPORTANT")

        assertEquals(1000, log.ticks().size)
        assertEquals("[IMPORTANT] Simulation: Tick 16 (42) started.", log.ticks().last())
        val serving = log.filter { it.startsWith("[IMPORTANT] Serving: Serving of evening") }
        assertEquals(42, serving.count { it.endsWith("starts.") })
        assertEquals(41, serving.count { it.endsWith("ends.") }, "the last evening has no end")
        assertTrue(log.contains(STATISTICS_CALCULATED))
    }

    // ---- framing of the evenings and ticks

    @Test
    fun `every evening is preparation then serving and the ticks restart in each evening`() {
        val log = run(SIMPLE, 2 * TICKS_PER_EVENING, "IMPORTANT")

        val frame = log.filter { it.contains("Preparation for") || it.contains("Serving of") }
        assertEquals(
            listOf(
                "[IMPORTANT] Preparation: Preparation for evening 1 starts.",
                "[IMPORTANT] Serving: Serving of evening 1 starts.",
                "[IMPORTANT] Serving: Serving of evening 1 ends.",
                "[IMPORTANT] Preparation: Preparation for evening 2 starts.",
                "[IMPORTANT] Serving: Serving of evening 2 starts.",
                "[IMPORTANT] Serving: Serving of evening 2 ends."
            ),
            frame
        )
        val evening1 = (1..TICKS_PER_EVENING).map { "[IMPORTANT] Simulation: Tick $it (1) started." }
        val evening2 = (1..TICKS_PER_EVENING).map { "[IMPORTANT] Simulation: Tick $it (2) started." }
        assertEquals(evening1 + evening2, log.ticks())
    }

    @Test
    fun `a tick line is followed by the restaurant and the evening is closed only after the last tick`() {
        val log = run(SIMPLE, TICKS_PER_EVENING)

        val serving = log.indexOfFirst { it.contains("Serving of evening 1 starts.") }
        assertEquals("[IMPORTANT] Simulation: Tick 1 (1) started.", log[serving + 1])
        val lastTick = log.indexOf("[IMPORTANT] Simulation: Tick 24 (1) started.")
        assertTrue(log.drop(lastTick).first { it.contains("Restaurant End") }.contains("(R 1)"))
        assertTrue(log.indexOf("[IMPORTANT] Serving: Serving of evening 1 ends.") > lastTick)
    }

    // ---- log levels

    @Test
    fun `each log level keeps exactly the lines of its level and the more important ones`() {
        val debug = run(SIMPLE, TICKS_PER_EVENING, "DEBUG")
        val info = run(SIMPLE, TICKS_PER_EVENING, "INFO")
        val important = run(SIMPLE, TICKS_PER_EVENING, "IMPORTANT")

        assertTrue(debug.any { it.startsWith("[DEBUG] ") })
        assertTrue(debug.any { it.startsWith("[INFO] ") })
        assertTrue(debug.any { it.startsWith("[IMPORTANT] ") })
        assertEquals(debug.filter { !it.startsWith("[DEBUG] ") }, info)
        assertEquals(debug.filter { it.startsWith("[IMPORTANT] ") }, important)
    }

    @Test
    fun `the simulation started and configuration lines are INFO and disappear at the IMPORTANT level`() {
        val info = run(SIMPLE, 0, "INFO")
        val important = run(SIMPLE, 0, "IMPORTANT")

        assertEquals(4, info.count { it.startsWith("[INFO] ") })
        assertFalse(important.any { it.startsWith("[INFO] ") })
        assertTrue(important.contains(STATISTICS_CALCULATED))
    }

    // ---- several restaurants

    @Test
    fun `the restaurants prepare in ascending id although the file lists them in another order`() {
        val log = run(THREE_RESTAURANTS, 0 + 1)

        val restocked = log.filter { it.contains("Restocked ingredients.") }
        assertEquals(
            listOf(
                "[INFO] Pantry (R 1): Restocked ingredients.",
                "[INFO] Pantry (R 2): Restocked ingredients.",
                "[INFO] Pantry (R 3): Restocked ingredients."
            ),
            restocked
        )
    }

    @Test
    fun `every tick lets each restaurant simulate in ascending id`() {
        val log = run(THREE_RESTAURANTS, 3)

        val frame = log.filter { it.contains("Restaurant Start (R") || it.contains("Restaurant End (R") }
        val oneTick = (1..3).flatMap { id ->
            listOf(
                "[DEBUG] Restaurant Start (R $id): Restaurant $id simulates a tick.",
                "[DEBUG] Restaurant End (R $id): Restaurant $id finished simulating the tick."
            )
        }
        assertEquals(oneTick + oneTick + oneTick, frame)
    }

    @Test
    fun `the statistics are four lines per restaurant in ascending id`() {
        val log = run(THREE_RESTAURANTS, 1)

        val statistics = log.drop(log.indexOf(STATISTICS_CALCULATED) + 1)
        assertEquals(12, statistics.size)
        val kinds = listOf("cooked", "served", "delivered", "received")
        for (restaurant in 1..3) {
            val block = statistics.subList((restaurant - 1) * 4, restaurant * 4)
            block.forEachIndexed { index, line ->
                assertTrue(
                    line.startsWith("[IMPORTANT] Simulation Statistics: Restaurant $restaurant ${kinds[index]}"),
                    line
                )
            }
        }
    }
}
