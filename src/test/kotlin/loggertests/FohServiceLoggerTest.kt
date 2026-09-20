package loggertests

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger
import de.unisaarland.cs.se.selab.loggers.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

class FohServiceLoggerTest {
    private lateinit var output: StringWriter
    private val restaurantId: Id = 1
    private val waitstaffId: Id = 2
    private val driverId: Id = 3
    private val groupId: Id = 4
    private val orderId: Id = 5
    private val tableId: Id = 6
    private val orderDurationTick: Tick = 7
    private val waitstaffNumber: Int = 8
    private val customerNumber: Int = 9
    private val mealNumber: Int = 10
    private val numberOfEatingCustomers: Int = 11
    private val numberOfFinishedCustomers: Int = 12
    private val positiveGroupRating: RatingType = RatingType.POSITIVE
    private val negativeGroupRating: RatingType = RatingType.NEGATIVE
    private val positiveRatingQuantity: Int = 13
    private val negativeRatingQuantity: Int = 14
    private val dishNameToAmount: Map<String, Int> = mapOf("chicken rice" to 15)
    private val dishNamesToAmounts: Map<String, Int> =
        mapOf(
            "chicken rice" to 16,
            "beef pasta" to 2
        )

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = restaurantId
    }

    private val loggedLine: String get() = output.toString().lines().single { it.isNotBlank() }
    private fun Map<String, Int>.toSortedString() =
        this.toList().sortedBy { it.first }.joinToString(separator = ",") { "${it.first}:${it.second}" }

    @Test
    fun `IMPORTANT - FOH Serving(waitstaffId, dishNameToAmount, tableId, orderDurationTick) - dish`() {
        FohServiceLogger.logFohServing(waitstaffId, dishNameToAmount, tableId, orderDurationTick)
        assertEquals(
            "[IMPORTANT] FOH Serving (R $restaurantId): Waitstaff $waitstaffId " +
                "serves ${dishNameToAmount.toSortedString()} to table $tableId $orderDurationTick ticks " +
                "after ordering.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Serving(waitstaffId, dishNamesToAmounts, tableId, orderDurationTick) - dishes`() {
        FohServiceLogger.logFohServing(waitstaffId, dishNamesToAmounts, tableId, orderDurationTick)
        assertEquals(
            "[IMPORTANT] FOH Serving (R $restaurantId): Waitstaff $waitstaffId " +
                "serves ${dishNamesToAmounts.toSortedString()} to table $tableId $orderDurationTick ticks " +
                "after ordering.",
            loggedLine
        )
    }

    @Test
    fun `DEBUG - FOH No Serving(waitstaffId, mealNumber, tableId)`() {
        FohServiceLogger.logFohNoServing(waitstaffId, mealNumber, tableId)
        assertEquals(
            "[DEBUG] FOH No Serving (R $restaurantId): Waitstaff $waitstaffId did " +
                "not serve $mealNumber meals to table $tableId.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Delivery(waitstaffId, dishNameToAmount, driverId, orderId) - dish`() {
        FohServiceLogger.logFohDelivery(waitstaffId, dishNameToAmount, driverId, orderId)
        assertEquals(
            "[IMPORTANT] FOH Delivery (R $restaurantId): Waitstaff $waitstaffId " +
                "serves ${dishNameToAmount.toSortedString()} meals to driver $driverId for order $orderId.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Delivery(waitstaffId, dishNameToAmount, driverId, orderId) - dishes`() {
        FohServiceLogger.logFohDelivery(waitstaffId, dishNamesToAmounts, driverId, orderId)
        assertEquals(
            "[IMPORTANT] FOH Delivery (R $restaurantId): Waitstaff $waitstaffId " +
                "serves ${dishNamesToAmounts.toSortedString()} meals to driver $driverId for order $orderId.",
            loggedLine
        )
    }

    @Test
    fun `DEBUG - FOH Serving Status(waitstaffNumber, mealNumber)`() {
        FohServiceLogger.logFohServingStatus(waitstaffNumber, mealNumber)
        assertEquals(
            "[DEBUG] FOH Serving Status (R $restaurantId): $waitstaffNumber " +
                "waitstaff served $mealNumber meals.",
            loggedLine
        )
    }

    @Test
    fun `INFO - Restaurant No Eating(customerNumber, groupId, tableId)`() {
        FohServiceLogger.logRestaurantNoEating(customerNumber, groupId, tableId)
        assertEquals(
            "[INFO] Restaurant No Eating (R $restaurantId): $customerNumber " +
                "customers of group $groupId leave table $tableId due to not being served.",
            loggedLine
        )
    }

    @Test
    fun `INFO - FOH Finished Eating(customerNumber, groupId, tableId)`() {
        FohServiceLogger.logFohFinishedEating(customerNumber, groupId, tableId)
        assertEquals(
            "[INFO] FOH Finished Eating (R $restaurantId): $customerNumber " +
                "customers of group $groupId have finished eating at table $tableId.",
            loggedLine
        )
    }

    @Test
    fun `DEBUG - FOH Eating Status(numberOfEatingCustomers, numberOfFinishedCustomers)`() {
        FohServiceLogger.logFohEatingStatus(numberOfEatingCustomers, numberOfFinishedCustomers)
        assertEquals(
            "[DEBUG] FOH Eating Status (R $restaurantId): $numberOfEatingCustomers " +
                "customers are eating and $numberOfFinishedCustomers customers have finished eating this tick.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Escorting(waitstaffId, customerNumber, groupId, tableId)`() {
        FohServiceLogger.logFohEscorting(waitstaffId, customerNumber, groupId, tableId)
        assertEquals(
            "[IMPORTANT] FOH Escorting (R $restaurantId): Waitstaff $waitstaffId " +
                "escorts $customerNumber customers of group $groupId from table $tableId outside.",
            loggedLine
        )
    }

    @Test
    fun `INFO - Rating(groupId, positiveGroupRating, positiveRatingQuantity, negativeRatingQuantity) - positive`() {
        FohServiceLogger.logCustomerRateRestaurant(
            groupId,
            positiveGroupRating,
            positiveRatingQuantity,
            negativeRatingQuantity
        )
        assertEquals(
            "[INFO] Rating (R $restaurantId): Group $groupId rates " +
                "the restaurant $restaurantId with POSITIVE rating, " +
                "leading to $positiveRatingQuantity positive ratings and " +
                "$negativeRatingQuantity negative ratings.",
            loggedLine
        )
    }

    @Test
    fun `INFO - Rating(groupId, negativeGroupRating, positiveRatingQuantity, negativeRatingQuantity) - negative`() {
        FohServiceLogger.logCustomerRateRestaurant(
            groupId,
            negativeGroupRating,
            positiveRatingQuantity,
            negativeRatingQuantity
        )
        assertEquals(
            "[INFO] Rating (R $restaurantId): Group $groupId rates " +
                "the restaurant $restaurantId with NEGATIVE rating, " +
                "leading to $positiveRatingQuantity positive ratings and " +
                "$negativeRatingQuantity negative ratings.",
            loggedLine
        )
    }

    @Test
    fun `DEBUG - Rating Status(customerNumber)`() {
        FohServiceLogger.logRatingStatus(customerNumber)
        assertEquals(
            "[DEBUG] Rating Status (R $restaurantId): $customerNumber groups " +
                "performed ratings this tick.",
            loggedLine
        )
    }

    @Test
    fun `DEBUG - FOH Escorting Status(waitstaffNumber, customerNumber)`() {
        FohServiceLogger.logFohEscortingStatus(waitstaffNumber, customerNumber)
        assertEquals(
            "[DEBUG] FOH Escorting Status (R $restaurantId): $waitstaffNumber " +
                "waitstaff escorted $customerNumber customers this tick.",
            loggedLine
        )
    }
}
