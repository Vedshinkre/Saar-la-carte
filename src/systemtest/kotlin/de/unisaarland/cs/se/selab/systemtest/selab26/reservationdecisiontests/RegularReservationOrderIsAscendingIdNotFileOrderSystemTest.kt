package de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "reservationdecisionjson/idordertie"

/**
 * Probe for the tutors' mandatory "ReservedForMe" component test (RRR: reservation of REGULAR
 * groups and their ratings). Specification page 14 states table criteria but never says in what
 * order multiple REGULAR groups competing for the same table are handled; page 31/37 establish
 * "ascending id" as the general convention for arrivals, serving and rating.
 * `Simulation.executePreparationPhase` (src/main/kotlin/.../system/Simulation.kt) sorts regulars by
 * id before handing them to `Restaurant.prepareForEvening`, so reservation should follow the same
 * convention.
 *
 * The restaurant has a single COMMON table for 2. Two REGULAR groups of 2 both want it on the same
 * evening; the scenario file deliberately lists the higher id (5) before the lower id (3) so that a
 * result keyed to file order rather than id order would show up immediately.
 */
class RegularReservationOrderIsAscendingIdNotFileOrderSystemTest : ExampleSystemTestExtension() {
    override val name = "RegularReservationOrderIsAscendingIdNotFileOrderSystemTest"
    override val description =
        "Two REGULAR groups competing for one table: the lower id wins regardless of file order (RRR probe)"
    override val logLevel = "DEBUG"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"
    override val maxTicks = 1

    override suspend fun run() {
        // group 3 (lower id) is reserved first and wins the only table, so group 5 is the one
        // that fails, even though it is listed first in the scenario file
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 5))

        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 1, listOf(1)))

        // group 5 never arrives and rates negatively in the first tick, same as a group turned away
        // by a full house
        skipUntilString(FohServiceTestLogs.rating(1, 5, "NEGATIVE", 0, 1))
    }
}
