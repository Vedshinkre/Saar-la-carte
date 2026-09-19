package de.unisaarland.cs.se.selab.systemtest.selab26.fulltests

import de.unisaarland.cs.se.selab.systemtest.selab26.SystemTestSELab26
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.utils.loadResource

private const val SCENARIO_DIR = "fulltests/scenarios"
private const val EXPECTED_LOG_DIR = "fulltests/logs"
private const val INIT_INFO = "Initialization Info: "

/** marks a region of the log the expectation deliberately does not pin down */
private const val ELLIPSIS = "..."

private const val STAFF_AND_TABLES = "staff-and-tables"
private const val KITCHEN_AND_SERVING = "kitchen-and-serving"
private const val DELIVERY_HANDOFF = "delivery-handoff"
private const val EVENT_MERGE_INCIDENTS = "event-merge-incidents"

/** test against scenarios of `fulltests/`, run simulation on each entry and compares log lines against expected ones */
fun fullScenarioSystemTests(): List<SystemTestSELab26> = listOf(
    FullScenarioSystemTest(STAFF_AND_TABLES, maxTicks = 24),
    FullScenarioSystemTest(STAFF_AND_TABLES, maxTicks = 24, logLevel = "INFO", variant = "info"),
    FullScenarioSystemTest(STAFF_AND_TABLES, maxTicks = 24, logLevel = "IMPORTANT", variant = "important"),
    FullScenarioSystemTest(STAFF_AND_TABLES, maxTicks = 12, variant = "maxticks12"),
    FullScenarioSystemTest(KITCHEN_AND_SERVING, maxTicks = 24),
    FullScenarioSystemTest(DELIVERY_HANDOFF, maxTicks = 48),
    FullScenarioSystemTest(EVENT_MERGE_INCIDENTS, maxTicks = 96),
)

/** replays the `fulltests` scenario [scenarioName]
 * asserts its output line by line against the expected log in `fulltests/logs`
 * a line of `...` in the expected log skips ahead to the next expected line instead of asserting it
 * [variant] names the run for one scenario is replayed several ways e.g. `staff-and-tables.info`
 */
class FullScenarioSystemTest(
    private val scenarioName: String,
    override val maxTicks: Int,
    override val logLevel: String = "DEBUG",
    private val variant: String = ""
) : ExampleSystemTestExtension() {
    private val runName = if (variant.isEmpty()) scenarioName else "$scenarioName.$variant"

    override val name = "FullTest-$runName"
    override val description =
        "Replays $runName for $maxTicks ticks at $logLevel and asserts the complete expected log."

    override val food = "$SCENARIO_DIR/$scenarioName/food.json"
    override val restaurants = "$SCENARIO_DIR/$scenarioName/restaurants.json"
    override val scenario = "$SCENARIO_DIR/$scenarioName/scenario.json"

    override suspend fun run() {
        var skipping = false
        for (line in expectedLines()) {
            if (line == ELLIPSIS) {
                skipping = true
            } else if (skipping) {
                skipUntilString(line)
                skipping = false
            } else {
                assertNextLine(line)
            }
        }
        if (!skipping) assertEnd()
    }

    private fun expectedLines(): List<String> {
        val path = "$EXPECTED_LOG_DIR/$runName.log"
        val log = requireNotNull(loadResource(javaClass, path)) { "missing expected log $path" }
        return log.lines().dropLastWhile { it.isBlank() }.map(::withBareInitPath)
    }
}

// fix the expected file paths for the initialization logs
private fun withBareInitPath(line: String): String {
    if (!line.contains(INIT_INFO)) return line
    return line.substringBefore(INIT_INFO) + INIT_INFO + line.substringAfter(INIT_INFO).substringAfterLast('/')
}
