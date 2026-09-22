package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "reservationdecisionjson/consecutivefailure"

/**
 * Probe for the tutors' mandatory "ReservedForMe" component test (RRR: reservation of REGULAR
 * groups and their ratings). Specification: "After two *consecutive* failed attempts ... they do
 * not visit a restaurant again." `RegularGroup.failedAttempts`
 * (src/main/kotlin/.../customer/RegularGroup.kt) is a plain counter that is incremented on every
 * failed reservation and is never reset, including by a successful visit in between. That models
 * "two failed attempts, ever", not "two failed attempts in a row" as the spec states.
 *
 * The restaurant has one COMMON table (2 seats) and one always-free BAR table (20 seats, wrong type
 * for either group, so it never resolves a reservation, but keeps the kitchen's estimate of "other
 * seats" positive every evening).
 *
 * Group 1 (id 1, period 4: evenings 1, 5, 9, ...) always wins the COMMON table over group 2 (id 2,
 * period 2: evenings 1, 3, 5, 7, ...), because reservations are granted in ascending group id.
 * Group 2's timeline is: evening 1 fails (1st attempt), evening 3 succeeds alone (breaks the streak),
 * evening 5 fails again (2nd attempt overall, but 1st of a new streak). Per the spec, group 2 should
 * still be visiting on evening 7 since it never failed twice *in a row*. Our counter instead reaches
 * 2 after evening 5 and permanently excludes group 2 from `isVisitingTonight()`, so it silently
 * disappears from evening 7 onward: no reservation attempt, no arrival, nothing.
 */
class RegularFailedAttemptsNotResetByInterveningSuccessSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularFailedAttemptsNotResetByInterveningSuccessSystemTest"
    override val description =
        "A regular that fails, then succeeds, then fails again should still visit next time (RRR probe)"
    override val logLevel = "DEBUG"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"

    /** first tick of evening 7: (7 - 1) * 24 + 1 */
    override val maxTicks = 145

    override suspend fun run() {
        // Evening 1: group 1 (lower id) is reserved for first, group 2 fails
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 2))

        // Evening 2 does not exist for either group (period 4 / period 2, both start at 1):
        // evening 3 is group 2's next visit, alone, and it succeeds
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        skipUntilString(TickStatusTestLogs.tickStart(1, 3))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 1, listOf(1)))

        // Evening 5: group 1 is back and wins the table again, group 2 fails a second time,
        // but not consecutively: evening 3 succeeded in between
        skipUntilString(InitialAndPrepTestLogs.prepStart(5))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 2))

        // Evening 7: group 1 is not visiting (period 4), so the table is free. Group 2 only failed
        // once in a row, so per the spec it should still be trying tonight and succeed.
        skipUntilString(InitialAndPrepTestLogs.prepStart(7))
        skipUntilString(TickStatusTestLogs.tickStart(1, 7))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 1, listOf(1)))
    }
}
