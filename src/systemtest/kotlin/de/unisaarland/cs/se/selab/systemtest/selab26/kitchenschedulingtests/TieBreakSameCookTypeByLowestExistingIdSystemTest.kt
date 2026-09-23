package de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val TOURNANT = "TOURNANT"
private const val SOUP_A = "Soup A"
private const val SOUP_B = "Soup B"

/**
 * Spec (p.13-14): "Cooks are seen as interchangeable, they do not have ids assigned at the start
 * of the simulation... A cook is assigned an id the moment they start cooking the first assigned
 * dish, and they will keep it for the remainder of the evening." When several free cooks of the
 * same type are eligible, the one that already has an id (lowest id first) must be preferred over
 * one that has never cooked yet.
 *
 * Tick 1: two different TOURNANT-only dishes are ordered at once with only 2 TOURNANT cooks
 * free and unassigned; one dish is assigned first and its cook becomes Cook 1, the other dish
 * then has only the remaining cook left and it becomes Cook 2 (lazy id assignment).
 * Tick 3: both cooks are free again, and both already have ids -- a third dish must go to
 * Cook 1, the lower-numbered of the two already-assigned cooks, not to Cook 2.
 */
class TieBreakSameCookTypeByLowestExistingIdSystemTest : ExampleSystemTestExtension() {
    override val name = "TieBreakSameCookTypeByLowestExistingIdSystemTest"
    override val description = "Among free cooks of the same type, the one with the lowest existing id is chosen"
    override val restaurants = "kitchenschedulingtests/idtiebreak/restaurants.json"
    override val scenario = "kitchenschedulingtests/idtiebreak/scenario.json"
    override val food = "kitchenschedulingtests/idtiebreak/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 4

    override suspend fun run() {
        // Tick 1: group 1 (Soup A) and group 2 (Soup B) both need a TOURNANT cook at once.
        // Two TOURNANT cooks are free and unassigned: one dish claims a cook first and it
        // becomes Cook 1 (lazy id assignment); the other dish is then left with only the
        // remaining, still-unassigned cook, which becomes Cook 2.
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(
            FohArrivalTestLogs.ordering(1, 1, 1, mapOf(SOUP_A to 2), 1)
        )
        skipUntilString(
            FohArrivalTestLogs.ordering(1, 2, 2, mapOf(SOUP_B to 2), 1)
        )
        skipUntilString(FohArrivalTestLogs.orderingStatus(1, 4, 1))
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = TOURNANT,
                meals = 2,
                dishName = SOUP_A,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 2,
                cookType = TOURNANT,
                meals = 2,
                dishName = SOUP_B,
                baseOrderId = 2,
                allOrders = listOf(2)
            )
        )

        // Tick 2: both meals finish (1 tick after ordering), freeing Cook 1 and Cook 2, each
        // keeping their assigned id for the rest of the evening.
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 1, 2, SOUP_A, 1))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 2, 2, SOUP_B, 1))

        // Tick 3: group 3 orders Soup C. Cook 1 and Cook 2 are both free, both already have
        // ids, and both are equally-ranked TOURNANT cooks: Cook 1 (the lowest existing id)
        // must be chosen, not Cook 2.
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = TOURNANT,
                meals = 2,
                dishName = "Soup C",
                baseOrderId = 3,
                allOrders = listOf(3)
            )
        )
    }
}
