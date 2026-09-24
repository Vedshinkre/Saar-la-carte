package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val TOURNANT = "TOURNANT"
private const val SOUP_A = "Soup A"
private const val SOUP_B = "Soup B"
private const val SOUP_C = "Soup C"

/**
 * Narrowing probes (Sep 23) for TieBreakSameCookTypeByLowestExistingIdSystemTest, which passed on
 * our implementation and failed on the reference. That test asserts three things in one run (who
 * cooks in tick 1, that both meals finish in tick 2, who cooks in tick 3), so its failure could not
 * say which one differs. Each question here is its own A/B pair, and each probe pins one line to
 * one tick.
 *
 * Two TOURNANT cooks and nothing else. In tick 1 two groups order two different dishes, so both
 * cooks start and both get an id. Both finish in tick 2. In tick 3 a third group orders, and one of
 * the two idle cooks takes the dish.
 */
abstract class CookIdTieBreakScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/cookidtiebreak/restaurants.json"
    override val scenario = "officehourjson/cookidtiebreak/scenario.json"
    override val food = "officehourjson/cookidtiebreak/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    /** pins one assignment line to exactly [tick] */
    protected suspend fun assertAssignedInTick(tick: Int, cookId: Int, dish: String, orderId: Int) {
        skipUntilString(TickStatusTestLogs.tickStart(tick, 1))
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = cookId,
                cookType = TOURNANT,
                meals = MEALS_PER_GROUP,
                dishName = dish,
                baseOrderId = orderId,
                allOrders = listOf(orderId)
            )
        )
        skipUntilString(TickStatusTestLogs.tickStart(tick + 1, 1))
    }

    protected companion object {
        const val MEALS_PER_GROUP = 2
    }
}

// ---- question 1: are both dishes started at once, and which cook gets which id? ----
// The reference chose reading A for both (runs 9-12).

/**
 * Reading A (the reference's): the cook ids follow the order in which the dishes are assigned, so
 * the earlier order's dish is started by cook 1 in tick 1.
 */
class CookIdsFollowAssignmentOrderSystemTest : CookIdTieBreakScenario() {
    override val name = "CookIdsFollowAssignmentOrderSystemTest"
    override val description = "The dish of the earlier order is started by cook 1 in tick 1"

    override suspend fun run() {
        assertAssignedInTick(tick = 1, cookId = 1, dish = SOUP_A, orderId = 1)
    }
}

/** Reading B (rejected, fails by design): the ids are the other way round, so cook 2 starts Soup A. */
class CookIdsAreReversedOnTheFirstTickSystemTest : CookIdTieBreakScenario() {
    override val name = "CookIdsAreReversedOnTheFirstTickSystemTest"
    override val description = "The dish of the earlier order is started by cook 2 in tick 1"

    override suspend fun run() {
        assertAssignedInTick(tick = 1, cookId = 2, dish = SOUP_A, orderId = 1)
    }
}

/** Reading A, second half (the reference's): the later order of tick 1 also starts in tick 1, by cook 2. */
class BothOrdersOfATickStartTogetherSystemTest : CookIdTieBreakScenario() {
    override val name = "BothOrdersOfATickStartTogetherSystemTest"
    override val description = "Two orders placed in one tick are both started in that tick"

    override suspend fun run() {
        assertAssignedInTick(tick = 1, cookId = 2, dish = SOUP_B, orderId = 2)
    }
}

/**
 * Reading C (rejected, fails by design): the kitchen starts only one order per tick, so the second
 * order of tick 1 is started in tick 2.
 */
class SecondOrderOfATickWaitsOneTickSystemTest : CookIdTieBreakScenario() {
    override val name = "SecondOrderOfATickWaitsOneTickSystemTest"
    override val description = "The second order of a tick is only started in the following tick"

    override suspend fun run() {
        assertAssignedInTick(tick = 2, cookId = 2, dish = SOUP_B, orderId = 2)
    }
}

// ---- question 2: which of two idle cooks that already have ids takes the next job? ----
// The reference chose reading B (runs 9-12): cook 2 takes Soup C. Question 3 finds out why.

/**
 * Reading A (rejected, fails by design): the idle cook with the lowest id takes Soup C. The test
 * asserts nothing about tick 1, so it does not depend on how the ids were given out.
 */
class IdleCookWithLowestIdTakesNextJobSystemTest : CookIdTieBreakScenario() {
    override val name = "IdleCookWithLowestIdTakesNextJobSystemTest"
    override val description = "Of two idle cooks of the same type, the lower id takes the next dish"

    override suspend fun run() {
        assertAssignedInTick(tick = 3, cookId = 1, dish = SOUP_C, orderId = 3)
    }
}

/**
 * Reading B (the reference's): cook 2 takes Soup C. Several rules give this result, and question 3
 * separates them.
 */
class IdleCookWithHigherIdTakesNextJobSystemTest : CookIdTieBreakScenario() {
    override val name = "IdleCookWithHigherIdTakesNextJobSystemTest"
    override val description = "Of two idle cooks of the same type, the higher id takes the next dish"

