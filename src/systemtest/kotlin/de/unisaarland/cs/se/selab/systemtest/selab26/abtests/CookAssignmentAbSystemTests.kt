package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** A/B probe for how many dishes of one order the kitchen starts in a single tick */
abstract class CookAssignmentScenario : ExampleSystemTestExtension() {
    override val restaurants = "abtests/cookparallel/restaurants.json"
    override val scenario = "abtests/cookparallel/scenario.json"
    override val food = "abtests/cookparallel/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    /**
     * Pins the Kitchen Status line to tick 1: the skip starts after the ordering status of tick 1,
     * and the start of tick 2 must still lie ahead of the line, so it cannot come from a later tick.
     */
    protected suspend fun assertKitchenStatusOfTickOne(cooks: Int, cooking: Int) {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.orderingStatus(restId = 1, customers = 2, waitstaff = 1))
        skipUntilString(
            KitchenTestLogs.kitchenStatus(restId = 1, cooks = cooks, cooking = cooking, finished = 0, servable = 0)
        )
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
    }
}

/**
 * Reading A: every dish of the order goes to a cook as soon as one is free, so both cooks start in
 * tick 1. This is what we implement.
 */
class OrderDishesStartInParallelSystemTest : CookAssignmentScenario() {
    override val name = "OrderDishesStartInParallelSystemTest"
    override val description = "Reading A: both dishes of one order are started in the same tick by two cooks"

    override suspend fun run() = assertKitchenStatusOfTickOne(cooks = 2, cooking = 2)
}

/** Reading B: the kitchen hands out only one dish per order per tick, so the second cook idles. */
class OrderDishesStartOnePerTickSystemTest : CookAssignmentScenario() {
    override val name = "OrderDishesStartOnePerTickSystemTest"
    override val description = "Reading B: only the first dish of an order is started in the ordering tick"

    override suspend fun run() = assertKitchenStatusOfTickOne(cooks = 1, cooking = 1)
}
