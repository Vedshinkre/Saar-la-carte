package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val G = "g"
private const val POSITIVE = "POSITIVE"
private const val STEW = "stew"

/**
 * All four incident types across a four-evening run: a PACKAGING change is visible in the very
 * first procurement, a STAFF cut only cripples the restaurant it targets (its sibling with the
 * same menu is unaffected), an UNAVAILABLE ingredient blocks an evening's supply and later
 * recovers, and a RECIPE change quietly adjusts a later evening's cooking.
 */
class FullSystemTest2 : ExampleSystemTestExtension() {
    override val name = "FullSystemTest2"
    override val description =
        "All four incident types: packaging, restaurant-scoped staff cut, ingredient outage and recovery, recipe change"
    override val food = "deniztests/fullsystemtest2/food.json"
    override val restaurants = "deniztests/fullsystemtest2/restaurants.json"
    override val scenario = "deniztests/fullsystemtest2/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        assertPackagingIncidentAndEveningOne()
        assertStaffAndUnavailableIncidentsEveningTwo()
        assertRecipeIncidentEveningThree()
        assertEventVisitAfterRecovery()
    }

    private suspend fun assertPackagingIncidentAndEveningOne() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, "PACKAGING", 1))
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        // The packaging incident makes every beef delivery a 100 g package instead of 30 g
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, G, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 20, G, "lettuce"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 20, G, "tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 100, G, "beef"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 20, G, "lettuce"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 20, G, "tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))

        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(STEW to 2), 1))
        skipUntilString(FohServiceTestLogs.rating(1, 1, POSITIVE, 6, 0))
    }

    private suspend fun assertStaffAndUnavailableIncidentsEveningTwo() {
        skipUntilString(InitialAndPrepTestLogs.incident(2, "STAFF", 2))
        assertNextLine(InitialAndPrepTestLogs.incident(3, "UNAVAILABLE", 2))

        // Restaurant 2 keeps its SOUS cook: group 3's delivery order for salad succeeds there
        skipUntilString(FohArrivalTestLogs.ordering(2, 3, 3, mapOf("salad" to 2), null))

        // Restaurant 1 lost its only SOUS cook: group 2's salad preference falls back to stew
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 4, mapOf(STEW to 2), 1))

        skipUntilString(FohServiceTestLogs.rating(2, 3, POSITIVE, 2, 0))
        skipUntilString(FohServiceTestLogs.rating(1, 2, POSITIVE, 8, 0))
    }

    private suspend fun assertRecipeIncidentEveningThree() {
        skipUntilString(InitialAndPrepTestLogs.incident(4, "RECIPE", 3))
        assertNextLine(InitialAndPrepTestLogs.prepStart(3))

        // The regular group's periodic visit continues unaffected by the recipe change
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 5, mapOf(STEW to 2), 1))
        skipUntilString(FohServiceTestLogs.rating(1, 1, POSITIVE, 9, 0))
    }

    private suspend fun assertEventVisitAfterRecovery() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 4))
        // The tomato shortage has ended: the event group gets its favourite basic dish, soup, again
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 4, listOf(2, 3), 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 4, 7, mapOf("soup" to 4), listOf(1)))
        skipUntilString(FohServiceTestLogs.rating(1, 4, POSITIVE, 10, 0))
    }
}
