package loggertests

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.Logger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

class LoggerTest {
    private lateinit var output: StringWriter
    private lateinit var writer: PrintWriter
    private lateinit var logger: Logger

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        writer = PrintWriter(output, true)

        logger.setup(writer)
    }

    @Test
    fun `DEBUG message is logged when current level is DEBUG`() {
        logger.setup(LogLevel.DEBUG)

        logger.log(LogLevel.DEBUG, "test message")

        assertEquals(
            "[DEBUG] test message\n",
            output.toString()
        )
    }
    fun `INFO message is logged when current level is INFO`() {
        logger.setup(LogLevel.INFO)

        logger.log(LogLevel.INFO, "test message")

        assertEquals(
            "[INFO] test message\n",
            output.toString()
        )
    }
    fun `IMPORTANT message is logged when current level is IMPORTANT`() {
        logger.setup(LogLevel.IMPORTANT)

        logger.log(LogLevel.IMPORTANT, "test message")

        assertEquals(
            "[IMPORTANT] test message\n",
            output.toString()
        )
    }

    @Test
    fun `debug level prints all messages`() {
        logger.setup(LogLevel.DEBUG)

        logger.log(LogLevel.DEBUG, "debug")
        logger.log(LogLevel.INFO, "info")
        logger.log(LogLevel.IMPORTANT, "important")

        val result = output.toString()

        assertTrue(result.contains("debug"))
        assertTrue(result.contains("info"))
        assertTrue(result.contains("important"))
    }

    @Test
    fun `info level does not print debug messages`() {
        logger.setup(LogLevel.INFO)

        logger.log(LogLevel.DEBUG, "debug")
        logger.log(LogLevel.INFO, "info")
        logger.log(LogLevel.IMPORTANT, "important")

        val result = output.toString()

        assertFalse(result.contains("debug"))
        assertTrue(result.contains("info"))
        assertTrue(result.contains("important"))
    }

    @Test
    fun `important level only prints important messages`() {
        logger.setup(LogLevel.IMPORTANT)

        logger.log(LogLevel.DEBUG, "debug")
        logger.log(LogLevel.INFO, "info")
        logger.log(LogLevel.IMPORTANT, "important")

        val result = output.toString()

        assertFalse(result.contains("debug"))
        assertFalse(result.contains("info"))
        assertTrue(result.contains("important"))
    }
}
