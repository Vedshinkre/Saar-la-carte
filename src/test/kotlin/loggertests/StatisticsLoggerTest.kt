package loggertests

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.loggers.StatisticsLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

/**
 * Tests StatisticsLogger against spec section 2.3.3 ("Simulation Statistics"). All
 * messages in this logger are IMPORTANT, so they must survive at every log level.
 */
class StatisticsLoggerTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 5
    }

    private fun loggedLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    @Test
    fun `stats calculated logs IMPORTANT message`() {
        StatisticsLogger.logSimulationStatsCalculated()
        assertEquals(
            listOf("[IMPORTANT] Simulation Info: Simulation statistics are calculated."),
            loggedLines()
        )
    }

    @Test
    fun `cooked stats log IMPORTANT message with restaurant id and meal count`() {
        StatisticsLogger.logSimulationStatsCooked(12)
        assertEquals(
            listOf("[IMPORTANT] Simulation Statistics: Restaurant 5 cooked 12 meals."),
            loggedLines()
        )
    }

    @Test
    fun `served stats log IMPORTANT message with restaurant id and customer count`() {
        StatisticsLogger.logSimulationStatsServed(9)
        assertEquals(
            listOf("[IMPORTANT] Simulation Statistics: Restaurant 5 served 9 customers."),
            loggedLines()
        )
    }

    @Test
    fun `delivered stats log IMPORTANT message with restaurant id and customer count`() {
        StatisticsLogger.logSimulationStatsDelivered(4)
        assertEquals(
            listOf("[IMPORTANT] Simulation Statistics: Restaurant 5 delivered meals to 4 customers."),
            loggedLines()
        )
    }

    @Test
    fun `ratings stats log IMPORTANT message with restaurant id and rating count`() {
        StatisticsLogger.logSimulationStatsRatingsGiven(7)
        assertEquals(
            listOf("[IMPORTANT] Simulation Statistics: Restaurant 5 received 7 ratings."),
            loggedLines()
        )
    }

    @Test
    fun `all statistics logs survive at IMPORTANT log level`() {
        Logger.setup(LogLevel.IMPORTANT)
        StatisticsLogger.logSimulationStatsCalculated()
        StatisticsLogger.logSimulationStatsCooked(12)
        StatisticsLogger.logSimulationStatsServed(9)
        StatisticsLogger.logSimulationStatsDelivered(4)
        StatisticsLogger.logSimulationStatsRatingsGiven(7)
        assertEquals(5, loggedLines().size)
    }
}
