package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DISH = "Rice Bowl"

/**
 * Written on Sep 21 after the office hour, where the tutors said the ReservedForMe component test
 * was missing the "FOH No Reserving" line (specification page 30). The manager reserves before the
 * kitchen plans, and a group that gets no table never arrives.
 *
 * One COMMON table of two seats. REGULAR group 1 (ten people) cannot get a table by any of the six
 * reservation steps. Group 2 (two people) fits it exactly.
 */
abstract class NoReservationScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/noreservation/restaurants.json"
    override val scenario = "officehourjson/noreservation/scenario.json"
    override val food = "officehourjson/noreservation/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 3
}

/**
 * The failed reservation is the first line of the preparation phase, before the kitchen procures.
 * Rice comes in one 1000 g package, so the procurement line shows the order of the lines but not
 * how many meals were planned.
 */
class NoReservationIsLoggedSystemTest : NoReservationScenario() {
    override val name = "NoReservationIsLoggedSystemTest"
    override val description = "A REGULAR group that gets no table is logged with FOH No Reserving"

    override suspend fun run() {
        // three "Initialization Info" lines precede the preparation
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 1))
        // then the kitchen buys one 1000 g package and the preparation ends
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 1000, "g", "rice"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }
}

/**
 * The group without a table never arrives (tick 1 starts with group 2's arrival and seating). It
 * rates NEGATIVE right after the escorting status of tick 1 (specification page 22; adjustment #25
 * says the opening tick, which is tick 1 here).
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
 * Written on Sep 21 to check the merging log the tutors asked about (specification page 14,
 * step 4). Tables are merged from the smallest upwards, then the smallest are dropped again while
 * the rest still seats the group. Tables of 2, 3 and 4 seats for a group of six become 3 + 4. The
 * merged table keeps the lowest id, 3.
 */
abstract class MergingScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/merging/restaurants.json"
    override val scenario = "officehourjson/merging/scenario.json"
    override val food = "officehourjson/merging/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5
}

/**
 * Arrival, merge (3 and 4 into 3), seating, order and seating status follow each other directly.
 * The merged table counts once in the status (adjustment #7).
 */
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

/** The serving and finished-eating lines of the merged group name the merged table 3. */
class MergedTableIsUsedForServingSystemTest : MergingScenario() {
    override val name = "MergedTableIsUsedForServingSystemTest"
    override val description = "Serving and eating of a merged group are logged on the merged table id"

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 3, listOf(1)))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(DISH to 6), 3, 0))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 6, 1, 3))
    }
}
