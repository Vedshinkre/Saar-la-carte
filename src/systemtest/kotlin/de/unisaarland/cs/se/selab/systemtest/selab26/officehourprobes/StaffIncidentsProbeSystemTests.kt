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
 * Narrowing probes (Sep 23) for FullTest-staff-incidents, which passed on our implementation and
 * failed on the reference (run 10). The full test replays a whole log, so its failure does not say
 * where the difference is. Each probe checks one of the places where that log is asserted line by
 * line. On the reference (runs 11-12) six of them pass. Only
 * [StaffIncidentsLateCasualLeavesSilentlySystemTest] fails, which puts the difference in tick 22
 * of evening 3.
 *
 * Private copy of `fulltests/scenarios/staff-incidents`: one SOUS cook, two waiters, two COMMON
 * tables of 4. REGULAR groups 1 and 2 (4 people each) reserve them every evening. REGULAR group 3
 * (5 people) never gets a table. In evening 3, three CASUAL groups arrive at tick 21 and group 10
 * finds no free waiter. Before evening 4, one STAFF incident adds a waiter and another removes the
 * only cook.
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

/** Group 3's failed reservation is rated NEGATIVE within tick 1, not at its visiting tick 7 (adjustment #25). */
class StaffIncidentsFailedReservationRatesInTickOneSystemTest : StaffIncidentsScenario() {
    override val name = "StaffIncidentsFailedReservationRatesInTickOneSystemTest"
    override val description = "A REGULAR group without a reserved table rates in tick 1 of the evening"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 3, NEGATIVE, 0, 1))
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
    }
}

/** After one failed reservation group 3 tries again: its "No Reserving" line opens the evening 2 preparation. */
class StaffIncidentsRegularTriesAgainAfterOneFailureSystemTest : StaffIncidentsScenario() {
    override val name = "StaffIncidentsRegularTriesAgainAfterOneFailureSystemTest"
    override val description = "A REGULAR group that failed to reserve once tries again the next evening"

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 3))
    }
}

/**
 * After two failed reservations in a row group 3 stops visiting, so the evening 3 preparation does
 * not start with a "No Reserving" line.
 */
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
 * Expects nothing to be logged for CASUAL group 10 in tick 22 of evening 3. The group found no
 * free waiter in tick 21, and no customers are seated in the last three ticks. So the restaurant
 * start should be followed directly by an empty seating status.
 *
 * This expectation was a guess, not a paired reading. It fails on the reference (runs 11-12), which
 * logs a line for the waiting group there (adjustments #16 and #17 cover waiting customers in the
 * last three ticks).
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

/** Both STAFF incidents are logged in id order, right after evening 3's serving ends, then evening 4's preparation. */
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
 * With its only cook gone in evening 4, REGULAR group 1 is still seated at its reserved table. It
 * gets "FOH No Ordering" directly after the seating, then rates NEGATIVE.
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

/** The four statistics lines: 42 meals cooked and served in evenings 1-3, none delivered, 10 ratings. */
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
