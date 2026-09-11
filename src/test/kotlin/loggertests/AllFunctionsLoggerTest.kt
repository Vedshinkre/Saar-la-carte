package loggertests

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.loggers.DeliveryLogger
import de.unisaarland.cs.se.selab.loggers.FohReceptionLogger
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger
import de.unisaarland.cs.se.selab.loggers.KitchenLogger
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.loggers.StatisticsLogger
import de.unisaarland.cs.se.selab.loggers.TickStatusLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

class AllFunctionsLoggerTest {

    private lateinit var stringWriter: StringWriter
    private lateinit var printWriter: PrintWriter

    @BeforeEach
    fun setUp() {
        stringWriter = StringWriter()
        printWriter = PrintWriter(stringWriter)

        Logger.setup(printWriter)
        Logger.setup(LogLevel.DEBUG)

        Logger.restaurantID = 0
    }

    @AfterEach
    fun tearDown() {
        printWriter.flush()
        printWriter.close()
    }

    private fun getLoggedLines(): List<String> {
        printWriter.flush()

        return stringWriter
            .toString()
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .toList()
    }

    private fun clearLog() {
        printWriter.flush()
        stringWriter.buffer.setLength(0)
    }

    private fun assertLoggedLines(vararg expectedLines: String) {
        assertEquals(expectedLines.toList(), getLoggedLines())
    }

    @Test
    fun `test logging flow from start to end`() {
        verifyInitializationPhase()
        verifyPreparationPhase()
        verifyServingDecisionPhase()
        verifyRestaurantActionPhase()
        verifyKitchenPhase()
        verifyServiceAndDeliveryPhase()
        verifyEatingAndRatingPhase()
        verifySimulationEndPhase()
    }
    private fun verifyRestaurantActionPhase() {
        logRestaurantActionPhase()
        assertRestaurantActionPhase()
        clearLog()
    }

    private fun logRestaurantActionPhase() {
        TickStatusLogger.logRestaurantStart()

        FohReceptionLogger.logRestaurantArrival(
            groupId = 1
        )

        FohReceptionLogger.logFohMergingTables(
            groupId = 1,
            oldTableIds = listOf(4, 3, 5),
            mergedTableId = 3
        )

        FohReceptionLogger.logFohSeating(
            groupId = 1,
            tableId = 3,
            waitstaffIds = listOf(2, 1)
        )

        FohReceptionLogger.logFohNoSeatingNoWaitstaff(
            groupId = 3
        )

        FohReceptionLogger.logFohNoSeating(
            groupId = 4,
            waitstaffId = 2
        )

        FohReceptionLogger.logFohOrdering(
            groupId = 1,
            orderId = 10,
            dishNameToAmount = mapOf(
                "chicken rice" to 2,
                "potato soup" to 1
            ),
            waitstaffId = 1
        )

        FohReceptionLogger.logFohNoOrdering(
            groupId = 5,
            customerNumber = 2
        )

        FohReceptionLogger.logSeatingStatus(
            waitstaffNumber = 2,
            customerNumber = 8,
            tableNumber = 1
        )

        FohReceptionLogger.logOrderingStatus(
            customerNumber = 3,
            tableNumber = 1
        )
    }

    private fun assertRestaurantActionPhase() {
        assertLoggedLines(
            "[DEBUG] Restaurant Start (R 1): Restaurant 1 simulates a tick.",
            "[INFO] Restaurant Arrival (R 1): Group 1 arrived at restaurant 1.",
            "[INFO] FOH Merging Tables (R 1): For group 1 the tables 3,4,5 were merged into 3.",
            "[IMPORTANT] FOH Seating (R 1): Group 1 seated at table 3 by waitstaff 1,2.",
            "[INFO] FOH No Seating (R 1): No free waitstaff available for group 3.",
            "[INFO] FOH No Seating (R 1): Assigned waitstaff 2 but no table available, " +
                "group 4 is sent away.",
            "[IMPORTANT] FOH Ordering (R 1): Group 1 placed order 10 of chicken rice:2," +
                "potato soup:1 with waitstaff 1.",
            "[IMPORTANT] FOH No Ordering (R 1): Group 1 could not place an order for 2 " +
                "customers, they leave the restaurant.",
            "[DEBUG] FOH Seating Status (R 1): 2 waitstaff seated 8 customers on 1 tables.",
            "[DEBUG] FOH Ordering Status (R 1): The restaurant received orders from 3 " +
                "customers, 1 waitstaff took orders."
        )
    }

