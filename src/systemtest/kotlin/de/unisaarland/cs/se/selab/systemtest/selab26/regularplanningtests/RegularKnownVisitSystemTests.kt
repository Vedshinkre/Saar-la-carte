package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val VISIT_DIR = "regularvisitjson"
private const val VISIT_UNIT = "g"
private const val BARLEY = "barley"
private const val THREE_EVENINGS = 72

private fun visitFile(file: String) = "$VISIT_DIR/$file"

/**
 * The kitchen plans "known" REGULAR groups, "those REGULAR groups that already have visited the
 * restaurant at least once" (page 11, lines 32-33), by their order history. The specification does
 * not say whether being seated without ordering counts as a visit. Written on Sep 21 as an A/B pair
 * to find out. The reference chose "only an order counts" (runs 3-8, later confirmed in forum topic
 * 328), and the losing test was removed.
 *
 * One dish of 100 g of barley, 1 g packages, best before one evening, so every evening starts with
 * an empty pantry. The REGULAR group of 2 excludes barley: it is seated every evening but never
 * orders. Evening 1: its reserved seats count, 32 seats, 4 meals, 400 g.
 */
abstract class RegularKnownVisitSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val food = visitFile("food.json")
    override val restaurants = visitFile("restaurants.json")
    override val scenario = visitFile("scenarioSeatedNoOrder.json")
    override val maxTicks = THREE_EVENINGS

    /** evening 1 plans 4 meals for all 32 seats under either reading */
    protected suspend fun assertSharedFirstEvening() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING, VISIT_UNIT, BARLEY))
    }

    /** the evening 1 stock has expired by evening 2, so the pantry starts empty either way */
    protected suspend fun assertSecondEveningPlans(amount: Int) {
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, FIRST_EVENING, VISIT_UNIT, BARLEY))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, amount, VISIT_UNIT, BARLEY))
    }

    protected companion object {
        /** 4 meals of 100 g for all 32 seats */
        const val FIRST_EVENING = 400
    }
}

/**
 * A group that was seated but never ordered is still unknown on evening 2, so its reserved seats
 * still count: 400 g again. The losing reading (seated counts as a visit) would plan only the 30
 * other seats and buy 300 g.
 */
class RegularSeatedWithoutOrderKeepsCountingSystemTest : RegularKnownVisitSystemTest() {
    override val name = "RegularSeatedWithoutOrderKeepsCountingSystemTest"
    override val description = "A REGULAR stays unknown until it places its first order"

    override suspend fun run() {
        assertSharedFirstEvening()
        assertSecondEveningPlans(SEATS_STILL_COUNT)
    }

    private companion object {
        /** 4 meals of 100 g for all 32 seats, as on evening 1 */
        const val SEATS_STILL_COUNT = 400
    }
}
