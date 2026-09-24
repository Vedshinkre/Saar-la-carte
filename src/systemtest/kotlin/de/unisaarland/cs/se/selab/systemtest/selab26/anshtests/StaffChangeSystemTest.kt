package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val STAFF = "STAFF"

/**
 * for F31
 * one tournant cook, one waiter
 *
 * incident 1 hires a pastry cook before evening 1 -> group 1 can order its fav tart (pastry only)
 * incident 2 removes 5 waiters before evening 2 -> clamped at 0
 * evening 2: no waiter in ticks 1 and 2 -> group rates negative
 */
class StaffChangeSystemTest : ExampleSystemTestExtension() {
    override val name = "StaffChangeHiresCookAndRemovesAllWaiters"
    override val description = "STAFF incidents add a new cook type and clamp the waitstaff at 0"
    override val food = "anshtests/staffchange/food.json"
    override val restaurants = "anshtests/staffchange/restaurants.json"
    override val scenario = "anshtests/staffchange/scenario.json"
    override val logLevel = "INFO"
    override val maxTicks = 27

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.incident(1, STAFF, 1))
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))

        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("tart" to 2), 1))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "PASTRY", 2, "tart", 1, listOf(1)))

        skipUntilString(TickStatusTestLogs.servingEnd(1))
        assertNextLine(InitialAndPrepTestLogs.incident(2, STAFF, 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))

        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 1))
        assertNextLine(TickStatusTestLogs.tickStart(2, 2))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 1))
        assertNextLine(FohServiceTestLogs.rating(1, 1, "NEGATIVE", 1, 1))
        assertNextLine(TickStatusTestLogs.tickStart(3, 2))
    }
}
