package de.unisaarland.cs.se.selab.systemtest.selab26.utils

import de.unisaarland.cs.se.selab.systemtest.api.SystemTestAssertionError
import de.unisaarland.cs.se.selab.systemtest.selab26.SystemTestSELab26

/**
 * Adds skips to basic systemtest class
 */
abstract class ExampleSystemTestExtension : SystemTestSELab26() {

    /**
     * Skips until the given [startString] is found
     */
    suspend fun skipUntilString(startString: String): String { // removed private on suggestion of the tutor
        val line: String = getNextLine()
            ?: throw SystemTestAssertionError("End of log reached when there should be more.")
        return if (line.startsWith(startString)) {
            line
        } else {
            skipUntilString(startString)
        }
    }

    /**
     * Skips until the given [logLevel] and [logType] is found
     */
    suspend fun skipUntilLogType(logLevel: LogLevel, logType: LogType): String {
        return skipUntilString("[$logLevel] $logType")
    }
}

/**
 * Enum for categories of logs
 */
enum class LogType(private val message: String) {
    INITIALIZATION_INFO("Initialization Info"),
    SIMULATION_INFO("Simulation Info"),
    SIMULATION_STATISTICS("Simulation Statistics");

    override fun toString(): String {
        return message
    }
}

/**
 * Enum for log levels
 */
enum class LogLevel {
    DEBUG,
    INFO,
    IMPORTANT
}
