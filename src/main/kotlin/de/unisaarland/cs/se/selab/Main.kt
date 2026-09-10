package de.unisaarland.cs.se.selab

import de.unisaarland.cs.se.selab.enums.LogLevel
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

/**
Main Function
 **/
fun main(args: Array<String>) {
    val cliinfo = parseCommandLineArgs(args)
    if (cliinfo.shouldPrintHelp) {
        return
    }
}

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

    val logLevel: LogLevel = LogLevel.valueOf(logLevelStr)

    parser.parse(args)

    return Cliinfo(foodPath, restaurantsPath, scenarioPath, maxTicks, logLevel, outputFilePathStr, false)
}


private fun help() {

}
