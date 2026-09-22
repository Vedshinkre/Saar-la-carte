package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val MEAL_A = "mealA"
private const val MEAL_B = "mealB"
private const val MEAL_C = "mealC"

/**
 * A/B probes for the two waiting windows of a partially cooked table. One customer orders mealA
 * (duration 30), one mealB and one mealC (duration 40 each), the kitchen has a single TOURNANT
 * cook, so the meals are finished one after the other and the table is never complete.
 *
 * mealA is finished in tick 3, two ticks after the order of tick 1.
 *
 * The fixture is a copy of the one
 * [de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CorrectPartialServing1] uses. It is
 * kept separate on purpose: that directory is shared with PartialServiceTimeoutTest and has already
 * been rewritten once under a test that depended on it, which is what broke PartialServiceSuccessTest.
 * The four probes below read exact tick numbers, so they must own their scenario.
 */
abstract class PartialServingScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/partialserving/restaurants.json"
    override val scenario = "officehourjson/partialserving/scenario.json"
    override val food = "officehourjson/partialserving/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 10

    /** the setup both readings agree on */
    protected suspend fun assertSharedSetup() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(
            FohArrivalTestLogs.ordering(1, 1, 1, mapOf(MEAL_A to 1, MEAL_B to 1, MEAL_C to 1), 1)
        )
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 1, 1, MEAL_A, 2))
    }
}

/**
 * Reading A of specification page 15: "after the first meal has been cooked, the table is not SERVED
 * for this and the following tick", so the cooking tick 3 and tick 4 are both blocked and the waiter
 * serves mealA in tick 5, four ticks after the order. This is what we implement.
 */
class PartialServingWaitsForTwoTicksSystemTest : PartialServingScenario() {
    override val name = "PartialServingWaitsForTwoTicksSystemTest"
    override val description = "A partly cooked table is served two ticks after the first meal was cooked"

    override suspend fun run() {
        assertSharedSetup()
        // the kitchen status of the tick sits between the two lines
        skipUntilString(FohServiceTestLogs.noServing(1, 1, 1, 1))
        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(FohServiceTestLogs.noServing(1, 1, 1, 1))
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(MEAL_A to 1), 1, 4))
    }
}

/**
 * Reading B: only the cooking tick itself is blocked, so the waiter already serves mealA in tick 4,
 * three ticks after the order.
 */
class PartialServingWaitsForOneTickSystemTest : PartialServingScenario() {
    override val name = "PartialServingWaitsForOneTickSystemTest"
    override val description = "A partly cooked table is served one tick after the first meal was cooked"

    override suspend fun run() {
        assertSharedSetup()
        skipUntilString(FohServiceTestLogs.noServing(1, 1, 1, 1))
        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(MEAL_A to 1), 1, 3))
    }
}

/**
 * The two extra ticks a table gets once one of its customers has been served (specification page
 * 21): "If at least one person on the table has received their meal, they wait for 2 more ticks."
 *
 * The first pair of probes for this asserted a follow-up status line to pin the tick down and was
 * mis-specified: both readings failed against the reference and against our own implementation, so
 * the run said nothing. These five say only which tick the walk-out happens in, one candidate each,
 * so exactly one of them can pass and the next run names the tick outright.
 *
 * The scenario is identical up to tick 5 on both implementations: mealA is cooked in tick 3 and,
 * after the two blocked ticks, served in tick 5 (confirmed by [PartialServingWaitsForTwoTicksSystemTest]).
 * From there the last customer, whose mealC is still not cooked, waits out the extension.
 * Our implementation leaves in tick 7, six ticks after the order of tick 1.
 */
abstract class ExtendedPatienceScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/partialserving/restaurants.json"
    override val scenario = "officehourjson/partialserving/scenario.json"
    override val food = "officehourjson/partialserving/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    /**
     * Pins the walk-out to exactly [tick]: the skip starts at that tick, so it cannot have happened
     * earlier, and the start of the following tick must still be ahead of it, so it cannot have
     * happened later either.
     */
    protected suspend fun assertLastCustomerLeavesInTick(tick: Int) {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(
            FohArrivalTestLogs.ordering(1, 1, 1, mapOf(MEAL_A to 1, MEAL_B to 1, MEAL_C to 1), 1)
        )
        skipUntilString(TickStatusTestLogs.tickStart(tick, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 1, 1, 1))
        skipUntilString(TickStatusTestLogs.tickStart(tick + 1, 1))
    }
}

/** the extension is ignored and the base window of four ticks decides */
class ExtendedPatienceLeavesInTickFiveSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickFiveSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 5"

    override suspend fun run() = assertLastCustomerLeavesInTick(5)
}

/** five ticks after ordering */
class ExtendedPatienceLeavesInTickSixSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickSixSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 6"

    override suspend fun run() = assertLastCustomerLeavesInTick(6)
}

/** six ticks after ordering, the base window of four plus the two extra ticks: what we implement */
class ExtendedPatienceLeavesInTickSevenSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickSevenSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 7"

    override suspend fun run() = assertLastCustomerLeavesInTick(7)
}

/** seven ticks after ordering, the two extra ticks counted on the old five tick base window */
class ExtendedPatienceLeavesInTickEightSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickEightSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 8"

    override suspend fun run() = assertLastCustomerLeavesInTick(8)
}

/** the two extra ticks counted from the tick mealA was served rather than from the order */
class ExtendedPatienceLeavesInTickNineSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickNineSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 9"

    override suspend fun run() = assertLastCustomerLeavesInTick(9)
}
