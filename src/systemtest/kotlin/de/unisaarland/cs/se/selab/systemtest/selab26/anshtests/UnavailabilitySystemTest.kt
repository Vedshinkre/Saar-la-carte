package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * for F34
 * regular group 1 (1 customer) likes paella, uses a whole 10 g saffron package, eats it on evening 1
 * saffron unavailable for evening 2 only: nothing bought, none left in pantry -> orders risotto
 * evening 3: saffron bought again -> paella again
 */
class UnavailabilitySystemTest : ExampleSystemTestExtension() {
    override val name = "UnavailableForOneEvening"
    override val description = "An UNAVAILABLE incident of duration 1 blocks procurement and ordering for its evening"
    override val food = "anshtests/unavailability/food.json"
    override val restaurants = "anshtests/unavailability/restaurants.json"
    override val scenario = "anshtests/unavailability/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 49

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("paella" to 1), 1))

        skipUntilString(TickStatusTestLogs.servingEnd(1))
        assertNextLine(InitialAndPrepTestLogs.incident(1, "UNAVAILABLE", 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 2, mapOf("risotto" to 1), 1))

        skipUntilString(TickStatusTestLogs.servingEnd(2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 10, "g", "rice"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 10, "g", "saffron"))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 3, mapOf("paella" to 1), 1))
    }
}
