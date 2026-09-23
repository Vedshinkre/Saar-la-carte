package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val TOURNANT = "TOURNANT"
private const val SOUP_A = "Soup A"
private const val SOUP_B = "Soup B"
private const val SOUP_C = "Soup C"

/**
 * Narrowing probes for TieBreakSameCookTypeByLowestExistingIdSystemTest, which passes on our
 * implementation but fails against the reference. That test asserts three separate things in one
 * run - who cooks in tick 1, that both meals finish in tick 2, and who cooks in tick 3 - so its
 * failure cannot say which of them the reference disagrees with.
 *
 * The scenario: a restaurant with exactly two TOURNANT cooks and nothing else. In tick 1 two groups
 * order two different TOURNANT-only dishes at once, so both cooks are put to work and both earn an
 * id. Both meals finish in tick 2, leaving two idle cooks that already have ids. In tick 3 a third
 * group orders a third TOURNANT-only dish, and exactly one of the two idle cooks has to take it.
 *
 * Each probe below pins a single line to a single tick: the skip starts at that tick, finds the
 * line, and then requires the following tick's start to still be ahead of it. Within each pair only
 * one can pass, so the next run names the rule outright.
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

// ---- question 1: are both dishes started at once, and which cook earns which id? ----

/**
 * Reading A: both orders of tick 1 are started in that same tick, and the cook ids follow the order
 * the dishes were assigned in, so the earlier order's dish belongs to cook 1. This is ours.
 */
class CookIdsFollowAssignmentOrderSystemTest : CookIdTieBreakScenario() {
    override val name = "CookIdsFollowAssignmentOrderSystemTest"
    override val description = "The dish of the earlier order is started by cook 1 in tick 1"

    override suspend fun run() {
        assertAssignedInTick(tick = 1, cookId = 1, dish = SOUP_A, orderId = 1)
    }
}

/** Reading B: the ids come out the other way round, so the earlier order's dish belongs to cook 2. */
class CookIdsAreReversedOnTheFirstTickSystemTest : CookIdTieBreakScenario() {
    override val name = "CookIdsAreReversedOnTheFirstTickSystemTest"
    override val description = "The dish of the earlier order is started by cook 2 in tick 1"

    override suspend fun run() {
        assertAssignedInTick(tick = 1, cookId = 2, dish = SOUP_A, orderId = 1)
    }
}

/** Reading A, second half: the later order of tick 1 is started in tick 1 as well, by the other cook. */
class BothOrdersOfATickStartTogetherSystemTest : CookIdTieBreakScenario() {
    override val name = "BothOrdersOfATickStartTogetherSystemTest"
    override val description = "Two orders placed in one tick are both started in that tick"

    override suspend fun run() {
        assertAssignedInTick(tick = 1, cookId = 2, dish = SOUP_B, orderId = 2)
    }
}

/**
 * Reading C: only one order is started per tick, so the second order of tick 1 waits and is started
 * in tick 2 instead. If this passes, the divergence is about how many jobs a kitchen starts per
 * tick and not about cook ids at all.
 */
class SecondOrderOfATickWaitsOneTickSystemTest : CookIdTieBreakScenario() {
    override val name = "SecondOrderOfATickWaitsOneTickSystemTest"
    override val description = "The second order of a tick is only started in the following tick"

    override suspend fun run() {
        assertAssignedInTick(tick = 2, cookId = 2, dish = SOUP_B, orderId = 2)
    }
}

// ---- question 2: which of two idle, already-numbered cooks takes the next job? ----

/**
 * Reading A: the free cook with the **lowest** id takes it, so the third dish goes back to cook 1.
 * This is ours. The assertion deliberately says nothing about tick 1, so it stands on its own even
 * if the ids were handed out differently there.
 */
class IdleCookWithLowestIdTakesNextJobSystemTest : CookIdTieBreakScenario() {
    override val name = "IdleCookWithLowestIdTakesNextJobSystemTest"
    override val description = "Of two idle cooks of the same type, the lower id takes the next dish"

    override suspend fun run() {
        assertAssignedInTick(tick = 3, cookId = 1, dish = SOUP_C, orderId = 3)
    }
}

/**
 * Reading B: the other cook takes it - the one that did not cook most recently, or simply the
 * higher id. Either way the third dish goes to cook 2.
 */
class IdleCookWithHigherIdTakesNextJobSystemTest : CookIdTieBreakScenario() {
    override val name = "IdleCookWithHigherIdTakesNextJobSystemTest"
    override val description = "Of two idle cooks of the same type, the higher id takes the next dish"

    override suspend fun run() {
        assertAssignedInTick(tick = 3, cookId = 2, dish = SOUP_C, orderId = 3)
    }
}
