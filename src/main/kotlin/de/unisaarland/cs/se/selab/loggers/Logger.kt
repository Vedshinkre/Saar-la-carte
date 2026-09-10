package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
import java.io.PrintWriter

object Logger {
    private var currentLevel: LogLevel? = null
    private var outputHandle: PrintWriter = PrintWriter(System.out)
    var restaurantId: Id? = null
}
