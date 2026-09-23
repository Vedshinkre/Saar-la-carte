package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.api.SystemTestAssertionError
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val NEGATIVE = "NEGATIVE"

/**
 * Narrowing probes for FullTest-staff-incidents, which passes on our implementation but fails
 * against the reference (results 10). The full test replays a whole log and cannot say which part
 * the reference disagrees with, so each probe below checks one of the places where that log is
 * asserted line by line instead of skipped over. All of them pass on our implementation, so the
 * one that fails on the next reference run names the divergence.
 *
 * None of the divergences found so far can be the cause: the scenario has no drivers, and only one
 * cook, so neither the delivery rules nor the idle-cook tie-break come into play.
 *
 * The scenario (a private copy of `fulltests/scenarios/staff-incidents`): one restaurant with one
 * SOUS cook, two waiters and two COMMON tables of 4. REGULAR groups 1 and 2 (4 people each) reserve
 * those tables every evening; REGULAR group 3 (5 people) never finds a table. In evening 3, three
 * CASUAL groups arrive at tick 21, and group 10 finds no free waiter. Before evening 4, a staff
 * incident adds a waiter and another removes the only cook, so nobody can order.
 */
abstract class StaffIncidentsScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/staffincidents/restaurants.json"
    override val scenario = "officehourjson/staffincidents/scenario.json"
    override val food = "officehourjson/staffincidents/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 96

    /** the next line of the log must not start with [prefix] */
    protected suspend fun assertNextLineIsNot(prefix: String) {
        val line = getNextLine() ?: throw SystemTestAssertionError("End of log reached when there should be more.")
        if (line.startsWith(prefix)) {
            throw SystemTestAssertionError("Did not expect\n    $line")
        }
    }
}

/** Group 3's failed reservation is rated in tick 1 of the evening, not at its visiting tick 7. */
class StaffIncidentsFailedReservationRatesInTickOneSystemTest : StaffIncidentsScenario() {
    override val name = "StaffIncidentsFailedReservationRatesInTickOneSystemTest"
    override val description = "A REGULAR group without a reserved table rates in tick 1 of the evening"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 3, NEGATIVE, 0, 1))
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
    }
}

/** After one failure, group 3 still tries to reserve in evening 2, as the first line of that preparation. */
class StaffIncidentsRegularTriesAgainAfterOneFailureSystemTest : StaffIncidentsScenario() {
    override val name = "StaffIncidentsRegularTriesAgainAfterOneFailureSystemTest"
    override val description = "A REGULAR group that failed to reserve once tries again the next evening"

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 3))
    }
}

/** After two failures in a row, group 3 no longer visits, so evening 3 has no failed reservation. */
class StaffIncidentsRegularStopsAfterTwoFailuresSystemTest : StaffIncidentsScenario() {
    override val name = "StaffIncidentsRegularStopsAfterTwoFailuresSystemTest"
    override val description = "A REGULAR group that failed to reserve twice in a row stops visiting"

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLineIsNot(NO_RESERVING_PREFIX)
    }

    private companion object {
        const val NO_RESERVING_PREFIX = "[IMPORTANT] FOH No Reserving"
    }
}

/**
 * Group 10 found no free waiter in tick 21, and from tick 22 the restaurant takes no new customers,
 * so tick 22 starts with an empty seating status and nothing is logged for group 10 before it.
 */
class StaffIncidentsLateCasualLeavesSilentlySystemTest : StaffIncidentsScenario() {
    override val name = "StaffIncidentsLateCasualLeavesSilentlySystemTest"
    override val description = "A CASUAL group still waiting in the last three ticks leaves without a log"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(LAST_OPEN_TICK + 1, EVENING))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
    }

    private companion object {
        const val LAST_OPEN_TICK = 21
        const val EVENING = 3
    }
}

/** Both staff incidents are logged between the end of evening 3 and the preparation of evening 4. */
class StaffIncidentsIncidentsLoggedBeforePreparationSystemTest : StaffIncidentsScenario() {
    override val name = "StaffIncidentsIncidentsLoggedBeforePreparationSystemTest"
    override val description = "Staff incidents are logged right after the serving of the evening before ends"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.servingEnd(INCIDENT_EVENING - 1))
        assertNextLine(InitialAndPrepTestLogs.incident(1, STAFF, INCIDENT_EVENING))
        assertNextLine(InitialAndPrepTestLogs.incident(2, STAFF, INCIDENT_EVENING))
        assertNextLine(InitialAndPrepTestLogs.prepStart(INCIDENT_EVENING))
    }

    private companion object {
        const val INCIDENT_EVENING = 4
        const val STAFF = "STAFF"
    }
}

/**
 * With its only cook gone, the restaurant still seats REGULAR group 1 at its reserved table, and the
 * group leaves at once because no dish can be ordered.
 */
class StaffIncidentsNoCookSeatsButCannotOrderSystemTest : StaffIncidentsScenario() {
    override val name = "StaffIncidentsNoCookSeatsButCannotOrderSystemTest"
    override val description = "Without any cook a REGULAR group is still seated but cannot order"

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(NO_COOK_EVENING))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 1, GROUP_SIZE))
        skipUntilString(FohServiceTestLogs.rating(1, 1, NEGATIVE, POSITIVE_BEFORE, NEGATIVE_BEFORE + 1))
    }

    private companion object {
        const val NO_COOK_EVENING = 4
        const val GROUP_SIZE = 4
        const val POSITIVE_BEFORE = 6
        const val NEGATIVE_BEFORE = 2
    }
}

/** The totals: 42 meals cooked and served in evenings 1 to 3, nothing delivered, 10 ratings. */
class StaffIncidentsStatisticsSystemTest : StaffIncidentsScenario() {
    override val name = "StaffIncidentsStatisticsSystemTest"
    override val description = "Final statistics of the staff-incidents scenario"

    override suspend fun run() {
        skipUntilString(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, MEALS))
        assertNextLine(StatisticsTestLogs.statsServed(1, MEALS))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(1, RATINGS))
    }

    private companion object {
        const val MEALS = 42
        const val RATINGS = 10
    }
}
