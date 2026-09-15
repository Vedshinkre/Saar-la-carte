package de.unisaarland.cs.se.selab.loggers
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
import java.io.PrintWriter
/**
 * Handles log levels and output.
 */
object Logger {

    private var currentLevel: LogLevel? = null
    private var outputHandle: PrintWriter = PrintWriter(System.out)
    var restaurantID: Id = -1

    /**
     *  In case a list of Ids must be
     * logged, output string of a comma-separated list of Ids, (e.g., 3,5,7,8).
     */
    fun formatIds(ids: List<Id>): String = ids.sorted().joinToString(",")

    /**
     *  In case key-value mapping must be
     * logged, output string of a comma-separated list of key:value pairs, (e.g., A:3,B:5,C:7,D:8).
     */
    fun formatKeyValueMap(values: Map<String, Int>): String = values.entries
        .sortedBy { it.key }
        .joinToString(",") { "${it.key}:${it.value}" }

    private fun shouldLog(level: LogLevel?): Boolean {
        return when (currentLevel) {
            LogLevel.DEBUG -> true
            LogLevel.INFO -> level != LogLevel.DEBUG
            LogLevel.IMPORTANT -> level == LogLevel.IMPORTANT
            null -> throw IllegalArgumentException("LogLevel is not correct")
        }
    }

    /**
     * Sets the current log level.
     */
    fun setup(level: LogLevel?) {
        currentLevel = level
    }

    /**
     * Sets the output writer.
     */
    fun setup(writer: PrintWriter) {
        outputHandle = writer
    }

    /**
     * Logs a message at the given level.
     */
    fun log(level: LogLevel, message: String) {
        if (shouldLog(level)) {
            outputHandle.println("[$level] $message")
            outputHandle.flush()
        }
    }
}