    private fun verifyServiceAndDeliveryPhase() {
        logServicePhase()
        logDeliveryPhase()
        assertServiceAndDeliveryPhase()
        clearLog()
    }

    private fun logServicePhase() {
        FohServiceLogger.logFohServing(
            waitstaffId = 1,
            dishNameToAmount = mapOf("chicken rice" to 2),
            tableId = 3,
            orderDurationTick = 1
        )

        FohServiceLogger.logFohNoServing(
            waitstaffId = 1,
            mealNumber = 1,
            tableId = 3
        )

        FohServiceLogger.logFohDelivery(
            waitstaffId = 1,
            dishNameToAmount = mapOf("potato soup" to 1),
            driverId = 1,
            orderId = 12
        )

        FohServiceLogger.logFohServingStatus(
            waitstaffNumber = 1,
            mealTotalNumber = 3
        )
    }

    private fun logDeliveryPhase() {
        DeliveryLogger.logDeliveryPreparation(
            driverId = 1,
            orderId = 12,
            groupId = 2,
            ticksRequiredToDeliver = 3
        )

        DeliveryLogger.logDeliveryDriving(
            driverId = 1,
            distanceCovered = 5,
            ticksRequiredToDelivers = 2
        )

        DeliveryLogger.logDeliveryArrival(
            driverId = 1,
            groupId = 2,
            orderId = 12
        )

        DeliveryLogger.logDeliveryFinished(
            driverId = 1,
            orderId = 12,
            groupId = 2
        )

        DeliveryLogger.logDeliveryFailed(
            driverId = 1,
            orderId = 13,
            groupId = 3
        )

        DeliveryLogger.logDeliveryGivenUp(
            groupId = 3,
            orderId = 13
        )

        DeliveryLogger.logDeliveryReturned(
            driverId = 1
        )

        DeliveryLogger.logDeliveryFinishedEating(
            groupId = 2
        )
    }

    private fun assertServiceAndDeliveryPhase() {
        assertLoggedLines(
            "[IMPORTANT] FOH Serving (R 1): Waitstaff 1 serves chicken rice:2 to table 3 " +
                "1 ticks after ordering.",
            "[DEBUG] FOH No Serving (R 1): Waitstaff 1 did not serve 1 meals to table 3.",
            "[IMPORTANT] FOH Delivery (R 1): Waitstaff 1 serves potato soup:1 meals to " +
                "driver 1 for order 12.",
            "[DEBUG] FOH Serving Status (R 1): 1 waitstaff served 3 meals.",
            "[INFO] Delivery Preparation (R 1): Driver 1 prepares driving order 12 to " +
                "group 2, which will take 3 ticks.",
            "[DEBUG] Delivery Driving (R 1): Driver 1 drove 5 km and needs 2 more ticks.",
            "[INFO] Delivery Arrival (R 1): Driver 1 arrived at group 2 with order 12.",
            "[IMPORTANT] Delivery Finished (R 1): Driver 1 gave delivery of order 12 " +
                "to group 2.",
            "[IMPORTANT] Delivery Failed (R 1): Driver 1 failed to deliver order 13 " +
                "to group 3.",
            "[INFO] Delivery Given Up (R 1): Group 3 gave up on waiting for delivery of " +
                "order 13.",
            "[INFO] Delivery Returned (R 1): Driver 1 has returned.",
            "[INFO] Delivery Finished Eating (R 1): Group 2 has finished eating."
        )
    }
    private fun verifyInitializationPhase() {
        InitialAndPrepLogger.logInitialization(
            success = true,
            filename = "food.json"
        )

        InitialAndPrepLogger.logInitialization(
            success = true,
            filename = "restaurants.json"
        )

        InitialAndPrepLogger.logInitialization(
            success = false,
            filename = "invalid_scenario.json"
        )

        InitialAndPrepLogger.logSimulationStart()

        assertLoggedLines(
            "[INFO] Initialization Info: food.json successfully parsed and validated.",
            "[INFO] Initialization Info: restaurants.json successfully parsed and validated.",
            "[IMPORTANT] Initialization Info: invalid_scenario.json is invalid.",
            "[INFO] Simulation Info: Simulation started."
        )

        clearLog()
    }

