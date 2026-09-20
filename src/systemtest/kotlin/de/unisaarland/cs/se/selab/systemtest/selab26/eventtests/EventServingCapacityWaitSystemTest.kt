package de.unisaarland.cs.se.selab.systemtest.selab26.eventtests

import de.unisaarland.cs.se.selab.systemtest.api.SystemTestAssertionError
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUP = "Tomato Soup"
private const val STEW = "Slow Stew"

/**
 * P04 (spec 2.2 "Front of house"): SERVING is done for REGULAR, then EVENT and then CASUAL groups.
 * The manager serves a complete EVENT table only if the SERVING capacity of the whole waitstaff
 * suffices for it. Otherwise the manager waits one tick and then serves the meals one by one.
 *
 * Restaurant 1 has two waiters:
 * - tick 1: REGULAR 2 (10 customers, orders the Slow Stew as the dish with the highest recipe id)
 *   is seated by waiter 1
 * - tick 3: EVENT 1 (15 customers) is seated by waiter 1 (current load 10) with 10 customers and
 *   by waiter 2 with 5. The stew of the REGULAR group and the soup of the event are cooked in the
 *   same tick 3
 * - REGULAR groups are served first: waiter 1 serves all 10 stews and uses up his SERVING capacity.
 *   Only waiter 2 has capacity left (10), the event table needs 15, so it is not served in tick 3
 * - in tick 4 the meals are served one by one: as much as possible, 10 by waiter 1 and 5 by waiter 2
 *
 * The scenario is checked in independent chunks, so that a failure points at a single rule.
 */
abstract class EventServingCapacityWaitScenario : ExampleSystemTestExtension() {
    override val restaurants = "eventtests/servingwait/restaurants.json"
    override val scenario = "eventtests/servingwait/scenario.json"
    override val food = "eventtests/servingwait/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 80
}

/** The REGULAR group (tick 1) and the EVENT group (tick 3) are seated as described above. */
class EventServingWaitSeatingSystemTest : EventServingCapacityWaitScenario() {
    override val name = "EventServingWaitSeatingSystemTest"
    override val description = "Setup of the EVENT serving capacity scenario: who seats REGULAR and EVENT"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        skipUntilString(FohArrivalTestLogs.seating(1, 2, 1, listOf(1)))

        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 2, listOf(1, 2)))
    }
}

/** REGULAR is served before EVENT and uses up the SERVING capacity of waiter 1. */
class EventServingWaitRegularServedFirstSystemTest : EventServingCapacityWaitScenario() {
    override val name = "EventServingWaitRegularServedFirstSystemTest"
    override val description = "The REGULAR table is served before the EVENT table and uses up waiter 1"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(STEW to 10), 1, 2))
    }
}

/** The EVENT table needs 15 but only 10 capacity is left, so the manager waits. */
class EventServingWaitEventNotServedSystemTest : EventServingCapacityWaitScenario() {
    override val name = "EventServingWaitEventNotServedSystemTest"
    override val description = "An EVENT table waits a tick if the waitstaff's SERVING capacity is insufficient"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(STEW to 10), 1, 2))
        assertEventNotServed(15, 2)
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 10))
    }

    /**
     * The spec logs "the last waiter that did serve or the first that would have served" for the
     * tables of EVENT groups, so which of the two waiters is named is not pinned down here
     */
    private suspend fun assertEventNotServed(meals: Int, tableId: Int) {
        val prefix = "[DEBUG] FOH No Serving (R 1): Waitstaff "
        val suffix = " did not serve $meals meals to table $tableId."
        val line = getNextLine() ?: throw SystemTestAssertionError("End of log reached when there should be more.")
        if (!line.startsWith(prefix) || !line.endsWith(suffix)) {
            throw SystemTestAssertionError("Expected '$prefix<id>$suffix' but got '$line'")
        }
    }
}

/** One tick later the EVENT meals are served one by one: 10 by waiter 1 and 5 by waiter 2. */
class EventServingWaitOneByOneSystemTest : EventServingCapacityWaitScenario() {
    override val name = "EventServingWaitOneByOneSystemTest"
    override val description = "After waiting a tick the EVENT meals are served one by one by several waiters"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(4, 4))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(SOUP to 10), 2, 1))
        assertNextLine(FohServiceTestLogs.serving(1, 2, mapOf(SOUP to 5), 2, 1))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 2, 15))
    }
}
