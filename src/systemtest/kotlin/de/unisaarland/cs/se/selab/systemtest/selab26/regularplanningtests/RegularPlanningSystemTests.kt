package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val DIR = "recipeprobejson"
private const val UNIT = "g"
private const val TWO_EVENINGS = 48
private const val THREE_EVENINGS = 72

private fun probe(file: String) = "$DIR/$file"

/**
 * Probes for the kitchen's ingredient planning for known REGULAR groups (specification page 11,
 * lines 31-35): only the regulars that are planning to visit tonight are planned for, with the
 * orders of their last three visits, on top of the estimate of ceil(otherSeats / 10) meals of
 * every dish on the menu (page 12).
 *
 * Every ingredient is sold in 1 g packages, so the amount procured equals the amount planned.
 */
abstract class RegularPlanningSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
}

/**
 * Two REGULAR groups of 2 visit the same restaurant every evening and each orders 2 meals of the
 * only dish (5 g each). They reserve the two 2 seat tables, leaving 60 other seats and an estimate
 * of 6 meals.
 *
 * Evening 1 has no history: 6 * 5 = 30 procured, and the 4 meals ordered reserve 20, leaving 10.
 * Evening 2 plans the estimate of 30 plus *both* groups' single visit, 4 * 5 = 20, so 50 are
 * required and 40 are bought on top of the 10 left over.
 *
 * Planning only one of the two groups would buy 30.
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

        /** the estimate alone: 6 meals of 5 g */
        const val FIRST_EVENING = 30

        /** 30 estimated plus 20 for the two groups' visit, minus the 10 left over */
        const val SECOND_EVENING = 40
    }
}

/**
 * A REGULAR group of 2 with a visitingPeriod of 2 visits on evenings 1, 3, 5. On the evenings in
 * between it is not planning to visit, so its order history must not be planned for and its table
 * is not reserved either, which raises the estimate instead.
 *
 * Millet is 10 g. Evening 1: the group reserves the 2 seat table, 30 other seats give an estimate
 * of 3 meals, 30 procured, 20 reserved by the order, 10 left. Evening 2: no reservation, so all 32
 * seats are other seats and the estimate is 4 meals, 40 required and 30 bought. Evening 3: the
 * group is back, the estimate is 3 meals again and its one visit adds 2 * 10, so 50 are required
 * and 10 are bought on top of the 40 in the pantry.
 *
 * Planning the absent group's history on evening 2 would buy 50 there.
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

        /** 3 meals estimated for the 30 unreserved seats */
        const val FIRST_EVENING = 30

        /** nobody reserves, so 4 meals are estimated for all 32 seats, minus the 10 left over */
        const val SECOND_EVENING = 30

        /** 30 estimated plus 20 for the one visit in the history, minus the 40 left over */
        const val THIRD_EVENING = 10
    }
}

/**
 * A REGULAR group of 2 excludes the only ingredient of the only dish, so it is seated but nobody
 * can order. A visit without an order leaves nothing in the order history, so the next evening
 * plans the estimate alone.
 *
 * Barley is 10 g and the estimate is 3 meals. Evening 1 buys 30 and none of it is reserved, because
 * the order fails. Evening 2 therefore still only requires those 30, which are already in the
 * pantry, so nothing at all is procured and the Restocked line follows the preparation line
 * directly. A phantom history entry of 2 meals would buy 20 there.
 *
 * The group also fails on evening 2, which is its second failed attempt, so it stops visiting
 * (specification page 20 and forum topic 299). Evening 3 has no reservation, so all 32 seats are
 * estimated for: 4 meals, 40 required, 10 bought on top of the 30 in the pantry.
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

        // after the second failed attempt the group stops visiting, so nothing is reserved
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, THIRD_EVENING, UNIT, BARLEY))
    }

    private companion object {
        const val BARLEY = "barley"

        /** 3 meals estimated for the 30 unreserved seats */
        const val FIRST_EVENING = 30

        /** 4 meals for all 32 seats once the group stopped visiting, minus the 30 left over */
        const val THIRD_EVENING = 10

        fun noOrdering(groupId: Int) =
            "[IMPORTANT] FOH No Ordering (R 1): Group $groupId could not place an order for 2 customers,"
    }
}
