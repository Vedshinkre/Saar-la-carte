package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DISH = "Rice Bowl"

/**
 * Probe for the "FOH No Reserving" line the tutors named as missing in the ReservedForMe component
 * test (specification page 30): the waitstaff manager reserves before the kitchen plans, and a group
 * it cannot seat is told immediately so it never shows up.
 *
 * The restaurant has a single COMMON table of two seats. Group 1 (ten customers) cannot be given a
 * table by any of the six reservation steps, group 2 (two customers) fits it perfectly.
 */
abstract class NoReservationScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/noreservation/restaurants.json"
    override val scenario = "officehourjson/noreservation/scenario.json"
    override val food = "officehourjson/noreservation/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 3
}

/** The failed reservation is logged in the preparation phase, before the kitchen plans. */
class NoReservationIsLoggedSystemTest : NoReservationScenario() {
    override val name = "NoReservationIsLoggedSystemTest"
    override val description = "A REGULAR group that gets no table is logged with FOH No Reserving"

    override suspend fun run() {
        // three "Initialization Info" lines precede the preparation
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 1))
        // only the group that kept its reservation is planned for: two meals of the only dish
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, "g", "rice"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }
}

/**
 * The group that lost its table never arrives, and rates the restaurant in the first tick of the
 * evening (specification page 22: "or in the first tick of the evening when the reservations failed").
 */
class NoReservationRatesInTheFirstTickSystemTest : NoReservationScenario() {
    override val name = "NoReservationRatesInTheFirstTickSystemTest"
    override val description = "A group whose reservation failed rates negatively in the first tick"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        // group 1 has no arrival line at all, the evening starts with group 2
        assertNextLine(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 1, listOf(1)))

        skipUntilString(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.rating(1, 1, "NEGATIVE", 0, 1))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))
    }
}

/**
 * Probe for the merging log the tutors asked us to check (specification page 14, step 4): the
 * tables are merged from the smallest upwards and the smallest ones are then dropped again as long
 * as the rest still seats the group. Tables of 2, 3 and 4 seats for a group of six become 3 + 4,
 * and the merged table keeps the lowest id of the tables it was built from.
 */
abstract class MergingScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/merging/restaurants.json"
    override val scenario = "officehourjson/merging/scenario.json"
    override val food = "officehourjson/merging/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5
}

/** The merge is logged before the seating and names the tables it was built from. */
class MergedTablesAreLoggedSystemTest : MergingScenario() {
    override val name = "MergedTablesAreLoggedSystemTest"
    override val description = "Merging two tables for a REGULAR group is logged with both table ids"

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 1, listOf(3, 4), 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(DISH to 6), 1))
        // the merged table counts as one table in the status of the tick
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 6, 1))
    }
}

/** Every later log of the group uses the merged table, not the tables it was built from. */
class MergedTableIsUsedForServingSystemTest : MergingScenario() {
    override val name = "MergedTableIsUsedForServingSystemTest"
    override val description = "Serving and eating of a merged group are logged on the merged table id"

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 3, listOf(1)))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(DISH to 6), 3, 0))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 6, 1, 3))
    }
}
