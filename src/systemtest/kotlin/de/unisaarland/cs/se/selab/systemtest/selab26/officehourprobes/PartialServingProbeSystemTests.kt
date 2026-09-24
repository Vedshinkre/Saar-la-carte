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
 * A/B probes (Sep 21, after the office hour) for the two waiting windows of a partly cooked table.
 * Three customers order mealA (30 min), mealB and mealC (40 min each) from a single TOURNANT cook,
 * so the meals finish one after another. mealA is finished in tick 3.
 *
 * The fixture is a private copy of the one CorrectPartialServing1 uses. That directory is shared
 * with other tests and was rewritten once already, which broke PartialServiceSuccessTest. These
 * probes assert exact ticks, so they need their own copy.
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
 * Reading A (the reference's, runs 4-12) of specification page 15: "after the first meal has been
 * cooked, the table is not SERVED for this and the following tick". Ticks 3 and 4 log "FOH No
 * Serving", and mealA is served in tick 5. The "4 ticks after ordering" in that line fixes the tick.
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
 * Reading B (rejected by the reference, fails by design): only the cooking tick is blocked, so
 * mealA would be served in tick 4, three ticks after the order.
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
 * The question is in which tick the customer still waiting for mealC walks out.
 *
 * A first pair of probes (Sep 21) also asserted a status line after the walk-out. That line was
 * wrong, so both failed on the reference and the run gave no answer. These five (Sep 22) assert
 * only the walk-out tick, one candidate reading each, so exactly one can pass. The reference chose
 * tick 7 (runs 6-12), which matches forum topic 227.
 *
 * mealA is served in tick 5 (see [PartialServingWaitsForTwoTicksSystemTest]).
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

/** Candidate (rejected, fails by design): the extension is ignored, four ticks after ordering. */
class ExtendedPatienceLeavesInTickFiveSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickFiveSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 5"

    override suspend fun run() = assertLastCustomerLeavesInTick(5)
}

/** Candidate (rejected, fails by design): five ticks after ordering, without the extension. */
class ExtendedPatienceLeavesInTickSixSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickSixSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 6"

    override suspend fun run() = assertLastCustomerLeavesInTick(6)
}

/** The reference's tick: six ticks after ordering, the base window of four plus the two extra ticks. */
class ExtendedPatienceLeavesInTickSevenSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickSevenSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 7"

    override suspend fun run() = assertLastCustomerLeavesInTick(7)
}

/** Candidate (rejected, fails by design): the two extra ticks added to a five tick base window. */
class ExtendedPatienceLeavesInTickEightSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickEightSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 8"

    override suspend fun run() = assertLastCustomerLeavesInTick(8)
}

/** Candidate (rejected, fails by design): the extra ticks counted from the serving tick 5. */
class ExtendedPatienceLeavesInTickNineSystemTest : ExtendedPatienceScenario() {
    override val name = "ExtendedPatienceLeavesInTickNineSystemTest"
    override val description = "The last customer of a partly served table leaves in tick 9"

    override suspend fun run() = assertLastCustomerLeavesInTick(9)
}
