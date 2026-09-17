package loggertests

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests the shared Logger object: level filtering (all branches of shouldLog),
 * the id/key-value formatting helpers, and output routing.
 */
class LoggerCoreTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    private fun loggedLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    @Test
    fun `formatIds sorts ascending and joins with commas`() {
        assertEquals("3,5,7,8", Logger.formatIds(listOf(8, 3, 7, 5)))
    }

    @Test
    fun `formatIds returns empty string for an empty list`() {
        assertEquals("", Logger.formatIds(emptyList()))
    }

    @Test
    fun `formatIds handles a single id`() {
        assertEquals("42", Logger.formatIds(listOf(42)))
    }

    @Test
    fun `formatKeyValueMap sorts by key ascending and joins with commas`() {
        val values = mapOf("C" to 7, "A" to 3, "D" to 8, "B" to 5)
        assertEquals("A:3,B:5,C:7,D:8", Logger.formatKeyValueMap(values))
    }

    @Test
    fun `formatKeyValueMap returns empty string for an empty map`() {
        assertEquals("", Logger.formatKeyValueMap(emptyMap()))
    }

    @Test
    fun `DEBUG level outputs DEBUG, INFO and IMPORTANT messages`() {
        Logger.setup(LogLevel.DEBUG)
        Logger.log(LogLevel.DEBUG, "d")
        Logger.log(LogLevel.INFO, "i")
        Logger.log(LogLevel.IMPORTANT, "m")

        assertEquals(listOf("[DEBUG] d", "[INFO] i", "[IMPORTANT] m"), loggedLines())
    }

    @Test
    fun `INFO level suppresses DEBUG but keeps INFO and IMPORTANT`() {
        Logger.setup(LogLevel.INFO)
        Logger.log(LogLevel.DEBUG, "d")
        Logger.log(LogLevel.INFO, "i")
        Logger.log(LogLevel.IMPORTANT, "m")

        assertEquals(listOf("[INFO] i", "[IMPORTANT] m"), loggedLines())
    }

    @Test
    fun `IMPORTANT level suppresses DEBUG and INFO but keeps IMPORTANT`() {
        Logger.setup(LogLevel.IMPORTANT)
        Logger.log(LogLevel.DEBUG, "d")
        Logger.log(LogLevel.INFO, "i")
        Logger.log(LogLevel.IMPORTANT, "m")

        assertEquals(listOf("[IMPORTANT] m"), loggedLines())
    }

    @Test
    fun `log throws when no log level has been configured yet`() {
        // shouldLog's `null -> throw` branch models the state before setup(LogLevel) is ever
        // called. Logger is a process-wide singleton with no reset function, so the only way to
        // reach that state deterministically (regardless of what other tests already configured)
        // is to reset the private field directly.
        val field = Logger::class.java.getDeclaredField("currentLevel")
        field.isAccessible = true
        field.set(Logger, null)

        assertFailsWith<IllegalArgumentException> {
            Logger.log(LogLevel.IMPORTANT, "unreachable")
        }
    }

    @Test
    fun `restaurantID is readable and writable`() {
        Logger.restaurantID = 17
        assertEquals(17, Logger.restaurantID)
    }

    @Test
    fun `log writes the level-prefixed message to the configured output handle`() {
        Logger.log(LogLevel.IMPORTANT, "hello")
        assertTrue(output.toString().contains("[IMPORTANT] hello"))
    }

    @Test
    fun `a suppressed message writes nothing to the output handle`() {
        Logger.setup(LogLevel.IMPORTANT)
        Logger.log(LogLevel.DEBUG, "hidden")
        assertEquals(emptyList<String>(), loggedLines())
    }
}
