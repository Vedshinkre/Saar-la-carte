package de.unisaarland.cs.se.selab

import de.unisaarland.cs.se.selab.enums.LogLevel

/**
 * The parsed command-line arguments.
 *
 * @property outputPath the log file, or an empty string to log to stdout
 */
// rename to CliInfo
data class Cliinfo(
    val foodFilePath: String,
    val restaurantFilePath: String,
    val scenarioFilePath: String,
    val maxTicks: Int,
    val logLevel: LogLevel,
    val outputPath: String
)
