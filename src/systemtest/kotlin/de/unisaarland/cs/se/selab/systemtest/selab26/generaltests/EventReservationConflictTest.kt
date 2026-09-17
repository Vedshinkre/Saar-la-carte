package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * two people want reserved tables , one gets it the other doesn't
 */
class EventReservationConflictTest : ExampleSystemTestExtension() {
    override val name = "EventReservationConflictTest"
    override val description = "Tests reservation conflict between EVENT and REGULAR groups on Evening 4"
    override val restaurants = "notablereservationjson/restaurants.json"
    override val scenario = "notablereservationjson/scenario.json"
    override val food = "notablereservationjson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        // Skip till Tick 1 of Evening 1 where the EVENT group makes its decision
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))

        //  Skip to Evening 4 Preparation where the conflict happens
        skipUntilString(InitialAndPrepTestLogs.prepStart(4))

        // EVENT group takes all tables, leaving nothing for the REGULAR group (Group 2)
        assertNextLine(InitialAndPrepTestLogs.fohNoReserving(1, 2))

        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 2500, "g", "Chicken"))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(TickStatusTestLogs.servingStart(4))

        //  Evening 4 Tick 1: The rejected REGULAR group leaves a negative rating
        skipUntilString(TickStatusTestLogs.tickStart(1, 4))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.rating(1, 2, "NEGATIVE", 10, 4))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))
        assertNextLine(TickStatusTestLogs.restEnd(1))

        // 4. Evening 4 Tick 2: The EVENT group arrives and takes the merged tables
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        assertNextLine(TickStatusTestLogs.restStart(1))

        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 1, listOf(1, 2, 3), 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))

        assertNextLine(
            FohArrivalTestLogs.ordering(
                restId = 1,
                groupId = 1,
                orderId = 1,
                dishes = mapOf("Grilled Chicken" to 10),
                waitstaffId = 1
            )
        )

        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 10, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 10, 1))
        assertNextLine(
            KitchenTestLogs.kitchenAssign(
                restId = 1,
                cookId = 1,
                cookType = "ROAST",
                meals = 10,
                dishName = "Grilled Chicken",
                baseOrderId = 1,
                allOrders = listOf(1)
            )
        )

        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 10, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
