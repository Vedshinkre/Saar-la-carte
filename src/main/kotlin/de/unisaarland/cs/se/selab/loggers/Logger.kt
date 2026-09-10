package loggers

import enums.LogLevel
import types.Id
import java.io.PrintWriter

object Logger {
    private var currentLevel: LogLevel? = null
    private var outputHandle: PrintWriter = PrintWriter(System.out)
    var restaurantId: Id? = null
}