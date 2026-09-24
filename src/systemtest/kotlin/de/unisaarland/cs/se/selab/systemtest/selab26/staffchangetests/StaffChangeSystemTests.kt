package de.unisaarland.cs.se.selab.systemtest.selab26.staffchangetests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "staffprobejson"
private const val STAFF = "STAFF"
private const val UNIT = "g"
private const val LENTIL = "lentil"
private const val STEW = "Lentil Stew"
private const val GROUP_SIZE = 2
private const val TWO_EVENINGS = 48
private const val THREE_EVENINGS = 72

private fun probe(file: String) = "$DIR/$file"
private fun stew(amount: Int) = mapOf(STEW to amount)

/**
 * Tests for the STAFF change incident (specification page 26, lines 1-11; forum topic 287:
 * incidents may reduce waitstaff, drivers or non-EXEC cooks to 0). Written on Sep 21, next to the
 * RECIPE incident tests. The procurement values were corrected in run 4, like the planning tests.
 *
 * One restaurant with a 2 and a 30 seat table, one waiter and one TOURNANT cook. A REGULAR group of
 * 2 visits every evening, reserves the 2 seat table and orders 2 meals of the one dish (10 g of
 * lentil, 1 g packages). Evening 1 buys 40 g (4 meals: the group's seats still count on its first
 * visit). Later evenings plan 3 meals for the 30 other seats plus 2 * 10 g for the earlier visit.
 */
abstract class StaffChangeSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val food = probe("food.json")
    override val restaurants = probe("restaurants.json")
}

/**
 * Removing the only cook before evening 2 makes every dish unorderable: a dish needs an eligible
 * cook in the restaurant (specification page 13, lines 14-15). The kitchen still plans the whole
 * menu, "including recipes for which they currently have no eligible cook" (page 12, line 3).
 * So evening 2 still buys 30 g (30 + 20 needed, 20 left over), and only the ordering fails.
 */
class StaffChangeNoCookStillProcuresSystemTest : StaffChangeSystemTest() {
    override val name = "StaffChangeNoCookStillProcuresSystemTest"
    override val description = "A restaurant without cooks still procures but can no longer take orders"
    override val scenario = probe("oneRegularScenarioRemoveCook.json")
    override val maxTicks = THREE_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING, UNIT, LENTIL))
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, 1, stew(GROUP_SIZE), 1))

        skipUntilString(InitialAndPrepTestLogs.incident(1, STAFF, 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SECOND_EVENING, UNIT, LENTIL))
        skipUntilString(FohArrivalTestLogs.noOrdering(1, 1, GROUP_SIZE))
    }

    private companion object {
        /** 4 meals: the 30 free seats plus the group's 2 reserved seats, which have no history yet */
        const val FIRST_EVENING = 40

        /** 30 estimated plus 20 of history, minus the 20 left over from evening 1 */
        const val SECOND_EVENING = 30
    }
}

/**
 * Two incidents of one evening are applied in ascending id order, and a reduction below zero leaves
 * zero rather than a negative count (specification page 26, lines 10-11).
 *
 * One TOURNANT cook. Incident 1 removes 2 (clamped at 0), incident 2 hires 1, so evening 2 has one
 * cook and the group places order 2. Both wrong implementations leave 0 cooks, so no order: the
 * reverse order (1 + 1 - 2) and a missing clamp (1 - 2 + 1). The only thing checked is that
 * order 2 exists.
 */
class StaffChangeClampThenHireSystemTest : StaffChangeSystemTest() {
    override val name = "StaffChangeClampThenHireSystemTest"
    override val description = "A reduction below zero clamps at zero before the next incident hires again"
    override val scenario = probe("oneRegularScenarioClampThenHire.json")
    override val maxTicks = TWO_EVENINGS

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, 1, stew(GROUP_SIZE), 1))

        skipUntilString(InitialAndPrepTestLogs.incident(1, STAFF, 2))
        assertNextLine(InitialAndPrepTestLogs.incident(2, STAFF, 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, SECOND_ORDER, stew(GROUP_SIZE), 1))
    }

    private companion object {
        const val SECOND_ORDER = 2
    }
}

/**
 * Removing the only waiter before evening 2 leaves nobody to seat the group. It gets "no free
 * waitstaff" in tick 1 (right after its arrival), retries in tick 2, gets it again and then leaves
 * (specification page 15, lines 29-33).
 *
 * Reservations are made by the manager, not a waiter, so the reservation and the evening 2
 * procurement (30 g, the same as with a waiter) are unaffected.
 */
class StaffChangeNoWaitstaffSystemTest : StaffChangeSystemTest() {
    override val name = "StaffChangeNoWaitstaffSystemTest"
    override val description = "Without waitstaff a group retries once and then leaves"
    override val scenario = probe("oneRegularScenarioNoWaitstaff.json")
    override val maxTicks = TWO_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, STAFF, 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SECOND_EVENING, UNIT, LENTIL))

        skipUntilString(TickStatusTestLogs.tickStart(1, 2))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 1))

        // second and last attempt, on the tick after the visiting tick, so without an arrival log
        skipUntilString(TickStatusTestLogs.tickStart(2, 2))
        skipUntilString(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 1))
    }

    private companion object {
        /** the reservation still happens, so the plan is 30 estimated plus 20 of history, minus 20 */
        const val SECOND_EVENING = 30
    }
}

/**
 * A STAFF incident affects only the restaurant it names. Two identical restaurants each have a
 * REGULAR group. Before evening 2, restaurant 2 loses its only cook: restaurant 1 still takes
 * order 3, and restaurant 2's group cannot order.
 */
class StaffChangeOnlyNamedRestaurantSystemTest : StaffChangeSystemTest() {
    override val name = "StaffChangeOnlyNamedRestaurantSystemTest"
    override val description = "A STAFF incident only affects the restaurant it names"
    override val restaurants = probe("restaurantsTwo.json")
    override val scenario = probe("twoRegularScenario.json")
    override val maxTicks = TWO_EVENINGS

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, 1, stew(GROUP_SIZE), 1))
        skipUntilString(FohArrivalTestLogs.ordering(2, 2, SECOND_ORDER, stew(GROUP_SIZE), 1))

        skipUntilString(InitialAndPrepTestLogs.incident(1, STAFF, 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))

        skipUntilString(FohArrivalTestLogs.ordering(1, 1, THIRD_ORDER, stew(GROUP_SIZE), 1))
        skipUntilString(FohArrivalTestLogs.noOrdering(2, 2, GROUP_SIZE))
    }

    private companion object {
        const val SECOND_ORDER = 2
        const val THIRD_ORDER = 3
    }
}
