package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs

/** Favorites, exclusions and preferred-ingredient counts pick dishes, ties go to the highest id. */
class PreferencesSystemTest : ExampleSystemTestExtension() {
    override val name = "PreferencesSystemTest"
    override val description = "Favorites, exclusions, preferred ingredients by count and highest id tie-breaks"
    override val food = "deniztests/preferences/food.json"
    override val restaurants = "deniztests/preferences/restaurants.json"
    override val scenario = "deniztests/preferences/scenario.json"
    override val logLevel = "IMPORTANT"
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(
            FohArrivalTestLogs.ordering(1, 1, 1, mapOf("gratin" to 1, "pie" to 1, "soup" to 1, "stew" to 2), 1)
        )
    }
}
