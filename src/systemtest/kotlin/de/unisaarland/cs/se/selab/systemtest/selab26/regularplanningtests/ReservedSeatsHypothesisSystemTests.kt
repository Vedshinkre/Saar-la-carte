package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val HYP_DIR = "recipeprobejson"
private const val HYP_UNIT = "g"
private const val RICE = "rice"

private fun hypothesis(file: String) = "$HYP_DIR/$file"

/**
 * Written on Sep 21, after [CasualConsumptionCarryOverSystemTest] passed on the reference. That
 * left the estimate on an evening with a REGULAR reservation as the only possible difference. At
 * the time we read "the seats on all other tables" (specification page 12, line 1) as every seat
 * not on a reserved table, and bought 3 meals (30 g). These two tests check the two other
 * explanations. Both passed on the reference (run 3), and our implementation was fixed to match.
 *
 * A 2 seat and a 30 seat table, one 10 g dish, and a REGULAR group of 2 on its first visit.
 */
abstract class ReservedSeatsHypothesisSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val food = hypothesis("historyFood.json")
    override val restaurants = hypothesis("historyRestaurants.json")
    override val scenario = hypothesis("historyScenario.json")
    override val maxTicks = 1
}

/**
 * "All other tables" means the tables the kitchen has not already planned for. A first-time
 * REGULAR group has no history, so nothing is planned for its table and its 2 seats still count:
 * 32 seats, 4 meals, 40 g.
 */
class RegularFirstVisitCountsReservedSeatsSystemTest : ReservedSeatsHypothesisSystemTest() {
    override val name = "RegularFirstVisitCountsReservedSeatsSystemTest"
    override val description = "A first-visit REGULAR's reserved seats still count for the estimate"

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, ALL_SEATS_COUNT, HYP_UNIT, RICE))
    }

    private companion object {
        /** 4 meals of 10 g for all 32 seats */
        const val ALL_SEATS_COUNT = 40
    }
}

/**
 * Rules out the other explanation: that the reference reserves the 30 seat table, which would leave
 * 2 other seats and plan 10 g. The seating line of tick 1 names the table: the group of 2 is seated
 * at table 1, the exact fit.
 */
class RegularReservedTableIsTheExactFitSystemTest : ReservedSeatsHypothesisSystemTest() {
    override val name = "RegularReservedTableIsTheExactFitSystemTest"
    override val description = "A REGULAR group of 2 reserves the 2 seat table, not the 30 seat one"

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, EXACT_FIT_TABLE, listOf(1)))
    }

    private companion object {
        const val EXACT_FIT_TABLE = 1
    }
}
