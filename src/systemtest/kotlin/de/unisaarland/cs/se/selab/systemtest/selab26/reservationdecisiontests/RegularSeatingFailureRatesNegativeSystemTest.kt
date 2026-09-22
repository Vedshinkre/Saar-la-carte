package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "reservationdecisionjson/seatingfailure"

/**
 * Probe for the tutors' mandatory "ReservedForMe" component test (RRR: reservation of REGULAR
 * groups and their ratings). Specification page 15: "In case there is no free waiter, the customer
 * group tries again the next tick and then leaves, the group does not reconsider." Page 23 lists
 * this among the negative experiences: "An experience counts as negative in case there is no staff
 * or table available and the customer group has to leave the restaurant without getting seated."
 * Every existing rating probe ties a NEGATIVE rating to a *reservation* failure
 * ([RegularReservationOrderIsAscendingIdNotFileOrderSystemTest]); none pins down the case where the
 * reservation succeeds but the waitstaff is unavailable at both the arrival tick and the retry tick.
 *
 * The restaurant has a single waiter and three tables that exactly fit their groups (2, 10 and 10
 * seats). Two REGULAR groups of 10 (ids 1 and 2) each alone saturate the waiter's SEATING tick load
 * (10/10) the tick they arrive. REGULAR group 3 (size 2) reserved the table of 2 and arrives at
 * tick 2 together with group 1: group 1 is seated first (lower id) and exhausts the waiter, so
 * group 3 finds no free waiter. It retries at tick 3, where group 2 arrives and again exhausts the
 * waiter first (lower id than group 3), so group 3 leaves for good and rates NEGATIVE.
 */
class RegularSeatingFailureRatesNegativeSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularSeatingFailureRatesNegativeSystemTest"
    override val description =
        "A REGULAR group that reserved a table but never finds a free waiter rates NEGATIVE (RRR probe)"
    override val logLevel = "DEBUG"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"
    override val maxTicks = 3

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 2, listOf(1)))
        skipUntilString(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 3))

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 3, listOf(1)))
        skipUntilString(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 3))

        skipUntilString(FohServiceTestLogs.rating(1, 3, "NEGATIVE", 0, 1))
    }
}
