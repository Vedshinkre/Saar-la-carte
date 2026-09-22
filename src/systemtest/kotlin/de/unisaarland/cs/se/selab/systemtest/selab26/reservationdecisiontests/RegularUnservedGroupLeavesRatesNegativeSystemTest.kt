package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "reservationdecisionjson/unservedgroup"

/**
 * Probe for the tutors' mandatory "ReservedForMe" component test (RRR: reservation of REGULAR
 * groups and their ratings). Specification page 23: "a negative experience is if the food arrives
 * too late or not at all for at least one customer in the group", and the REGULAR-specific rule
 * (page 20) counts "the whole group leaving the restaurant because no one was served food" as a
 * failed attempt. Every existing rating probe ties the NEGATIVE rating to a failure to be seated;
 * none pins down the case where a REGULAR group is seated and orders normally, but the kitchen
 * never gets around to cooking its dish before its patience runs out.
 *
 * The restaurant has a single TOURNANT cook and three tables of 2 that exactly fit three REGULAR
 * groups of 2, all arriving and ordering at tick 2. Groups 1 and 2 (lower ids) each order their own
 * dish, so the lone cook works through both of their orders first (40 minute duration each, the
 * maximum), before it could ever reach group 3's order. Group 3 (the target) therefore never has a
 * single dish served, so by the time its patience for an unserved order runs out (4 ticks after
 * ordering, i.e. tick 6), the whole group leaves and rates NEGATIVE. Group 2's own dish only starts
 * cooking that same tick 6 (right after group 1's is done), so it is still not served in time
 * either and leaves alongside group 3 - which is why the rating assertion below does not pin down
 * an exact positive/negative count, only that group 3's rating is NEGATIVE.
 */
class RegularUnservedGroupLeavesRatesNegativeSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularUnservedGroupLeavesRatesNegativeSystemTest"
    override val description =
        "A REGULAR group that never gets a single dish served leaves as a whole and rates NEGATIVE (RRR probe)"
    override val logLevel = "DEBUG"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"
    override val maxTicks = 6

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 2, 3, 3))
        skipUntilString("[INFO] Rating (R 1): Group 3 rates the restaurant 1 with NEGATIVE rating,")
    }
}
