package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val SEATS_DIR = "regularseatsjson"
private const val SEATS_UNIT = "g"
private const val SEATS_BARLEY = "barley"
private const val TWO_EVENINGS_HERE = 48

private fun seatsFile(file: String) = "$SEATS_DIR/$file"

/**
 * The reference counts a REGULAR group's reserved seats towards the guess until that group has
 * placed its first order, which [RegularSeatedWithoutOrderKeepsCountingSystemTest] established.
 * Two follow-up questions are left open, and each is asked here as a pair of tests that state the
 * competing answers, so one run settles both.
 *
 * The dish costs 100 g of barley, sold in 1 g packages with a best before of one evening, so every
 * evening starts from an empty pantry and the procured amount is exactly the amount planned. One
 * meal is therefore 100 g and every reading is a clearly different number.
 */
abstract class ReservedSeatCountSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val food = seatsFile("food.json")
}

/**
 * Question one: how many seats does a reservation contribute, the seats of the reserved table or
 * the size of the group sitting on it?
 *
 * The restaurant has a 12 seat and a 30 seat table, and a REGULAR group of 2 reserves. No table is
 * a three quarter fit for a group of 2, so the lifted rule puts the group on the smallest table
 * that fits, the 12 seat one, and the 30 seat table is left over.
 *
 * Answer A, the one we implement: the reserved table contributes all 12 of its seats, so 42 seats
 * give 5 meals and 500 g.
 */
class ReservedSeatsAreTheWholeTableSystemTest : ReservedSeatCountSystemTest() {
    override val name = "ReservedSeatsAreTheWholeTableSystemTest"
    override val description = "Answer A: a reservation contributes the seats of the whole reserved table"
    override val restaurants = seatsFile("restaurantsOversizedTable.json")
    override val scenario = seatsFile("scenarioPlainRegular.json")
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, WHOLE_TABLE, SEATS_UNIT, SEATS_BARLEY))
    }

    private companion object {
        /** 30 free seats plus the 12 of the reserved table, so 5 meals */
        const val WHOLE_TABLE = 500
    }
}

/**
 * Question one, answer B: only the 2 customers of the group count, not the 10 empty seats they
 * leave on the 12 seat table, so 32 seats give 4 meals and 400 g.
 */
class ReservedSeatsAreTheGroupSizeSystemTest : ReservedSeatCountSystemTest() {
    override val name = "ReservedSeatsAreTheGroupSizeSystemTest"
    override val description = "Answer B: a reservation contributes only the size of the group"
    override val restaurants = seatsFile("restaurantsOversizedTable.json")
    override val scenario = seatsFile("scenarioPlainRegular.json")
    override val maxTicks = 1

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, GROUP_SIZE_ONLY, SEATS_UNIT, SEATS_BARLEY))
    }

    private companion object {
        /** 30 free seats plus the group's own 2, so 4 meals */
        const val GROUP_SIZE_ONLY = 400
    }
}

/**
 * Question two: does a group that arrives but is never seated count as having visited?
 *
 * A STAFF incident takes the only waiter away before evening 1, so the REGULAR group of 2 reserves
 * the 2 seat table, arrives, finds nobody to seat it on both of its attempts and leaves, twice over.
 * Its order history stays empty and it is never seated, so every reading agrees on evening 1:
 * 32 seats, 4 meals, 400 g.
 *
 * Answer A, the one we implement: only an order makes a group known, so on evening 2 its seats
 * still count and the plan is the same 400 g.
 */
class ArrivedWithoutSeatingStillCountsSystemTest : ReservedSeatCountSystemTest() {
    override val name = "ArrivedWithoutSeatingStillCountsSystemTest"
    override val description = "Answer A: arriving without being seated does not make a REGULAR known"
    override val restaurants = seatsFile("restaurantsExactFit.json")
    override val scenario = seatsFile("scenarioNoWaitstaff.json")
    override val maxTicks = TWO_EVENINGS_HERE

    override suspend fun run() {
        assertSharedFirstEvening()
        assertSecondEveningPlans(STILL_COUNTS)
    }

    private companion object {
        /** the same 4 meals as on evening 1 */
        const val STILL_COUNTS = 400
    }
}

/**
 * Question two, answer B: showing up at all is enough to make the group known, so from evening 2
 * its empty history is planned for instead of its seats and only the 30 seats of the other table
 * are guessed for: 3 meals, 300 g.
 */
class ArrivedWithoutSeatingCountsAsVisitSystemTest : ReservedSeatCountSystemTest() {
    override val name = "ArrivedWithoutSeatingCountsAsVisitSystemTest"
    override val description = "Answer B: arriving is enough to make a REGULAR known"
    override val restaurants = seatsFile("restaurantsExactFit.json")
    override val scenario = seatsFile("scenarioNoWaitstaff.json")
    override val maxTicks = TWO_EVENINGS_HERE

    override suspend fun run() {
        assertSharedFirstEvening()
        assertSecondEveningPlans(COUNTS_AS_VISIT)
    }

    private companion object {
        /** only the 30 seats of the other table, so 3 meals */
        const val COUNTS_AS_VISIT = 300
    }
}

/** evening 1 plans 4 meals for all 32 seats under either reading */
private suspend fun ReservedSeatCountSystemTest.assertSharedFirstEvening() {
    skipUntilString(InitialAndPrepTestLogs.prepStart(1))
    assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING_PLAN, SEATS_UNIT, SEATS_BARLEY))
}

/** the evening 1 stock has expired by evening 2, so the pantry starts empty either way */
private suspend fun ReservedSeatCountSystemTest.assertSecondEveningPlans(amount: Int) {
    skipUntilString(InitialAndPrepTestLogs.prepStart(2))
    assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, FIRST_EVENING_PLAN, SEATS_UNIT, SEATS_BARLEY))
    assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, amount, SEATS_UNIT, SEATS_BARLEY))
}

/** 4 meals of 100 g for all 32 seats */
private const val FIRST_EVENING_PLAN = 400
