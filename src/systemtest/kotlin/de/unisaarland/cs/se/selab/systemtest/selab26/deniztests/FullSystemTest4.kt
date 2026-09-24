package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val G = "g"
private const val POSITIVE = "POSITIVE"
private const val NEGATIVE = "NEGATIVE"
private const val SOUP = "soup"

/**
 * Two REGULAR groups compete for one table every evening: the loser is rated negative twice in a
 * row and then stops visiting entirely, exactly as the spec's consecutive-failure rule demands.
 * Meanwhile an EVENT group reserved three evenings ahead needs two merged tables and two waiters
 * to seat, serve and escort its 15 guests once its evening arrives.
 */
class FullSystemTest4 : ExampleSystemTestExtension() {
    override val name = "FullSystemTest4"
    override val description =
        "Regular reservation streak stops a group after two failures; event needs merged tables and two waiters"
    override val food = "deniztests/fullsystemtest4/food.json"
    override val restaurants = "deniztests/fullsystemtest4/restaurants.json"
    override val scenario = "deniztests/fullsystemtest4/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        assertEveningOneReservationBattleAndVisits()
        assertGroupTwoStopsAfterTwoFailures()
        assertEventMultiWaiterVisitOnEveningFour()
    }

    private suspend fun assertEveningOneReservationBattleAndVisits() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, G, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, G, "tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        // Group 2 loses the Annex's single table to lower-id group 1
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(2, 2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 100, G, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 100, G, "tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))

        skipUntilString(TickStatusTestLogs.servingStart(1))
        assertNextLine(TickStatusTestLogs.tickStart(1, 1))
        // The event group reserves its tables three evenings before its actual visit
        assertNextLine(TickStatusTestLogs.restDecision(4, 1))
        skipUntilString(FohArrivalTestLogs.arrival(2, 1))
        assertNextLine(FohArrivalTestLogs.seating(2, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 1, 1, mapOf(SOUP to 2), 1))
        // The failed reservation is rated on tick 1 of serving
        skipUntilString(FohServiceTestLogs.rating(2, 2, NEGATIVE, 1, 1))

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(3, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 3, 2, mapOf(SOUP to 2), 1))
        skipUntilString(FohServiceTestLogs.rating(2, 1, POSITIVE, 2, 1))

        skipUntilString(FohServiceTestLogs.rating(1, 3, POSITIVE, 5, 0))
    }

    private suspend fun assertGroupTwoStopsAfterTwoFailures() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(2, 2))
        skipUntilString(FohServiceTestLogs.rating(2, 2, NEGATIVE, 2, 2))

        // Two consecutive failed reservations stop group 2 for good: evening 3's preparation
        // has no failed-reservation line at all, only group 1's table is ever requested again
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))
    }

    private suspend fun assertEventMultiWaiterVisitOnEveningFour() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 4, listOf(1, 2), 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 1, listOf(1, 2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 4, 6, mapOf(SOUP to 15), listOf(1, 2)))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(SOUP to 10), 1, 0))
        assertNextLine(FohServiceTestLogs.serving(1, 2, mapOf(SOUP to 5), 1, 0))

        skipUntilString(FohServiceTestLogs.finishedEating(1, 15, 4, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 15))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 10, 4, 1))
        assertNextLine(FohServiceTestLogs.escorting(1, 2, 5, 4, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 2, 15))
        assertNextLine(FohServiceTestLogs.rating(1, 4, POSITIVE, 6, 0))
    }
}
