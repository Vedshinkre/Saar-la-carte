package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.api.SystemTestAssertionError
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "reservationdecisionjson/mixedconsecutivefailure"

/**
 * Probe for the tutors' mandatory "ReservedForMe" component test (RRR: reservation of REGULAR
 * groups and their ratings). Specification: "After two *consecutive* failed attempts, which are
 * either failed reservations, failures to be seated by the waitstaff, or the whole group leaving
 * the restaurant because no one was served food, they do not visit a restaurant again." Every
 * existing consecutive-failure probe ([RegularFailedAttemptsNotResetByInterveningSuccessSystemTest])
 * only exercises *one* of these three failure kinds (failed reservations) in a row; none checks
 * that two failures of *different* kinds still count towards the same streak.
 *
 * The restaurant has one COMMON table of 2, two COMMON tables of 10, and a single waiter.
 *
 * - Evening 1: a REGULAR group of 2 (id 1, period 10, so it only ever visits evening 1) reserves
 *   the table of 2 before our target (id 4, period 1) can, so the target's *reservation* fails.
 *   Two more REGULAR groups of 10 (ids 2 and 3, period 1) reserve the two tables of 10, purely so
 *   that on evening 2 they can saturate the restaurant's single waiter.
 * - Evening 2: id 1 does not return (its next visit is evening 11), freeing the table of 2 for the
 *   target, whose reservation now succeeds. It arrives at the same tick as group 2 (size 10), which
 *   exhausts the waiter's SEATING tick load first (lower id); the target retries the following
 *   tick, where group 3 (size 10) exhausts the waiter again first. The target's *seating* fails on
 *   both ticks, so it leaves and rates NEGATIVE: its second failed attempt in a row.
 * - Evening 3: with two *consecutive* failures (reservation, then seating), the target must not
 *   visit again: no reservation attempt, no arrival, nothing mentioning it at all.
 */
class RegularConsecutiveFailuresAcrossDifferentFailureTypesSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularConsecutiveFailuresAcrossDifferentFailureTypesSystemTest"
    override val description =
        "A failed reservation followed by a failed seating still counts as two failures in a row (RRR probe)"
    override val logLevel = "DEBUG"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"

    /** third tick of evening 3: (3 - 1) * 24 + 3 */
    override val maxTicks = 51

    override suspend fun run() {
        // Evening 1: group 1 (lower id) wins the only table of 2, so group 4 (target) fails its
        // reservation and rates NEGATIVE right away, as the very first rating of the simulation.
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 4))
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 4, "NEGATIVE", 0, 1))

        // Evening 2: group 1 does not return, so the target's reservation succeeds this time, but
        // it never finds a free waiter.
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))

        skipUntilString(TickStatusTestLogs.tickStart(2, 2))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))
        skipUntilString(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 4))

        skipUntilString(TickStatusTestLogs.tickStart(3, 2))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(1)))
        skipUntilString(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 4))

        // the exact positive/negative counts by now depend on when groups 2 and 3 finish eating
        // in the meantime, so only the rating type is pinned down here
        skipUntilString("[INFO] Rating (R 1): Group 4 rates the restaurant 1 with NEGATIVE rating,")

        // Evening 3: two consecutive failures (reservation, then seating) mean group 4 does not
        // visit again: nothing in the rest of the captured log may mention it.
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        var line = getNextLine()
        while (line != null) {
            if (line.contains("Group 4")) {
                throw SystemTestAssertionError("Group 4 should not visit again but got '$line'")
            }
            line = getNextLine()
        }
    }
}
