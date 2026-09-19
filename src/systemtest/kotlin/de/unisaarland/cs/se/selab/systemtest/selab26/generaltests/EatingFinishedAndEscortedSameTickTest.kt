package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs

/**
 * A CASUAL pair is served and then eats. Eating (step 5) is followed by escorting (step 6)
 * within the same tick, so the tick in which the group finishes eating is also the tick in
 * which it is escorted out - the escorting lines must directly follow the eating lines.
 */
class EatingFinishedAndEscortedSameTickTest : ExampleSystemTestExtension() {
    override val name = "EatingFinishedAndEscortedSameTickTest"
    override val description = "A group that finishes eating is escorted out in that same tick"
    override val restaurants = "eatfinishescortsametickjson/restaurants.json"
    override val scenario = "eatfinishescortsametickjson/scenario.json"
    override val food = "eatfinishescortsametickjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        // whichever tick the group finishes eating in, the very next lines are its escorting
        skipUntilString(FohServiceTestLogs.finishedEating(1, 2, 1, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 2))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 2, 1, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 2))
    }
}
