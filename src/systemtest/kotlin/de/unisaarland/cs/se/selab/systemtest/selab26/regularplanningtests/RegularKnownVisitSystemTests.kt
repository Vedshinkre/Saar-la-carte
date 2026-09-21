package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val VISIT_DIR = "regularvisitjson"
private const val VISIT_UNIT = "g"
private const val BARLEY = "barley"
private const val THREE_EVENINGS = 72

private fun visitFile(file: String) = "$VISIT_DIR/$file"

/**
 * The specification calls the regulars that are planned for by their order history the "known"
 * ones, "so those REGULAR groups that already have visited the restaurant at least once"
 * (page 11, lines 32-33), and leaves open what counts as having visited. The two readings only
 * differ for a group that was seated but never managed to order, so that is what this scenario
 * builds.
 *
 * The restaurant has a 2 seat and a 30 seat table and one dish of 100 g of barley, sold in 1 g
 * packages with a best before of one evening, so every evening starts from an empty pantry and the
 * procured amount is exactly the amount planned. The REGULAR group of 2 excludes barley, so it is
 * seated every evening and can never order, and it never builds an order history.
 *
 * Evening 1 is the same under both readings: the group has neither visited nor ordered, so its 2
 * reserved seats count towards the guess, 32 seats give 4 meals and 400 g are bought. Evening 2 is
 * where the readings split. Exactly one of the two tests below should pass.
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
 * Reading A, the one we implement: "visited" means seated. After the group has been seated once it
 * counts as a known regular, so from evening 2 on it is planned for by its order history alone.
 * That history is empty, so nothing at all is planned for its table and only the 30 seats of the
 * other table are guessed for: 3 meals, 300 g.
 */
class RegularSeatedWithoutOrderStopsCountingSystemTest : RegularKnownVisitSystemTest() {
    override val name = "RegularSeatedWithoutOrderStopsCountingSystemTest"
    override val description = "Reading A: being seated once makes a REGULAR known, even without an order"

    override suspend fun run() {
        assertSharedFirstEvening()
        assertSecondEveningPlans(HISTORY_ONLY)
    }

    private companion object {
        /** 3 meals of 100 g for the 30 seats of the other table, nothing for the empty history */
        const val HISTORY_ONLY = 300
    }
}

/**
 * Reading B: "visited" means having placed an order. A group that was seated but never ordered is
 * still unknown, so its 2 reserved seats keep counting towards the guess until it manages to order
 * for the first time: 32 seats, 4 meals, 400 g again on evening 2.
 */
class RegularSeatedWithoutOrderKeepsCountingSystemTest : RegularKnownVisitSystemTest() {
    override val name = "RegularSeatedWithoutOrderKeepsCountingSystemTest"
    override val description = "Reading B: a REGULAR stays unknown until it places its first order"

    override suspend fun run() {
        assertSharedFirstEvening()
        assertSecondEveningPlans(SEATS_STILL_COUNT)
    }

    private companion object {
        /** 4 meals of 100 g for all 32 seats, as on evening 1 */
        const val SEATS_STILL_COUNT = 400
    }
}