    private fun verifyPreparationPhase() {
        InitialAndPrepLogger.logIncident(
            incidentId = 1,
            incidentType = "STAFF"
        )

        InitialAndPrepLogger.logPreparationStart()

        Logger.restaurantID = 1

        InitialAndPrepLogger.logFohNoReservation(
            groupId = 6
        )

        InitialAndPrepLogger.logPantryRemovedIngredient(
            removedIngredientAmount = 300,
            ingredientName = "beef"
        )

        InitialAndPrepLogger.logPantryRemovedIngredient(
            removedIngredientAmount = 500,
            ingredientName = "pasta"
        )

        InitialAndPrepLogger.logPantryProcured(
            amount = 1000,
            unit = MeasurementUnit.G,
            name = "chicken"
        )

        InitialAndPrepLogger.logPantryProcured(
            amount = 2000,
            unit = MeasurementUnit.G,
            name = "rice"
        )

        InitialAndPrepLogger.logPantryRestocked()

        assertLoggedLines(
            "[IMPORTANT] Incident: Incident 1 of type STAFF occurred before evening 1.",
            "[IMPORTANT] Preparation: Preparation for evening 1 starts.",
            "[IMPORTANT] FOH No Reserving (R 1): No table could be reserved for group 6.",
            "[DEBUG] Pantry (R 1): Removed 300 G of beef from the pantry.",
            "[DEBUG] Pantry (R 1): Removed 500 G of pasta from the pantry.",
            "[DEBUG] Pantry (R 1): Procured 1000 G of chicken from the supplier.",
            "[DEBUG] Pantry (R 1): Procured 2000 G of rice from the supplier.",
            "[INFO] Pantry (R 1): Restocked ingredients."
        )

        clearLog()
    }

    private fun verifyServingDecisionPhase() {
        TickStatusLogger.logServingStart()
        TickStatusLogger.logCurrentTick()

        TickStatusLogger.logRestaurantDecision(
            groupId = 1,
            restId = 1
        )

        TickStatusLogger.logRestaurantNoDecision(
            groupId = 2
        )

        assertLoggedLines(
            "[IMPORTANT] Serving: Serving of evening 1 starts.",
            "[IMPORTANT] Simulation: Tick 1 (1) started.",
            "[DEBUG] Restaurant Decision: Group 1 decided on restaurant 1.",
            "[DEBUG] Restaurant No Decision: Group 2 could not decide for a restaurant."
        )

        clearLog()
    }

    private fun verifyKitchenPhase() {
        KitchenLogger.logKitchenDishAssignment(
            cookId = 1,
            cookType = "TOURNANT",
            numberOfMeals = 2,
            dishName = "chicken rice",
            baseOrderId = 10,
            allOrderIds = listOf(10, 11)
        )

        KitchenLogger.logKitchenMealCooked(
            cookId = 1,
            numberOfMeals = 2,
            dishName = "chicken rice",
            cookDurationTick = 2
        )

        KitchenLogger.logKitchenStatus(
            numberOfCooks = 1,
            totalNumberOfMeals = 2,
            finishedNumberOfMeals = 2,
            servableMeals = 2
        )

        assertLoggedLines(
            "[IMPORTANT] Kitchen Dish Assignment (R 1): Cook 1 of type TOURNANT starts " +
                "cooking 2 meals of dish chicken rice based on order 10 for orders 10,11.",
            "[IMPORTANT] Kitchen Meal Cooked (R 1): Cook 1 finished cooking 2 meals of " +
                "dish chicken rice 2 ticks after ordering.",
            "[DEBUG] Kitchen Status (R 1): 1 cooks were active cooking 2 and finishing 2 " +
                "meals. 2 meals can be served by the waitstaff."
        )

        clearLog()
    }

