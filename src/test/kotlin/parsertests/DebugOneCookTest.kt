package parsertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.parsers.ParserController
import de.unisaarland.cs.se.selab.system.Simulation
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

class DebugOneCookTest {
    @Test
    fun debug() {
        val output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        val food = "src/systemtest/resources/notablereservationjson/food.json"
        val restaurants = "src/systemtest/resources/notablereservationjson/restaurants.json"
        val scenario = "src/systemtest/resources/notablereservationjson/scenario.json"
        val result = ParserController().parseFiles(food, restaurants, scenario)
        System.err.println("DEBUG wasInvalidFile=" + result.wasInvalidFile)
        if (!result.wasInvalidFile) {
            Time.setMaxTicks(5)
            val sim = Simulation(result)
            try {
                sim.runSimulation()
            } catch (e: Throwable) {
                System.err.println("DEBUG EXCEPTION: " + e)
            }
        }
        System.err.println("DEBUG SIM output=[\n" + output.toString() + "]")
    }
}
