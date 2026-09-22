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

}
