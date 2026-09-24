package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val DIR = "recipeprobejson"
private const val UNIT = "g"
private const val TWO_EVENINGS = 48
private const val THREE_EVENINGS = 72

private fun probe(file: String) = "$DIR/$file"

/**
 * Tests for the kitchen's ingredient planning for known REGULAR groups (specification page 11,
 * lines 31-35). Only the regulars visiting tonight are planned for, with the orders of their last
 * three visits, on top of ceil(otherSeats / 10) meals of every dish on the menu (page 12).
 *
 * Written on Sep 21 to narrow down the failing planning component tests. Corrected in run 4, once
 * the reference showed that a REGULAR group's reserved seats count towards the estimate until the
 * group has ordered once. Every ingredient comes in 1 g packages, so the amount bought is the
 * amount planned minus what is left in the pantry.
 */
abstract class RegularPlanningSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
}

/**
 * The order histories of all regulars visiting tonight are added up.
 *
 * Two REGULAR groups of 2 visit every evening, reserve the two 2 seat tables (next to two 30 seat
 * ones) and order 2 meals of a 5 g dish each:
 *  - evening 1: first visits, so all 64 seats count: 7 meals, 35 g, of which 20 g are eaten;
 *  - evening 2: 6 meals for the 60 other seats (30) plus both groups' visit (20), minus the 15 left
 *    over: 35 g bought.
 *
 * Planning only one group's history would buy 25 g.
 */
class RegularPlanningTwoGroupsSystemTest : RegularPlanningSystemTest() {
    override val name = "RegularPlanningTwoGroupsSystemTest"
    override val description = "The planning sums the order history of every regular visiting tonight"
    override val food = probe("multiRegularFood.json")
    override val restaurants = probe("multiRegularRestaurants.json")
    override val scenario = probe("multiRegularScenario.json")
    override val maxTicks = TWO_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING, UNIT, SPELT))

        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SECOND_EVENING, UNIT, SPELT))
    }

    private companion object {
        const val SPELT = "spelt"

        /** 7 meals of 5 g: the 60 free seats plus the two groups' 4 reserved seats */
        const val FIRST_EVENING = 35

        /** 30 estimated plus 20 for the two groups' visit, minus the 15 left over */
        const val SECOND_EVENING = 35
    }
}

/**
 * A regular that does not visit tonight is not planned for, and its table is not reserved.
 *
 * A REGULAR group of 2 with visitingPeriod 2 comes on evenings 1 and 3. Millet is 10 g:
 *  - evening 1: first visit, 32 seats, 4 meals, 40 g bought, 20 g eaten;
 *  - evening 2: no visit and no reservation, 32 seats, 40 g needed, 20 g bought;
 *  - evening 3: 3 meals for the 30 other seats plus one visit (20), 50 g needed, 10 g bought.
 *
 * Planning the absent group's history on evening 2 would buy 40 g there.
 */
class RegularPlanningVisitingPeriodSystemTest : RegularPlanningSystemTest() {
    override val name = "RegularPlanningVisitingPeriodSystemTest"
    override val description = "A regular that does not visit tonight is not planned for"
    override val food = probe("periodFood.json")
    override val restaurants = probe("historyRestaurants.json")
    override val scenario = probe("periodScenario.json")
    override val maxTicks = THREE_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING, UNIT, MILLET))

        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SECOND_EVENING, UNIT, MILLET))

        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, THIRD_EVENING, UNIT, MILLET))
    }

    private companion object {
        const val MILLET = "millet"

        /** 4 meals: the 30 free seats plus the group's 2 reserved seats, which have no history yet */
        const val FIRST_EVENING = 40

        /** nobody reserves, so 4 meals are estimated for all 32 seats, minus the 20 left over */
        const val SECOND_EVENING = 20

        /** 30 estimated plus 20 for the one visit in the history, minus the 40 left over */
        const val THIRD_EVENING = 10
    }
}

/**
 * A visit without an order adds nothing to the order history.
 *
 * A REGULAR group of 2 excludes barley, the only ingredient of the only dish, so it is seated but
 * cannot order. With an empty history its reserved seats keep counting: 32 seats, 4 meals, 40 g
 * on every evening. Evening 1 buys the 40 g and nothing is eaten. Evening 2 needs the same 40 g, so
 * nothing is bought and the restocked line follows the preparation line directly. A phantom
 * history entry of 2 meals would plan 30 + 20 g and buy 10 g.
 *
 * Evening 3 also buys nothing. That holds whether or not the group comes back after its second
 * evening without an order, so it only checks that the estimate stays the same.
 */
class RegularPlanningFailedOrderSystemTest : RegularPlanningSystemTest() {
    override val name = "RegularPlanningFailedOrderSystemTest"
    override val description = "A visit without an order adds nothing to the planning of the next evening"
    override val food = probe("failedOrderFood.json")
    override val restaurants = probe("historyRestaurants.json")
    override val scenario = probe("failedOrderScenario.json")
    override val maxTicks = THREE_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING, UNIT, BARLEY))
        skipUntilString(noOrdering(1))

        // nothing is procured on evening 2: the failed visit left no order in the history
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        skipUntilString(noOrdering(1))

        // evening 3: the same estimate, so still nothing to buy
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }

    private companion object {
        const val BARLEY = "barley"

        /** 4 meals for all 32 seats: the group's reserved seats have no history behind them yet */
        const val FIRST_EVENING = 40

        fun noOrdering(groupId: Int) =
            "[IMPORTANT] FOH No Ordering (R 1): Group $groupId could not place an order for 2 customers,"
    }
}
