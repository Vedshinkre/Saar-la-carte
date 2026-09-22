package de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * Spec (p.13): among dishes with equal basic-ness, the tie is broken by "the lower id of the
 * dish recipe" alone -- not by dish name, and not by the order the recipes were declared in the
 * food file or the restaurant's own recipe list.
 *
 * Both dishes here are non-basic. "Apple Tart" (recipe id 5) is both declared first in the food
 * file and comes first alphabetically; "Zucchini Bake" (recipe id 3) is declared second and
 * comes last alphabetically. Only the ascending recipe id (3 before 5) predicts the correct
 * assignment order.
 */
class NonBasicDishesTieBreakByAscendingIdSystemTest : ExampleSystemTestExtension() {
    override val name = "NonBasicDishesTieBreakByAscendingIdSystemTest"
    override val description = "Two non-basic dishes in one order are assigned by ascending recipe id"
    override val restaurants = "kitchenschedulingtests/nonbasictiebreak/restaurants.json"
    override val scenario = "kitchenschedulingtests/nonbasictiebreak/scenario.json"
    override val food = "kitchenschedulingtests/nonbasictiebreak/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 2

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        skipUntilString(
            FohArrivalTestLogs.ordering(
                1,
                1,
                1,
                mapOf("Apple Tart" to 1, "Zucchini Bake" to 1),
                1
            )
        )
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 2, 1))

        // Zucchini Bake has the lower recipe id (3) so it must be assigned first, despite
        // Apple Tart (id 5) being declared earlier in the file and sorting first alphabetically.
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "VEGETABLE",
                meals = 1,
                dishName = "Zucchini Bake",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 2,
                cookType = "PASTRY",
                meals = 1,
                dishName = "Apple Tart",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 2, 0, 0))
    }
}
