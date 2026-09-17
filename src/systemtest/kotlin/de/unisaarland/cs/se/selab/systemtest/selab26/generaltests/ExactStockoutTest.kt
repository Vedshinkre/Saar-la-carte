package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * this test is supposed to test the case where the restaurant runs out
 * of ingredient and one customer needs to run out of the restaurant
 */
class ExactStockoutTest : ExampleSystemTestExtension() {
    override val name = "ExactStockoutTest"
    override val description = "Tests real-time stockout when all food is over"
    override val restaurants = "nofoodforyoujson/restaurants.json"
    override val scenario = "nofoodforyoujson/scenario.json"
    override val food = "nofoodforyoujson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    override suspend fun run() {
        assertEvening1Preparation()
        assertEvening1Tick1()
    }

    private suspend fun assertEvening1Preparation() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))

        // #otherSeats = 2. ceil(2/10) = 1.
        // The kitchen plans for exactly 1 of each recipe
        // 1 * 250g Chicken = 250g = exactly 1 package.
        // 1 * 100g Tomato = 100g = exactly 1 package.
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 250, "g", "Chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 100, "g", "Tomato"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        assertNextLine(TickStatusTestLogs.servingStart(1))
    }

    private suspend fun assertEvening1Tick1() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))

        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))

        // Group 1 arrives and is seated by Waitstaff 1
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))

        // Customer A successfully orders the 1 and only Grilled Chicken.
        // The kitchen instantly reserves the 250g of Chicken. Stock drops to 0.
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf("Grilled Chicken" to 1), 1))

        // Customer B cannot eat Tomato. The only dish left is Tomato Soup.
        // Customer B has nothing to eat and leaves immediately!
        assertNextLine(FohArrivalTestLogs.noOrdering(1, 1, 1))

        // Status: 2 people were seated, but only 1 ordered.
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 2, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 1, 1))

        skipUntilString(TickStatusTestLogs.restEnd(1))
    }
}
