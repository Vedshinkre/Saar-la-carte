package de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val GRILLED_CHICKEN = "Grilled Chicken"

/**
 * Spec (p.13-14): "When the cook starts with their assigned dish, the cook doesn't just cook the
 * meals wanted in that single order but for all orders that include this dish." The scaling
 * effect only folds in orders that are *already queued at the moment the cook starts*: a
 * later-arriving order of the same dish must not be silently absorbed into an already-running
 * cooking job, and must instead wait for its own scheduling opportunity.
 *
 * Group 1 (2 meals of Grilled Chicken) starts the restaurant's only ROAST cook in tick 1.
 * Group 2 orders 2 more meals of Grilled Chicken in tick 2, while that cook is still busy: this
 * dish must not be added to the running job (still exactly 2 meals finish), and it must not get
 * a Kitchen Dish Assignment line of its own until the cook frees up in tick 3 -- at which point
 * it forms its own, separate batch triggered by order 2 alone.
 */
class BatchDoesNotAbsorbLaterArrivingOrderSystemTest : ExampleSystemTestExtension() {
    override val name = "BatchDoesNotAbsorbLaterArrivingOrderSystemTest"
    override val description = "An order placed after a cook already started its job is not folded into that batch"
    override val restaurants = "kitchenschedulingtests/batchboundary/restaurants.json"
    override val scenario = "kitchenschedulingtests/batchboundary/scenario.json"
    override val food = "kitchenschedulingtests/batchboundary/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 4

    override suspend fun run() {
        // Tick 1: group 1 orders 2 meals of Grilled Chicken; the only ROAST cook starts on them.
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(
            FohArrivalTestLogs.ordering(1, 1, 1, mapOf(GRILLED_CHICKEN to 2), 1)
        )
        skipUntilString(FohArrivalTestLogs.orderingStatus(1, 2, 1))
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "ROAST",
                meals = 2,
                dishName = GRILLED_CHICKEN,
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )

        // Tick 2: group 2 orders 2 more meals of Grilled Chicken while the cook is still busy
        // on order 1.
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(
            FohArrivalTestLogs.ordering(1, 2, 2, mapOf(GRILLED_CHICKEN to 2), 1)
        )

        // The cook finishes order 1's original 2 meals -- not 4 -- proving order 2's dishes were
        // not folded into the already-running job.
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 1, 2, GRILLED_CHICKEN, 1))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 2, 2, 2))

        // Tick 3: the cook is free again and now starts order 2's Grilled Chicken as its own,
        // separate batch -- triggered by order 2 alone, not merged retroactively into order 1.
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "ROAST",
                meals = 2,
                dishName = GRILLED_CHICKEN,
                baseOrderId = 2,
                allOrders = listOf(2)
            )
        )
    }

    private companion object {
        const val GRILLED_CHICKEN = "Grilled Chicken"
    }
}
