package loggertests

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.loggers.*
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
    }

    @AfterEach
    fun tearDown() {
        printWriter.close()
    }

    private fun getLoggedLines(): List<String> {
        printWriter.flush()
        val content = stringWriter.toString()

        if (content.isEmpty()) {
            return emptyList()
        }

        return content
            .trim()
            .split("\\r?\\n".toRegex())
            .map { it.trim() }
    }

    @Suppress("LongMethod")
    @Test
    fun `test logging flow from start to end`() {
        // =====================================================================
        // STEP 1: INITIALIZATION PHASE
        // =====================================================================
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

        var lines = getLoggedLines()

        assertEquals(4, lines.size)
        assertEquals(
            "[INFO] Initialization Info: food.json successfully parsed and validated.",
            lines[0]
        )
        assertEquals(
            "[INFO] Initialization Info: restaurants.json successfully parsed and validated.",
            lines[1]
        )
        assertEquals(
            "[IMPORTANT] Initialization Info: invalid_scenario.json is invalid.",
            lines[2]
        )
        assertEquals(
            "[INFO] Simulation Info: Simulation started.",
            lines[3]
        )

        stringWriter.buffer.setLength(0)

        // =====================================================================
        // STEP 2: PREPARATION PHASE & INCIDENTS
        // =====================================================================
        InitialAndPrepLogger.logIncident(
            incidentId = 1,
            incidentType = "STAFF"
        )

        InitialAndPrepLogger.logPreparationStart()

        Logger.restaurantID = 1

        InitialAndPrepLogger.logFohNoReservation(groupId = 6)

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

        lines = getLoggedLines()

        assertEquals(8, lines.size)
        assertEquals(
            "[IMPORTANT] Incident: Incident 1 of type STAFF occurred before evening 1.",
            lines[0]
        )
        assertEquals(
            "[IMPORTANT] Preparation: Preparation for evening 1 starts.",
            lines[1]
        )
        assertEquals(
            "[IMPORTANT] FOH No Reserving (R 1): No table could be reserved for group 6.",
            lines[2]
        )
        assertEquals(
            "[DEBUG] Pantry (R 1): Removed 300 G of beef from the pantry.",
            lines[3]
        )
        assertEquals(
            "[DEBUG] Pantry (R 1): Removed 500 G of pasta from the pantry.",
            lines[4]
        )
        assertEquals(
            "[DEBUG] Pantry (R 1): Procured 1000 G of chicken from the supplier.",
            lines[5]
        )
        assertEquals(
            "[DEBUG] Pantry (R 1): Procured 2000 G of rice from the supplier.",
            lines[6]
        )
        assertEquals(
            "[INFO] Pantry (R 1): Restocked ingredients.",
            lines[7]
        )

        stringWriter.buffer.setLength(0)

        // =====================================================================
        // STEP 3: SERVING PHASE - TICK 1 INITIALIZATION & DECISION
        // =====================================================================
        TickStatusLogger.logServingStart()
        TickStatusLogger.logCurrentTick()

        TickStatusLogger.logRestaurantDecision(
            groupId = 1,
            restId = 1
        )
        TickStatusLogger.logRestaurantNoDecision(groupId = 2)

        lines = getLoggedLines()

        assertEquals(4, lines.size)
        assertEquals(
            "[IMPORTANT] Serving: Serving of evening 1 starts.",
            lines[0]
        )
        assertEquals(
            "[IMPORTANT] Simulation: Tick 1 (1) started.",
            lines[1]
        )
        assertEquals(
            "[DEBUG] Restaurant Decision: Group 1 decided on restaurant 1.",
            lines[2]
        )
        assertEquals(
            "[DEBUG] Restaurant No Decision: Group 2 could not decide for a restaurant.",
            lines[3]
        )

        stringWriter.buffer.setLength(0)

        // =====================================================================
        // STEP 4: RESTAURANT 1 SIMULATION - TICK 1 ACTION LOOP
        // =====================================================================
        TickStatusLogger.logRestaurantStart()

        FohReceptionLogger.logRestaurantArrival(groupId = 1)
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

        FohReceptionLogger.logFohNoSeatingNoWaitstaff(groupId = 3)
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

        lines = getLoggedLines()

        assertEquals(10, lines.size)
        assertEquals(
            "[DEBUG] Restaurant Start (R 1): Restaurant 1 simulates a tick.",
            lines[0]
        )
        assertEquals(
            "[INFO] Restaurant Arrival (R 1): Group 1 arrived at restaurant 1.",
            lines[1]
        )
        assertEquals(
            "[INFO] FOH Merging Tables (R 1): For group 1 the tables 3,4,5 were merged into 3.",
            lines[2]
        )
        assertEquals(
            "[IMPORTANT] FOH Seating (R 1): Group 1 seated at table 3 by waitstaff 1,2.",
            lines[3]
        )
        assertEquals(
            "[INFO] FOH No Seating (R 1): No free waitstaff available for group 3.",
            lines[4]
        )
        assertEquals(
            "[INFO] FOH No Seating (R 1): Assigned waitstaff 2 but no table available, " +
                "group 4 is sent away.",
            lines[5]
        )
        assertEquals(
            "[IMPORTANT] FOH Ordering (R 1): Group 1 placed order 10 of chicken rice:2," +
                "potato soup:1 with waitstaff 1.",
            lines[6]
        )
        assertEquals(
            "[IMPORTANT] FOH No Ordering (R 1): Group 1 could not place an order for 2 " +
                "customers, they leave the restaurant.",
            lines[7]
        )
        assertEquals(
            "[DEBUG] FOH Seating Status (R 1): 2 waitstaff seated 8 customers on 1 tables.",
            lines[8]
        )
        assertEquals(
            "[DEBUG] FOH Ordering Status (R 1): The restaurant received orders from 3 " +
                "customers, 1 waitstaff took orders.",
            lines[9]
        )

        stringWriter.buffer.setLength(0)

        // =====================================================================
        // STEP 5: KITCHEN SCHEDULING & COOKING
        // =====================================================================
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

        lines = getLoggedLines()

        assertEquals(3, lines.size)
        assertEquals(
            "[IMPORTANT] Kitchen Dish Assignment (R 1): Cook 1 of type TOURNANT starts " +
                "cooking 2 meals of dish chicken rice based on order 10 for orders 10,11.",
            lines[0]
        )
        assertEquals(
            "[IMPORTANT] Kitchen Meal Cooked (R 1): Cook 1 finished cooking 2 meals of " +
                "dish chicken rice 2 ticks after ordering.",
            lines[1]
        )
        assertEquals(
            "[DEBUG] Kitchen Status (R 1): 1 cooks were active cooking 2 and finishing 2 " +
                "meals. 2 meals can be served by the waitstaff.",
            lines[2]
        )

        stringWriter.buffer.setLength(0)

        // =====================================================================
        // STEP 6: SERVING & DELIVERY DRIVING
        // =====================================================================
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
        DeliveryLogger.logDeliveryReturned(driverId = 1)
        DeliveryLogger.logDeliveryFinishedEating(groupId = 2)

        lines = getLoggedLines()

        assertEquals(12, lines.size)
        assertEquals(
            "[IMPORTANT] FOH Serving (R 1): Waitstaff 1 serves chicken rice:2 to table 3 " +
                "1 ticks after ordering.",
            lines[0]
        )
        assertEquals(
            "[DEBUG] FOH No Serving (R 1): Waitstaff 1 did not serve 1 meals to table 3.",
            lines[1]
        )
        assertEquals(
            "[IMPORTANT] FOH Delivery (R 1): Waitstaff 1 serves potato soup:1 meals to " +
                "driver 1 for order 12.",
            lines[2]
        )
        assertEquals(
            "[DEBUG] FOH Serving Status (R 1): 1 waitstaff served 3 meals.",
            lines[3]
        )
        assertEquals(
            "[INFO] Delivery Preparation (R 1): Driver 1 prepares driving order 12 to " +
                "group 2, which will take 3 ticks.",
            lines[4]
        )
        assertEquals(
            "[DEBUG] Delivery Driving (R 1): Driver 1 drove 5 km and needs 2 more ticks.",
            lines[5]
        )
        assertEquals(
            "[INFO] Delivery Arrival (R 1): Driver 1 arrived at group 2 with order 12.",
            lines[6]
        )
        assertEquals(
            "[IMPORTANT] Delivery Finished (R 1): Driver 1 gave delivery of order 12 " +
                "to group 2.",
            lines[7]
        )
        assertEquals(
            "[IMPORTANT] Delivery Failed (R 1): Driver 1 failed to deliver order 13 " +
                "to group 3.",
            lines[8]
        )
        assertEquals(
            "[INFO] Delivery Given Up (R 1): Group 3 gave up on waiting for delivery of " +
                "order 13.",
            lines[9]
        )
        assertEquals(
            "[INFO] Delivery Returned (R 1): Driver 1 has returned.",
            lines[10]
        )
        assertEquals(
            "[INFO] Delivery Finished Eating (R 1): Group 2 has finished eating.",
            lines[11]
        )

        stringWriter.buffer.setLength(0)

        // =====================================================================
        // STEP 7: CUSTOMER EATING, ESCORTING & RATINGS
        // =====================================================================
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

        lines = getLoggedLines()

        assertEquals(8, lines.size)
        assertEquals(
            "[INFO] Restaurant No Eating (R 1): 2 customers of group 1 leave table 3 " +
                "due to not being served.",
            lines[0]
        )
        assertEquals(
            "[INFO] FOH Finished Eating (R 1): 6 customers of group 1 have finished " +
                "eating at table 3.",
            lines[1]
        )
        assertEquals(
            "[DEBUG] FOH Eating Status (R 1): 0 customers are eating and 6 customers " +
                "have finished eating this tick.",
            lines[2]
        )
        assertEquals(
            "[IMPORTANT] FOH Escorting (R 1): Waitstaff 1 escorts 6 customers of group " +
                "1 from table 3 outside.",
            lines[3]
        )
        assertEquals(
            "[DEBUG] FOH Escorting Status (R 1): 1 waitstaff escorted 6 customers this tick.",
            lines[4]
        )
        assertEquals(
            "[INFO] Rating (R 1): Group 1 rates the restaurant 1 with 5 rating, " +
                "leading to 12 positive ratings and 5 negative ratings.",
            lines[5]
        )
        assertEquals(
            "[DEBUG] Rating Status (R 1): 1 groups performed ratings this tick.",
            lines[6]
        )
        assertEquals(
            "[DEBUG] Restaurant End (R 1): Restaurant 1 finished simulating the tick.",
            lines[7]
        )

        stringWriter.buffer.setLength(0)

        // =====================================================================
        // STEP 8: SIMULATION END & STATISTICS LOGGER
        // =====================================================================
        TickStatusLogger.logServingEnd()

        StatisticsLogger.logSimulationStatsCalculated()
        StatisticsLogger.logSimulationStatsCooked(numberOfCookedMeals = 120)
        StatisticsLogger.logSimulationStatsServed(numberOfCustomersServed = 98)
        StatisticsLogger.logSimulationStatsDelivered(
            numberOfCustomersDelivered = 22
        )
        StatisticsLogger.logSimulationStatsRatingsGiven(
            numberOfCustomersGivingRatings = 15
        )

        lines = getLoggedLines()

        assertEquals(6, lines.size)
        assertEquals(
            "[IMPORTANT] Serving: Serving of evening 1 ends.",
            lines[0]
        )
        assertEquals(
            "[IMPORTANT] Simulation Info: Simulation statistics are calculated.",
            lines[1]
        )
        assertEquals(
            "[IMPORTANT] Simulation Statistics: Restaurant 1 cooked 120 meals.",
            lines[2]
        )
        assertEquals(
            "[IMPORTANT] Simulation Statistics: Restaurant 1 served 98 customers.",
            lines[3]
        )
        assertEquals(
            "[IMPORTANT] Simulation Statistics: Restaurant 1 delivered meals to 22 customers.",
            lines[4]
        )
        assertEquals(
            "[IMPORTANT] Simulation Statistics: Restaurant 1 received 15 ratings.",
            lines[5]
        )
    }
}
