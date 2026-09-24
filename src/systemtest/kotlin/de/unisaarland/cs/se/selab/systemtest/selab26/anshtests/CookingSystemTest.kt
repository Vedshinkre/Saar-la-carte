package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val TOURNANT = "TOURNANT"
private const val STEW = "long stew"
private const val BOWL = "quick bowl"
private const val CURRY = "slow curry"

/**
 * for F12
 * 2 tournant cooks, 1 waiter
 *
 * regular group n (1 customer) orders in tick n: stew (30, 2 ticks), stew, bowl (10, 0 ticks), curry (11, 1 tick), bowl
 *
 * cook 1 busy with stew 1 -> cook 2 takes stew 2
 * cook 1 done in tick 3 but only picks up the tick 3 bowl in tick 4
 * curry from tick 4 waits till tick 5
 *
 * ids restart at 1 on evening 2
 */
class CookingSystemTest : ExampleSystemTestExtension() {
    override val name = "CookingDurationsBusyCooksAndIds"
    override val description = "Cooking durations, busy cooks, next-tick restarts, kitchen status and cook ids"
    override val food = "anshtests/cooking/food.json"
    override val restaurants = "anshtests/cooking/restaurants.json"
    override val scenario = "anshtests/cooking/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 25

    override suspend fun run() {
        kitchenOfTick(1, 1)
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, STEW, 1, listOf(1)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))

        kitchenOfTick(2, 1)
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 2, TOURNANT, 1, STEW, 2, listOf(2)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 2, 0, 0))

        kitchenOfTick(3, 1)
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, STEW, 2))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 2, 1, 1))

        kitchenOfTick(4, 1)
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, BOWL, 3, listOf(3)))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, BOWL, 1))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 2, 1, STEW, 2))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 2, 2, 2))

        kitchenOfTick(5, 1)
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, CURRY, 4, listOf(4)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 2, TOURNANT, 1, BOWL, 5, listOf(5)))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 2, 1, BOWL, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 2, 1, 1))

        kitchenOfTick(6, 0)
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, CURRY, 2))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 1, 1))

        kitchenOfTick(1, 1, evening = 2)
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, STEW, 6, listOf(6)))
    }

    /** skip to the ordering status of the tick, kitchen logs come right after */
    private suspend fun kitchenOfTick(tick: Int, customers: Int, evening: Int = 1) {
        skipUntilString(TickStatusTestLogs.tickStart(tick, evening))
        skipUntilString(FohArrivalTestLogs.orderingStatus(1, customers, customers))
    }
}
