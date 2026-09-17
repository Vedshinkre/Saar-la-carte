package loggertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.loggers.TickStatusLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

/**
 * Tests TickStatusLogger against spec section 2.3.2 ("Serving Phase" / "Restaurant Decision").
 */
class TickStatusLoggerTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 6
        Time.evening = 3
        Time.tick = 5
    }

    private fun loggedLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    @Test
    fun `serving start logs IMPORTANT message with the current evening`() {
        TickStatusLogger.logServingStart()
        assertEquals(listOf("[IMPORTANT] Serving: Serving of evening 3 starts."), loggedLines())
    }

    @Test
    fun `current tick logs IMPORTANT message with the tick and evening`() {
        TickStatusLogger.logCurrentTick()
        assertEquals(listOf("[IMPORTANT] Simulation: Tick 5 (3) started."), loggedLines())
    }

    @Test
    fun `restaurant decision logs DEBUG message with group and restaurant id`() {
        TickStatusLogger.logRestaurantDecision(9, 6)
        assertEquals(
            listOf("[DEBUG] Restaurant Decision: Group 9 decided on restaurant 6."),
            loggedLines()
        )
    }

    @Test
    fun `restaurant no decision logs DEBUG message with the group id`() {
        TickStatusLogger.logRestaurantNoDecision(9)
        assertEquals(
            listOf("[DEBUG] Restaurant No Decision: Group 9 could not decide for a restaurant."),
            loggedLines()
        )
    }

    @Test
    fun `restaurant start logs DEBUG message with the restaurant id`() {
        TickStatusLogger.logRestaurantStart()
        assertEquals(
            listOf("[DEBUG] Restaurant Start (R 6): Restaurant 6 simulates a tick."),
            loggedLines()
        )
    }

    @Test
    fun `restaurant end logs DEBUG message with the restaurant id`() {
        TickStatusLogger.logRestaurantEnd()
        assertEquals(
            listOf("[DEBUG] Restaurant End (R 6): Restaurant 6 finished simulating the tick."),
            loggedLines()
        )
    }

    @Test
    fun `serving end logs IMPORTANT message with the current evening`() {
        TickStatusLogger.logServingEnd()
        assertEquals(listOf("[IMPORTANT] Serving: Serving of evening 3 ends."), loggedLines())
    }

    @Test
    fun `all DEBUG-level logs are suppressed at INFO log level`() {
        Logger.setup(LogLevel.INFO)
        TickStatusLogger.logRestaurantDecision(9, 6)
        TickStatusLogger.logRestaurantNoDecision(9)
        TickStatusLogger.logRestaurantStart()
        TickStatusLogger.logRestaurantEnd()
        assertEquals(emptyList<String>(), loggedLines())
    }

    @Disabled
    @Test
    fun `IMPORTANT-level logs survive even at IMPORTANT log level`() {
        Logger.setup(LogLevel.IMPORTANT)
        TickStatusLogger.logServingStart()
        TickStatusLogger.logCurrentTick()
        TickStatusLogger.logServingEnd()
        assertEquals(
            listOf(
                "[IMPORTANT] Serving: Serving of evening 3 starts.",
                "[IMPORTANT] Simulation: Tick 5 (3) started.",
                "[IMPORTANT] Serving: Serving of evening 3 ends."
            ),
            loggedLines()
        )
    }
}
