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
 * Probes for the STAFF change incident (specification page 26, lines 1-11, and forum topic 287:
 * incidents may reduce waitstaff, drivers or non-EXEC cooks to 0).
 *
 * A regular group of 2 visits every evening, reserves the 2 seat table and orders 2 meals of the
 * one dish. Lentil is sold in 1 g packages and the remaining 30 seats give an estimate of 3 meals,
 * so evening 1 always procures 30 g and every later evening plans 30 estimated plus 2 * 10 for the
 * visit in the history.
 */
abstract class StaffChangeSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val food = probe("food.json")
    override val restaurants = probe("restaurants.json")
}

/**
 * Losing the last cook of the only cook type makes every dish unorderable, because a dish can only
 * be ordered if an eligible cook exists in the restaurant (specification page 13, lines 14-15).
 *
 * The kitchen still buys the ingredients for it, though: the planning covers the whole menu
 * "including recipes for which they currently have no eligible cook" (page 12, line 3). Evening 2
 * therefore still procures the 40 g that the estimate and the history call for, and only the
 * ordering fails.
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
 * The restaurant has one TOURNANT cook. Incident 1 removes 2 of them, which clamps at 0, and
 * incident 2 hires one, so the evening runs with exactly one cook and the group can order. In the
 * other order the restaurant would end up with 2 - 2 = 0 cooks and the order would fail, and
 * without the clamp 1 - 2 + 1 would be 0 as well.
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
 * Losing the last waiter leaves the group without anyone to seat it. The group tries again on the
 * next tick and then leaves (specification page 15, lines 29-33), so the "no free waitstaff" line
 * appears twice and only the first tick carries an arrival log.
 *
 * Reserving tables is the manager's job and not a waiter's, so the reservation and the procurement
 * of the evening are unaffected.
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
 * A STAFF incident names one restaurant and must leave every other restaurant alone. Two identical
 * EUROPEAN restaurants each lose nothing until evening 2, when restaurant 2 loses its only cook.
 * Restaurant 1 keeps taking orders, restaurant 2 can no longer take any.
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
