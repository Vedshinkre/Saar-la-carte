package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** A/B probe for the positive/neutral boundary of the 4 tick expectation window */
abstract class ExpectationWindowAbSystemTest : ExampleSystemTestExtension() {
    override val restaurants = "abtests/expectationwindow/restaurants.json"
    override val scenario = "abtests/expectationwindow/scenario.json"
    override val food = "abtests/expectationwindow/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    /** the tick group 2 is escorted out and rates */
    protected suspend fun skipToRatingStepOfTickSeven() {
        skipUntilString(TickStatusTestLogs.tickStart(RATING_TICK, 1))
        skipUntilString(
            FohServiceTestLogs.escorting(restId = 1, waitstaffId = 1, customers = 2, groupId = 2, tableId = 2)
        )
    }

    protected companion object {
        const val RATING_TICK = 7
    }
}

/** reading A: food that arrives exactly 4 ticks after the order is still a positive experience. */
class ServedOnFourthTickIsPositiveSystemTest : ExpectationWindowAbSystemTest() {
    override val name = "ServedOnFourthTickIsPositiveSystemTest"
    override val description = "Reading A: being served 4 ticks after ordering is a positive experience"

    override suspend fun run() {
        skipToRatingStepOfTickSeven()
        assertNextLine(FohServiceTestLogs.escortingStatus(restId = 1, waitstaff = 1, customers = 2))
        assertNextLine(FohServiceTestLogs.rating(restId = 1, groupId = 2, rating = "POSITIVE", pos = 1, neg = 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(restId = 1, groups = 1))
    }
}

/** reading B: only the first 3 ticks are inside the window, 4 ticks is already neutral. */
class ServedOnFourthTickIsNeutralSystemTest : ExpectationWindowAbSystemTest() {
    override val name = "ServedOnFourthTickIsNeutralSystemTest"
    override val description = "Reading B: being served 4 ticks after ordering is only a neutral experience"

    override suspend fun run() {
        skipToRatingStepOfTickSeven()
        assertNextLine(FohServiceTestLogs.escortingStatus(restId = 1, waitstaff = 1, customers = 2))
        // a SOME group stays silent about a neutral experience, so no rating line is logged at all
        assertNextLine(FohServiceTestLogs.ratingStatus(restId = 1, groups = 0))
    }
}
