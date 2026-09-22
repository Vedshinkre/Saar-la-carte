package de.unisaarland.cs.se.selab.systemtest.selab26.losttest

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Validates that a REGULAR group:
 * 1. Fails table reservation when an EVENT group reserves all available tables (Evening 4).
 * 2. Leaves an immediate NEGATIVE rating on Tick 1 of Evening 4.
 * 3. Fails table reservation a second consecutive time due to an EVENT group on Evening 5.
 * 4. Leaves a second NEGATIVE rating on Tick 1 of Evening 5.
 * 5. Permanently de-registers and never visits on Evening 6.
 */
class RegularFailedTest : ExampleSystemTestExtension() {

    private companion object {
        const val NO_RESERVING =
            "[IMPORTANT] FOH No Reserving (R 1): No table could be reserved for group 2."
        const val RATING_PREFIX =
            "[INFO] Rating (R 1): Group 2 rates the restaurant 1 with NEGATIVE rating"
    }

    override val name = "RegularFailedTest"
    override val description = "Regular fails reservation twice due to events and deregisters"
    override val restaurants = "twotimesthecharm/restaurants.json"
    override val scenario = "twotimesthecharm/scenario.json"
    override val food = "twotimesthecharm/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 123 // 5 evenings (120 ticks) + 3 ticks of Evening 6

    override suspend fun run() {
        assertEvening1Browsing()
        assertEvening2Browsing()
        assertEvening4Strike1()
        assertEvening5Strike2()
        assertEvening6Absence()
    }

    private suspend fun assertEvening1Browsing() {
        // Event 1 decides 3 evenings prior to EventEvening 4 -> Evening 1, Tick 1
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(groupId = 1, restId = 1))
    }

    private suspend fun assertEvening2Browsing() {
        // Event 3 decides 3 evenings prior to EventEvening 5 -> Evening 2, Tick 1
        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        assertNextLine(TickStatusTestLogs.restDecision(groupId = 3, restId = 1))
    }

    private suspend fun assertEvening4Strike1() {
        // Group 2 is evaluated next and fails reservation (Strike 1).
        skipUntilString("[IMPORTANT] Preparation: Preparation for evening 4 starts.")
        assertNextLine(NO_RESERVING)

        // Tick 1: Group 2 rates NEGATIVE immediately due to failed reservation
        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(RATING_PREFIX)
    }

    private suspend fun assertEvening5Strike2() {
        // Group 2 is evaluated next and fails reservation (Strike 2).
        skipUntilString("[IMPORTANT] Preparation: Preparation for evening 5 starts.")
        assertNextLine(NO_RESERVING)

        // Tick 1: Group 2 rates NEGATIVE immediately, triggering permanent de-registration
        skipUntilString(TickStatusTestLogs.tickStart(1, 5))
        skipUntilString(RATING_PREFIX)
    }

    private suspend fun assertEvening6Absence() {
        // Tick 3 of Evening 6 (Group 2's visitingTick)
        skipUntilString(TickStatusTestLogs.tickStart(3, 6))

        // If Group 2 were still active, it would arrive and be seated at table 2 in Tick 3.
        // Confirm that 0 customers were seated and 0 orders were placed.
        skipUntilString("[DEBUG] FOH Seating Status (R 1): 0 waitstaff seated 0 customers on 0 tables.")
        assertNextLine(
            "[DEBUG] FOH Ordering Status (R 1): " +
                "The restaurant received orders from 0 customers, 0 waitstaff took orders."
        )
    }
}
