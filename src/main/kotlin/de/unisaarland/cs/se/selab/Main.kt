package de.unisaarland.cs.se.selab

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.parsers.ParserController
import de.unisaarland.cs.se.selab.system.Simulation
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.File
import java.io.PrintWriter

private const val MIN_MAX_TICKS = 0
private const val MAX_MAX_TICKS = 1000

/**
 * Entry point of the simulation. Parses the CLI arguments, sets up global state
 * (logger + Time), delegates config parsing to the [ParserController] and then
 * runs the simulation to completion.
 */
fun main(args: Array<String>) {
    val cli = parseCommandLineArgs(args)

    setupLogging(cli)
    Time.setMaxTicks(cli.maxTicks)

    val parser = ParserController()
    val simConfig = parser.parseFiles(cli.foodFilePath, cli.restaurantFilePath, cli.scenarioFilePath)
    if (simConfig.wasInvalidFile) {
        return
    }
    val sim = Simulation(simConfig)
    sim.runSimulation()
}

/**
 * Parses the raw CLI [args] into a [Cliinfo] instance. `--help` is handled by [ArgParser] itself,
 * which prints the usage info and exits.
 */
private fun parseCommandLineArgs(args: Array<String>): Cliinfo {
    val parser = ArgParser("SaarLaCarte")
    val foodPath by parser.option(
        ArgType.String,
        fullName = "food",
        description = "Path to the food configuration JSON file (required)"
    ).required()
    val restaurantsPath by parser.option(
        ArgType.String,
        fullName = "restaurants",
        description = "Path to the restaurant configuration JSON file (required)"
    ).required()
    val scenarioPath by parser.option(
        ArgType.String,
        fullName = "scenario",
        description = "Path to the scenario configuration JSON file (required)"
    ).required()
    val maxTicks by parser.option(
        ArgType.Int,
        fullName = "maxTicks",
        description = "Maximum number of ticks the simulation should run for (required)"
    ).required()
    val logLevel by parser.option(
        ArgType.Choice(
            LogLevel.entries,
            { LogLevel.valueOf(it) },
            { it.name }
        ),
        fullName = "logLevel",
        description = "One of DEBUG, INFO, IMPORTANT (required)"
    ).required()
    val outputFilePathStr by parser.option(
        ArgType.String,
        fullName = "out",
        description = "Output file for logs (defaults to stdout)"
    ).default("")

    parser.parse(args)
    require(maxTicks in MIN_MAX_TICKS..MAX_MAX_TICKS) {
        "--maxTicks has to be between $MIN_MAX_TICKS and $MAX_MAX_TICKS, but was $maxTicks"
    }

    return Cliinfo(
        foodFilePath = foodPath,
        restaurantFilePath = restaurantsPath,
        scenarioFilePath = scenarioPath,
        maxTicks = maxTicks,
        logLevel = logLevel,
        outputPath = outputFilePathStr,
    )
}

/**
 * Wires up the global [Logger] singleton: the requested log level and the output
 * handle (stdout, unless an output file path was given).
 */
private fun setupLogging(cli: Cliinfo) {
    Logger.setup(cli.logLevel)
    val writer = if (cli.outputPath.isEmpty()) {
        PrintWriter(System.out)
    } else {
        PrintWriter(File(cli.outputPath))
    }
    Logger.setup(writer)
}
