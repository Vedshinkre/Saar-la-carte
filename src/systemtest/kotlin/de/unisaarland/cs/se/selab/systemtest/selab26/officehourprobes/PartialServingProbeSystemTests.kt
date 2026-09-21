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
 * A/B probe for the two extra ticks a table gets once one of its customers has been served
 * (specification page 21): "If at least one person on the table has received their meal, they wait
 * for 2 more ticks."
 *
 * mealA reaches the table in tick 5, mealB in tick 7, and mealC is only assigned to the cook in tick
 * 8. Reading A counts the two extra ticks on top of the five of the base window, so the last
 * customer leaves once Time.tick - orderedAt reaches 7, which is tick 8. This is what we implement.
 */
class ExtendedPatienceEndsSevenTicksAfterOrderingSystemTest : PartialServingScenario() {
    override val name = "ExtendedPatienceEndsSevenTicksAfterOrderingSystemTest"
    override val description = "The last customer of a partly served table leaves 7 ticks after ordering"

    override suspend fun run() {
        assertSharedSetup()
        // nothing may be given up before tick 8, so the skip starts there
        skipUntilString(TickStatusTestLogs.tickStart(8, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 1, 0))
    }
}

/** Reading B: the two extra ticks are counted inclusively, so the last customer leaves in tick 7. */
class ExtendedPatienceEndsSixTicksAfterOrderingSystemTest : PartialServingScenario() {
    override val name = "ExtendedPatienceEndsSixTicksAfterOrderingSystemTest"
    override val description = "The last customer of a partly served table leaves 6 ticks after ordering"

    override suspend fun run() {
        assertSharedSetup()
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 1, 1, 1))
        // in tick 7 the customer of mealA is the one who has just finished eating
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 1, 1))
    }
}
