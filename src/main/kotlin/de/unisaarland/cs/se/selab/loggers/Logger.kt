package de.unisaarland.cs.se.selab.loggers
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
import java.io.PrintWriter

object Logger {

    private var currentLevel: LogLevel? = null
    private var outputHandle: PrintWriter = PrintWriter(System.out)
    var restaurantID: Id = -1

    private fun shouldLog(level: LogLevel?): Boolean {
        return when (currentLevel) {
            LogLevel.DEBUG -> true
            LogLevel.INFO -> level != LogLevel.DEBUG
            LogLevel.IMPORTANT -> level == LogLevel.IMPORTANT
            null -> throw IllegalArgumentException("LogLevel is not correct")
        }
    }

    fun setup(level: LogLevel?) {
        currentLevel = level
    }

    fun setup(writer: PrintWriter) {
        outputHandle = writer
    }

    fun log(level: LogLevel?, message: String) {
        if (shouldLog(level)) {
            outputHandle.println("[$level] $message")
            outputHandle.flush()
        }
    }
}
