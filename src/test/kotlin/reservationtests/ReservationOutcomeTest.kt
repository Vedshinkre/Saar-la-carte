package reservationtests

import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.ArrivalProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the outcome of [ArrivalProcessor.reserveTables]: what happens to table state,
 * the group's experience, and the turned-away/failed-attempts bookkeeping once a REGULAR
 * or EVENT reservation succeeds or fails. Deliberately uses setups where at most one table
 * is a candidate, or where two tables sum exactly to the group size, so these tests do not
 * depend on the merge-selection algorithm itself (that belongs to F15).
 */
class ReservationOutcomeTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
    }

    private fun countertop(): Countertop {
        val stock = Stock(emptyList<Ingredient>())
        return Countertop(
            pantry = Pantry(stock),
            orderQueue = ArrayDeque(),
            cooks = emptyList(),
            restaurantType = RestaurantType.EUROPEAN
        )
    }

    private fun processor(
        tables: List<Table>,
        customerToTable: MutableMap<CustomerGroup, List<Table>> = mutableMapOf(),
        turnedAwayGroups: MutableList<CustomerGroup> = mutableListOf()
    ): ArrivalProcessor = ArrivalProcessor(
        tables = tables,
        waiters = emptyList(),
        customerToTable = customerToTable,
        inHouseGroupsToWaiter = mutableMapOf(),
        turnedAwayGroups = turnedAwayGroups,
        eventGroups = mutableListOf(),
        countertop = countertop(),
        recruitWaitersForEventGroup = { _, _ -> emptyList() },
        getNextWaiterId = { 0 }
    )

    private fun regularGroup(id: Int, size: Int, restaurantId: Int = 1): RegularGroup = RegularGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = emptyList(),
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = restaurantId
    )

    private fun eventGroup(id: Int, size: Int): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.ASIAN),
        eventEvening = 4,
        eventDishes = emptyMap()
    )

    // ---- Successful reservations ----

    @Test
    fun `a successful single table reservation marks the table RESERVED`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val processor = processor(listOf(table))

        val success = processor.reserveTables(regularGroup(id = 1, size = 4))

        assertTrue(success)
        assertEquals(TableStatus.RESERVED, table.status)
    }

    @Test
    fun `a successful single table reservation is recorded under the group in customerToTable`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val customerToTable = mutableMapOf<CustomerGroup, List<Table>>()
        val processor = processor(listOf(table), customerToTable = customerToTable)
        val group = regularGroup(id = 1, size = 4)

        processor.reserveTables(group)

        assertEquals(listOf(table), customerToTable[group])
    }

    @Test
    fun `a successful merged reservation marks every involved table RESERVED and records all of them`() {
        val tableOne = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val tableTwo = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val customerToTable = mutableMapOf<CustomerGroup, List<Table>>()
        val processor = processor(listOf(tableOne, tableTwo), customerToTable = customerToTable)
        val group = eventGroup(id = 1, size = 4)

        val success = processor.reserveTables(group)

        assertTrue(success)
        assertEquals(TableStatus.RESERVED, tableOne.status)
        assertEquals(TableStatus.RESERVED, tableTwo.status)
        assertEquals(setOf(tableOne, tableTwo), customerToTable[group]?.toSet())
    }

    // ---- Failed reservations ----

    @Test
    fun `a failed reservation for a REGULAR group turns them away and counts as a failed attempt`() {
        val turnedAwayGroups = mutableListOf<CustomerGroup>()
        val processor = processor(emptyList(), turnedAwayGroups = turnedAwayGroups)
        val group = regularGroup(id = 1, size = 4)

        val success = processor.reserveTables(group)

        assertFalse(success)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(turnedAwayGroups.contains(group))
        assertEquals(1, group.failedAttempts)
    }

    @Test
    fun `a failed reservation for an EVENT group sets a negative experience and turns the group away`() {
        val turnedAwayGroups = mutableListOf<CustomerGroup>()
        val processor = processor(emptyList(), turnedAwayGroups = turnedAwayGroups)
        val group = eventGroup(id = 1, size = 4)

        val success = processor.reserveTables(group)

        assertFalse(success)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(turnedAwayGroups.contains(group))
    }

    @Test
    fun `a failed reservation logs FOH No Reserving with the group's id`() {
        val processor = processor(emptyList())

        processor.reserveTables(regularGroup(id = 7, size = 4))

        assertTrue(output.toString().contains("FOH No Reserving (R 1): No table could be reserved for group 7."))
    }

    // ---- Reservation state persists to block later reservations ----

    @Test
    fun `a table already reserved by an earlier group cannot be reserved again with no fallback available`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val barTable = Table(id = 2, size = 4, tableType = TableType.BAR) // right size, wrong type
        val processor = processor(listOf(table, barTable))

        val firstReserved = processor.reserveTables(regularGroup(id = 1, size = 4))
        val secondReserved = processor.reserveTables(regularGroup(id = 2, size = 4))

        assertTrue(firstReserved)
        assertFalse(secondReserved)
        assertEquals(TableStatus.RESERVED, table.status)
        assertEquals(TableStatus.FREE, barTable.status, "a BAR table is no fallback for a COMMON group")
    }

    @Test
    fun `once the perfectly fitting table is taken a later group gets a larger table within the three quarter rule`() {
        val largerTable = Table(id = 1, size = 5, tableType = TableType.COMMON)
        val perfectFit = Table(id = 2, size = 4, tableType = TableType.COMMON)
        val customerToTable = mutableMapOf<CustomerGroup, List<Table>>()
        val processor = processor(listOf(largerTable, perfectFit), customerToTable = customerToTable)
        val firstGroup = regularGroup(id = 1, size = 4)
        val secondGroup = regularGroup(id = 2, size = 4)

        val firstReserved = processor.reserveTables(firstGroup)
        val secondReserved = processor.reserveTables(secondGroup)

        assertTrue(firstReserved)
        assertTrue(secondReserved)
        assertEquals(listOf(perfectFit), customerToTable[firstGroup], "a perfect fit wins over a lower table id")
        assertEquals(listOf(largerTable), customerToTable[secondGroup], "4 of 5 seats meets the three quarter rule")
    }
}
