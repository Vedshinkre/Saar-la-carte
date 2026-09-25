package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val SEATS_DIR = "regularseatsjson"
private const val SEATS_UNIT = "g"
private const val SEATS_BARLEY = "barley"
private const val TWO_EVENINGS_HERE = 48

private fun seatsFile(file: String) = "$SEATS_DIR/$file"

/**
 * Two A/B pairs written on Sep 21 as follow-ups to [RegularSeatedWithoutOrderKeepsCountingSystemTest]
 * (reserved seats count until the group's first order). Each pair states two answers to one open
 * question, so exactly one of them can pass. The reference chose answer A both times (runs 4-6).
 *
 * The dish costs 100 g of barley (1 g packages, best before one evening), so every evening starts
 * with an empty pantry, one meal is 100 g, and each answer gives a clearly different amount.
 */
abstract class ReservedSeatCountSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val food = seatsFile("food.json")
}

/**
 * Question one: does a reservation add the seats of the reserved table or the size of the group?
 * A REGULAR group of 2 reserves the 12 seat table (the smallest that fits), next to a 30 seat one.
 *
 * Answer A (the reference's, also forum topic 142): the whole table counts, 42 seats, 5 meals, 500 g.
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
 * Question one, answer B (rejected by the reference, fails by design): only the group's 2 seats
 * count, 32 seats, 4 meals, 400 g.
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
 * A STAFF incident removes the only waiter before evening 1, so the REGULAR group of 2 reserves,
 * arrives, is never seated and leaves. Evening 1 is the same under both answers: 32 seats, 400 g.
 *
 * Answer A (the reference's, also forum topic 328): only an order makes a group known, so its seats
 * still count on evening 2: 400 g again.
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
 * Question two, answer B (rejected by the reference, fails by design): arriving makes the group
 * known, so on evening 2 only the 30 other seats are estimated: 3 meals, 300 g.
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
