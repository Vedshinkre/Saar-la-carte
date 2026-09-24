package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** Groups are served by id within capacity, a table that does not fit is served by dish priority. */
class ServingSystemTest : ExampleSystemTestExtension() {
    override val name = "ServingSystemTest"
    override val description = "Serving by id, waiting at the capacity limit, partial serving by dish priority"
    override val food = "deniztests/serving/food.json"
    override val restaurants = "deniztests/serving/restaurants.json"
    override val scenario = "deniztests/serving/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf("salad" to 3), 1, 1))
        assertNextLine(FohServiceTestLogs.noServing(1, 1, 8, 3))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 3))

        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf("stew" to 7), 2, 1))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf("soup" to 2, "tart" to 1), 3, 3))
        assertNextLine(FohServiceTestLogs.noServing(1, 1, 5, 3))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 10))

        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf("pie" to 3, "tart" to 2), 3, 4))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 5))
    }
}
