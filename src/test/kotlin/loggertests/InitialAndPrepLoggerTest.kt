package loggertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger
import de.unisaarland.cs.se.selab.loggers.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

/**
 * Tests InitialAndPrepLogger against the exact wording and levels of spec section 2.3.2.
 */
class InitialAndPrepLoggerTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 4
        Time.evening = 2
    }

    private fun loggedLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    @Test
    fun `successful parse logs INFO success message with the filename`() {
        InitialAndPrepLogger.logInitialization(true, "food.json")
        assertEquals(
            listOf("[INFO] Initialization Info: food.json successfully parsed and validated."),
            loggedLines()
        )
    }

    @Test
    fun `failed parse logs IMPORTANT failure message with the filename`() {
        InitialAndPrepLogger.logInitialization(false, "restaurants.json")
        assertEquals(listOf("[IMPORTANT] Initialization Info: restaurants.json is invalid."), loggedLines())
    }

    @Test
    fun `simulation start logs INFO message`() {
        InitialAndPrepLogger.logSimulationStart()
        assertEquals(listOf("[INFO] Simulation Info: Simulation started."), loggedLines())
    }

    @Test
    fun `incident logs IMPORTANT message with id, type and the current evening`() {
        InitialAndPrepLogger.logIncident(9, "STAFF")
        assertEquals(
            listOf("[IMPORTANT] Incident: Incident 9 of type STAFF occurred before evening 2."),
            loggedLines()
        )
    }

    @Test
    fun `preparation start logs IMPORTANT message with the current evening`() {
        InitialAndPrepLogger.logPreparationStart()
        assertEquals(listOf("[IMPORTANT] Preparation: Preparation for evening 2 starts."), loggedLines())
    }

    @Test
    fun `no reservation logs IMPORTANT message with restaurant and group id`() {
        InitialAndPrepLogger.logFohNoReservation(6)
        assertEquals(
            listOf("[IMPORTANT] FOH No Reserving (R 4): No table could be reserved for group 6."),
            loggedLines()
        )
    }

    @Test
    fun `pantry procured logs DEBUG message with amount, unit and name`() {
        InitialAndPrepLogger.logPantryProcured(5, MeasurementUnit.G, "Flour")
        assertEquals(
            listOf("[DEBUG] Pantry (R 4): Procured 5 g of Flour from the supplier."),
            loggedLines()
        )
    }

    @Test
    fun `pantry restocked logs INFO message`() {
        InitialAndPrepLogger.logPantryRestocked()
        assertEquals(listOf("[INFO] Pantry (R 4): Restocked ingredients."), loggedLines())
    }

    @Test
    fun `pantry removed ingredient logs DEBUG message with amount, unit and name`() {
        InitialAndPrepLogger.logPantryRemovedIngredient(3, MeasurementUnit.ML, "Milk")
        assertEquals(
            listOf("[DEBUG] Pantry (R 4): Removed 3 mL of Milk from the pantry."),
            loggedLines()
        )
    }

    @Test
    fun `all three pantry DEBUG logs are suppressed at IMPORTANT log level`() {
        Logger.setup(LogLevel.IMPORTANT)
        InitialAndPrepLogger.logPantryProcured(5, MeasurementUnit.G, "Flour")
        InitialAndPrepLogger.logPantryRemovedIngredient(3, MeasurementUnit.ML, "Milk")
        assertEquals(emptyList<String>(), loggedLines())
    }

    @Test
    fun `INFO-level pantry restocked message survives at INFO log level`() {
        Logger.setup(LogLevel.INFO)
        InitialAndPrepLogger.logPantryRestocked()
        assertEquals(listOf("[INFO] Pantry (R 4): Restocked ingredients."), loggedLines())
    }
}
