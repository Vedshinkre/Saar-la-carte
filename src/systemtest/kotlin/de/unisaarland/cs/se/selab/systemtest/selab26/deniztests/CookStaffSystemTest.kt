package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SOUS = "SOUS"
private const val EXEC = "EXEC"

/** Dishes go to the lowest-ranking eligible cook, and cook ids follow the order of first dishes. */
class CookStaffSystemTest : ExampleSystemTestExtension() {
    override val name = "CookStaffSystemTest"
    override val description = "Cook ranking, per-restaurant ids in order of first dish, only eligible cooks"
    override val food = "deniztests/cookstaff/food.json"
    override val restaurants = "deniztests/cookstaff/restaurants.json"
    override val scenario = "deniztests/cookstaff/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipToKitchen(1, customers = 2, waitstaff = 1)
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, SOUS, 1, "soup", 1, listOf(1)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 2, EXEC, 1, "pie", 1, listOf(1)))
        skipToKitchen(2, customers = 1, waitstaff = 1)
        assertNextLine(KitchenTestLogs.kitchenAssign(2, 1, EXEC, 1, "soup", 2, listOf(2)))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipToKitchen(1, customers = 1, waitstaff = 1)
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 2, 0, 0))

        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipToKitchen(1, customers = 0, waitstaff = 0)
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, SOUS, 1, "pie", 3, listOf(3)))

        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipToKitchen(1, customers = 1, waitstaff = 1)
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 3, "ROAST", 1, "roast", 4, listOf(4)))
    }

    /** the kitchen logs of restaurant [restId] follow its ordering status */
    private suspend fun skipToKitchen(restId: Int, customers: Int, waitstaff: Int) {
        skipUntilString(FohArrivalTestLogs.orderingStatus(restId, customers, waitstaff))
    }
}
