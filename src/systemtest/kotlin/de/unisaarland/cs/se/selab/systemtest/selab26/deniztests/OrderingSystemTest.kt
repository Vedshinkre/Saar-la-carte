package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** Customers order most exclusions first, then fewest favorites, and failed groups use no order id. */
class OrderingSystemTest : ExampleSystemTestExtension() {
    override val name = "OrderingSystemTest"
    override val description = "Customer ordering sequence, failed customers, ordering status and order ids"
    override val food = "deniztests/ordering/food.json"
    override val restaurants = "deniztests/ordering/restaurants.json"
    override val scenario = "deniztests/ordering/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 27

    override suspend fun run() {
        skipUntilString(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("bread" to 1, "cake" to 2), 1))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 1, 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 4, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 3, 1))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        skipUntilString(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 2, 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 1, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(FohArrivalTestLogs.seating(2, 3, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 3, 2, mapOf("soup" to 1), 1))

        skipUntilString(TickStatusTestLogs.tickStart(3, 2))
        skipUntilString(FohArrivalTestLogs.seating(2, 3, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(2, 3, 3, mapOf("soup" to 1), 1))
    }
}
