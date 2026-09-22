package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs

private const val DIR = "reservationdecisionjson/positiverating"

/**
 * Probe for the tutors' mandatory "ReservedForMe" component test (RRR: reservation of REGULAR
 * groups and their ratings). Specification page 22: a neutral experience becomes a positive rating
 * ("All customers that leave a rating will leave a positive rating in case of neutral experiences").
 * Every existing rating-related system test asserts a NEGATIVE outcome (a group turned away); none
 * pins down the POSITIVE case for a REGULAR group that is simply seated, served and finishes its
 * meal without incident.
 */
class RegularUneventfulVisitRatesPositiveSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularUneventfulVisitRatesPositiveSystemTest"
    override val description = "A REGULAR group with no incident during its visit rates POSITIVE (RRR probe)"
    override val logLevel = "DEBUG"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"
    override val maxTicks = 24

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))

        skipUntilString(FohServiceTestLogs.rating(1, 1, "POSITIVE", 1, 0))
    }
}
