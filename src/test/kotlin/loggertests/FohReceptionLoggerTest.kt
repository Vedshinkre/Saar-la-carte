package loggertests

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.FohReceptionLogger
import de.unisaarland.cs.se.selab.loggers.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.collections.sorted
import kotlin.test.assertEquals

class FohReceptionLoggerTest {
    private lateinit var output: StringWriter
    private val restaurantId: Id = 1
    private val groupId: Id = 2
    private val orderId: Id = 3
    private val tableId: Id = 4
    private val tableIds: List<Id> = listOf(5, 4)
    private val waitstaffId: List<Id> = listOf(6)
    private val waitstaffIds: List<Id> = listOf(8, 7)
    private val dishNameToAmount: Map<String, Int> = mapOf("chicken rice" to 9)
    private val dishNamesToAmounts: Map<String, Int> =
        mapOf(
            "chicken rice" to 10,
            "beef pasta" to 1
        )
    private val customerNumber: Int = 2
    private val waitstaffNumber: Int = 3
    private val tableNumber: Int = 4

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = restaurantId
    }

    private val loggedLine: String get() = output.toString().lines().single { it.isNotBlank() }
    private fun List<Id>.toSortedString() = this.sorted().joinToString(separator = ",")
    private fun Map<String, Int>.toSortedString() =
        this.toList().sortedBy { it.first }.joinToString(separator = ",") { "${it.first}:${it.second}" }

    @Test
    fun `INFO - Restaurant Arrival(groupId)`() {
        FohReceptionLogger.logRestaurantArrival(groupId)
        assertEquals(
            "[INFO] Restaurant Arrival (R $restaurantId): Group $groupId arrived " +
                "at restaurant $restaurantId.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Seating(tableId, waitstaffId) - waiter`() {
        FohReceptionLogger.logFohSeating(groupId, tableId, waitstaffId)
        assertEquals(
            "[IMPORTANT] FOH Seating (R $restaurantId): Group $groupId seated at " +
                "table $tableId by waitstaff ${waitstaffId.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Seating(tableId, waitstaffIds) - waiters`() {
        FohReceptionLogger.logFohSeating(groupId, tableId, waitstaffIds)
        assertEquals(
            "[IMPORTANT] FOH Seating (R $restaurantId): Group $groupId seated at " +
                "table $tableId by waitstaff ${waitstaffIds.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `INFO - FOH Merging Tables(groupId, tableIds, tableId)`() {
        FohReceptionLogger.logFohMergingTables(groupId, tableIds, tableId)
        assertEquals(
            "[INFO] FOH Merging Tables (R $restaurantId): For group $groupId the " +
                "tables ${tableIds.toSortedString()} were merged into $tableId.",
            loggedLine
        )
    }

    @Test
    fun `INFO - FOH No Seating(groupId)`() {
        FohReceptionLogger.logFohNoSeatingNoWaitstaff(groupId)
        assertEquals(
            "[INFO] FOH No Seating (R $restaurantId): No free waitstaff available for group $groupId.",
            loggedLine
        )
    }

    @Test
    fun `INFO - FOH No Seating(groupId, waitstaffId)`() {
        FohReceptionLogger.logFohNoSeating(groupId, waitstaffId.single())
        assertEquals(
            "[INFO] FOH No Seating (R $restaurantId): Assigned waitstaff " +
                "${waitstaffId.toSortedString()} but no table available, group $groupId is sent away.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Ordering(groupId, orderId, dishNameToAmount, waitstaffId) - dish, waiter`() {
        FohReceptionLogger.logFohOrdering(groupId, orderId, dishNameToAmount, waitstaffId)
        assertEquals(
            "[IMPORTANT] FOH Ordering (R $restaurantId): Group $groupId placed " +
                "order $orderId of ${dishNameToAmount.toSortedString()} " +
                "with waitstaff ${waitstaffId.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Ordering(groupId, orderId, dishNamesToAmounts, waitstaffId) - dishes, waiter`() {
        FohReceptionLogger.logFohOrdering(groupId, orderId, dishNamesToAmounts, waitstaffId)
        assertEquals(
            "[IMPORTANT] FOH Ordering (R $restaurantId): Group $groupId placed " +
                "order $orderId of ${dishNamesToAmounts.toSortedString()} " +
                "with waitstaff ${waitstaffId.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Ordering(groupId, orderId, dishNameToAmount, waitstaffIds) - dish, waiters`() {
        FohReceptionLogger.logFohOrdering(groupId, orderId, dishNameToAmount, waitstaffIds)
        assertEquals(
            "[IMPORTANT] FOH Ordering (R $restaurantId): Group $groupId placed " +
                "order $orderId of ${dishNameToAmount.toSortedString()} " +
                "with waitstaff ${waitstaffIds.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Ordering(groupId, orderId, dishNamesToAmounts, waitstaffIds) - dishes, waiters`() {
        FohReceptionLogger.logFohOrdering(groupId, orderId, dishNamesToAmounts, waitstaffIds)
        assertEquals(
            "[IMPORTANT] FOH Ordering (R $restaurantId): Group $groupId placed " +
                "order $orderId of ${dishNamesToAmounts.toSortedString()} " +
                "with waitstaff ${waitstaffIds.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Ordering(groupId, orderId, dishNameToAmount, null) - dish, null-waiter`() {
        FohReceptionLogger.logFohOrdering(groupId, orderId, dishNameToAmount, null)
        assertEquals(
            "[IMPORTANT] FOH Ordering (R $restaurantId): Group $groupId placed " +
                "order $orderId of ${dishNameToAmount.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH Ordering(groupId, orderId, dishNamesToAmounts, null) - dishes, null-waiter`() {
        FohReceptionLogger.logFohOrdering(groupId, orderId, dishNamesToAmounts, null)
        assertEquals(
            "[IMPORTANT] FOH Ordering (R $restaurantId): Group $groupId placed " +
                "order $orderId of ${dishNamesToAmounts.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - FOH No Ordering(groupId, customerNumber)`() {
        FohReceptionLogger.logFohNoOrdering(groupId, customerNumber)
        assertEquals(
            "[IMPORTANT] FOH No Ordering (R $restaurantId): Group $groupId could " +
                "not place an order for $customerNumber customers, they leave the restaurant.",
            loggedLine
        )
    }

    @Test
    fun `DEBUG - FOH Seating Status(waitstaffNumber, customerNumber, tableNumber)`() {
        FohReceptionLogger.logSeatingStatus(waitstaffNumber, customerNumber, tableNumber)
        assertEquals(
            "[DEBUG] FOH Seating Status (R $restaurantId): $waitstaffNumber " +
                "waitstaff seated $customerNumber customers on $tableNumber tables.",
            loggedLine
        )
    }

    @Test
    fun `DEBUG - FOH Ordering Status(customerNumber, waitstaffNumber)`() {
        FohReceptionLogger.logOrderingStatus(customerNumber, waitstaffNumber)
        assertEquals(
            "[DEBUG] FOH Ordering Status (R $restaurantId): The restaurant received " +
                "orders from $customerNumber customers, $waitstaffNumber waitstaff took orders.",
            loggedLine
        )
    }
}