    private fun verifyEatingAndRatingPhase() {
        FohServiceLogger.logRestaurantNoEating(
            customerNumber = 2,
            groupId = 1,
            tableId = 3
        )

        FohServiceLogger.logFohFinishedEating(
            customerNumber = 6,
            groupId = 1,
            tableId = 3
        )

        FohServiceLogger.logFohEatingStatus(
            numberOfEatingCustomers = 0,
            numberOfFinishedCustomers = 6
        )

        FohServiceLogger.logFohEscorting(
            waitstaffId = 1,
            numberOfCustomers = 6,
            groupId = 1,
            tableId = 3
        )

        FohServiceLogger.logFohEscortingStatus(
            waitstaffNumber = 1,
            customerEscortingNumber = 6
        )

        FohServiceLogger.logCustomerRateRestaurant(
            groupID = 1,
            groupRating = 5,
            positiveRatingQuantity = 12,
            negativeRatingQuantity = 5
        )

        FohServiceLogger.logRatingStatus(
            numberOfGroupsGivingRatings = 1
        )

        TickStatusLogger.logRestaurantEnd()

        assertLoggedLines(
            "[INFO] Restaurant No Eating (R 1): 2 customers of group 1 leave table 3 " +
                "due to not being served.",
            "[INFO] FOH Finished Eating (R 1): 6 customers of group 1 have finished " +
                "eating at table 3.",
            "[DEBUG] FOH Eating Status (R 1): 0 customers are eating and 6 customers " +
                "have finished eating this tick.",
            "[IMPORTANT] FOH Escorting (R 1): Waitstaff 1 escorts 6 customers of group " +
                "1 from table 3 outside.",
            "[DEBUG] FOH Escorting Status (R 1): 1 waitstaff escorted 6 customers this tick.",
            "[INFO] Rating (R 1): Group 1 rates the restaurant 1 with 5 rating, " +
                "leading to 12 positive ratings and 5 negative ratings.",
            "[DEBUG] Rating Status (R 1): 1 groups performed ratings this tick.",
            "[DEBUG] Restaurant End (R 1): Restaurant 1 finished simulating the tick."
        )

        clearLog()
    }

    private fun verifySimulationEndPhase() {
        TickStatusLogger.logServingEnd()

        StatisticsLogger.logSimulationStatsCalculated()

        StatisticsLogger.logSimulationStatsCooked(
            numberOfCookedMeals = 120
        )

        StatisticsLogger.logSimulationStatsServed(
            numberOfCustomersServed = 98
        )

        StatisticsLogger.logSimulationStatsDelivered(
            numberOfCustomersDelivered = 22
        )

        StatisticsLogger.logSimulationStatsRatingsGiven(
            numberOfCustomersGivingRatings = 15
        )

        assertLoggedLines(
            "[IMPORTANT] Serving: Serving of evening 1 ends.",
            "[IMPORTANT] Simulation Info: Simulation statistics are calculated.",
            "[IMPORTANT] Simulation Statistics: Restaurant 1 cooked 120 meals.",
            "[IMPORTANT] Simulation Statistics: Restaurant 1 served 98 customers.",
            "[IMPORTANT] Simulation Statistics: Restaurant 1 delivered meals to 22 customers.",
            "[IMPORTANT] Simulation Statistics: Restaurant 1 received 15 ratings."
        )
    }
}
