package de.unisaarland.cs.se.selab

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.loggers.StatisticsLogger
import de.unisaarland.cs.se.selab.parsers.ParserController
import de.unisaarland.cs.se.selab.system.Simulation
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.File
import java.io.PrintWriter

/**
 * Entry point of the simulation. Parses the CLI arguments, sets up global state
 * (logger + Time), delegates config parsing to the [ParserController] and then
 * runs the simulation to completion.
 */
fun main(args: Array<String>) {
    val cli = parseCommandLineArgs(args)
    if (cli.shouldPrintHelp) {
        help()
        return
    }

    setupLogging(cli)
    Time.setMaxTicks(cli.maxTicks)

    val parser = ParserController()
    val simConfig = parser.parseFiles(cli.foodFilePath, cli.restaurantFilePath, cli.scenarioFilePath)

    val sim = Simulation(simConfig)
    sim.runSimulation()

    StatisticsLogger.logSimulationStatsCalculated()
}

/**
 * Parses the raw CLI [args] into a [CliInfo] instance.
 */
private fun parseCommandLineArgs(args: Array<String>): Cliinfo {
    val parser = ArgParser("SaarLaCarte cli parser")
    val foodPath by parser.option(ArgType.String, fullName = "food").required()
    val restaurantsPath by parser.option(ArgType.String, fullName = "restaurants").required()
    val scenarioPath by parser.option(ArgType.String, fullName = "scenario").required()
    val maxTicks by parser.option(ArgType.Int, fullName = "maxTicks").required()
    val logLevelStr by parser.option(ArgType.String, fullName = "logLevel").required()
    val outputFilePathStr by parser.option(
        ArgType.String,
        fullName = "out",
    ).default("") // empty means stdout
    val shouldPrintHelp by parser.option(ArgType.Boolean, fullName = "shouldPrintHelp").default(false)

    parser.parse(args)

    val logLevel = LogLevel.valueOf(logLevelStr)

    return Cliinfo(
        foodFilePath = foodPath,
        restaurantFilePath = restaurantsPath,
        scenarioFilePath = scenarioPath,
        maxTicks = maxTicks,
        logLevel = logLevel,
        outputPath = outputFilePathStr,
        shouldPrintHelp = shouldPrintHelp,
    )
}

/**
 * Wires up the global [Logger] singleton: the requested log level and the output
 * handle (stdout, unless an output file path was given).
 */
private fun setupLogging(cli: Cliinfo) {
    Logger.setup(cli.logLevel)
    val writer = if (cli.outputPath.isNullOrEmpty()) {
        PrintWriter(System.out)
    } else {
        PrintWriter(File(cli.outputPath))
    }
    Logger.setup(writer)

    // The specialized loggers (InitialAndPrepLogger, TickStatusLogger, ...) are modeled
    // as singleton `object`s that all delegate to Logger internally (see the class
    // diagram's "uses" dependencies), so nothing further needs to be instantiated here.
}

private fun help() {
    println(
        """
        SaarLaCarte - restaurant simulation

        Usage:
          --food <path>        Path to the food configuration JSON file (required)
          --restaurants <path> Path to the restaurant configuration JSON file (required)
          --scenario <path>    Path to the scenario configuration JSON file (required)
          --maxTicks <n>       Maximum number of ticks the simulation should run for (required)
          --logLevel <level>   One of DEBUG, INFO, IMPORTANT (required)
          --out <path>         Output file for logs (defaults to stdout)
        """.trimIndent(),
    )
}