    override suspend fun run() {
        assertAssignedInTick(tick = 3, cookId = 2, dish = SOUP_C, orderId = 3)
    }
}

// ---- question 3: which rule makes the reference pick cook 2 in question 2? ----

/**
 * Three rules explain the answer to question 2. The two scenarios below tell them apart by letting
 * the cooks finish in different ticks. Each scenario has one test per possible cook. The reference
 * chose "most recently freed" (runs 11-12): cook 1 in the first scenario, cook 2 in the second.
 *
 * Tick 1 again gives Soup A to cook 1 and Soup B to cook 2, but one of the soups now takes 40
 * minutes (finished in tick 4) and the other 10 (finished in tick 1). Soup C is ordered in tick 5,
 * when both cooks are idle again.
 *
 * | rule                                 | cook 1 finishes last | cook 2 finishes last |
 * |--------------------------------------|----------------------|----------------------|
 * | highest id (or last to start)        | cook 2               | cook 2               |
 * | most recently freed (a stack)        | cook 1               | cook 2               |
 * | idle the longest (a queue)           | cook 2               | cook 1               |
 * | lowest id (ours, already disproved)  | cook 1               | cook 1               |
 */
abstract class IdleCookRuleScenario(private val fixture: String) : CookIdTieBreakScenario() {
    override val restaurants = "officehourjson/$fixture/restaurants.json"
    override val scenario = "officehourjson/$fixture/scenario.json"
    override val food = "officehourjson/$fixture/food.json"
    override val maxTicks = 6

    /** the premise: the slow soup finishes in tick 4, then Soup C goes to [cookId] in tick 5 */
    protected suspend fun assertSoupCGoesTo(slowCook: Int, slowDish: String, cookId: Int) {
        skipUntilString(TickStatusTestLogs.tickStart(SLOW_DISH_DONE, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(1, slowCook, MEALS_PER_GROUP, slowDish, SLOW_DISH_DONE - 1))
        assertAssignedInTick(tick = SOUP_C_ORDERED, cookId = cookId, dish = SOUP_C, orderId = 3)
    }

    protected companion object {
        const val SLOW_DISH_DONE = 4
        const val SOUP_C_ORDERED = 5
    }
}

/** Cook 1 cooks the slow Soup A, so cook 2 has been idle since tick 1 and cook 1 only since tick 4. */
abstract class CookOneFinishesLastScenario : IdleCookRuleScenario("idlecookonefinisheslast") {
    protected suspend fun assertSoupCGoesTo(cookId: Int) = assertSoupCGoesTo(1, SOUP_A, cookId)
}

/** Cook 2 cooks the slow Soup B, so cook 1 has been idle since tick 1 and cook 2 only since tick 4. */
abstract class CookTwoFinishesLastScenario : IdleCookRuleScenario("idlecooktwofinisheslast") {
    protected suspend fun assertSoupCGoesTo(cookId: Int) = assertSoupCGoesTo(2, SOUP_B, cookId)
}

/** Rejected, fails by design (highest id or idle the longest): cook 2 takes Soup C. */
class CookOneFinishesLastCookTwoTakesNextJobSystemTest : CookOneFinishesLastScenario() {
    override val name = "CookOneFinishesLastCookTwoTakesNextJobSystemTest"
    override val description = "Cook 1 freed in tick 4 and cook 2 in tick 1: cook 2 takes the next dish"

    override suspend fun run() = assertSoupCGoesTo(cookId = 2)
}

/** The reference's (most recently freed): cook 1, freed in tick 4, takes Soup C. */
class CookOneFinishesLastCookOneTakesNextJobSystemTest : CookOneFinishesLastScenario() {
    override val name = "CookOneFinishesLastCookOneTakesNextJobSystemTest"
    override val description = "Cook 1 freed in tick 4 and cook 2 in tick 1: cook 1 takes the next dish"

    override suspend fun run() = assertSoupCGoesTo(cookId = 1)
}

/** The reference's (most recently freed, or highest id): cook 2, freed in tick 4, takes Soup C. */
class CookTwoFinishesLastCookTwoTakesNextJobSystemTest : CookTwoFinishesLastScenario() {
    override val name = "CookTwoFinishesLastCookTwoTakesNextJobSystemTest"
    override val description = "Cook 2 freed in tick 4 and cook 1 in tick 1: cook 2 takes the next dish"

    override suspend fun run() = assertSoupCGoesTo(cookId = 2)
}

/** Rejected, fails by design (idle the longest): cook 1, idle since tick 1, takes Soup C. */
class CookTwoFinishesLastCookOneTakesNextJobSystemTest : CookTwoFinishesLastScenario() {
    override val name = "CookTwoFinishesLastCookOneTakesNextJobSystemTest"
    override val description = "Cook 2 freed in tick 4 and cook 1 in tick 1: cook 1 takes the next dish"

    override suspend fun run() = assertSoupCGoesTo(cookId = 1)
}
