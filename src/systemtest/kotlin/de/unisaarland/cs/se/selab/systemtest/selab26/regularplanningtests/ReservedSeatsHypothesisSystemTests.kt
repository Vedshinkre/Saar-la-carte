package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val HYP_DIR = "recipeprobejson"
private const val HYP_UNIT = "g"
private const val RICE = "rice"

private fun hypothesis(file: String) = "$HYP_DIR/$file"

/**
 * [RegularReservationEstimateSystemTest] fails against the reference while
 * [CasualConsumptionCarryOverSystemTest] passes, so the difference is in the estimate of an evening
 * on which a REGULAR group holds a reservation, and nowhere else. The restaurant has a 2 seat and a
 * 30 seat table and a single 10 g dish; the group of 2 reserves on its first ever visit, so nothing
 * is planned for its table. We buy 3 meals (30 g), reading "the seats on all other tables"
 * (specification page 12, line 1) as every seat that is not on a reserved table.
 *
 * These two tests state the two competing readings so that one run names the right one. They are
 * expected to fail against our own implementation: that is the point.
 */
abstract class ReservedSeatsHypothesisSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val food = hypothesis("historyFood.json")
    override val restaurants = hypothesis("historyRestaurants.json")
    override val scenario = hypothesis("historyScenario.json")
    override val maxTicks = 1
}

/**
 * Reading A: "all other tables" means all tables that the kitchen did not already plan for. A
 * REGULAR group on its first visit has no order history, so nothing is planned for its table and
 * its 2 seats still count towards the guess: 32 seats, 4 meals, 40 g.
 *
 * That the reference computes 4 meals out of these same 32 seats is already established by
 * [CasualConsumptionCarryOverSystemTest], which uses this very restaurant shape without a
 * reservation and passes.
 */
class RegularFirstVisitCountsReservedSeatsSystemTest : ReservedSeatsHypothesisSystemTest() {
    override val name = "RegularFirstVisitCountsReservedSeatsSystemTest"
    override val description = "Reading A: a first-visit REGULAR's reserved seats still count for the estimate"

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
 * Reading B: the reference picks a different table for the reservation. If the group of 2 were put
 * on the 30 seat table instead of the 2 seat one, only 2 seats would be left over and the estimate
 * would be a single meal, 10 g.
 *
 * The seating log of tick 1 names the reserved table directly, which settles the question on its
 * own: we reserve table 1, the perfect fit.
 */
class RegularReservedTableIsTheExactFitSystemTest : ReservedSeatsHypothesisSystemTest() {
    override val name = "RegularReservedTableIsTheExactFitSystemTest"
    override val description = "Reading B: which table a REGULAR group of 2 reserves out of a 2 seat and a 30 seat one"

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, EXACT_FIT_TABLE, listOf(1)))
    }

    private companion object {
        const val EXACT_FIT_TABLE = 1
    }
}
