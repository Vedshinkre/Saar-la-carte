package de.unisaarland.cs.se.selab

import enums.LogLevel

/**
 * data class that contains all the command line arguments that get passed to main
 */
data class Cliinfo(
    val foodFilePath: String,
    val restaurantFilePath: String,
    val scenarioFilePath: String,
    val maxTicks: Int,
    val logLevel: LogLevel,
    val outputPath: String?,
    val shouldPrintHelp: Boolean
)
