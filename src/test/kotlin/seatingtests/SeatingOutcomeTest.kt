package seatingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
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
 * Unit tests for the seating half of F16 (arrival, waitstaff hand-off, seating outcome),
 * exercised through [ArrivalProcessor.processArrival] with an empty menu. Since ordering
 * always fails with no menu (F18 territory, tested separately), these tests only rely on
 * state that seating itself sets and that a subsequent failed order does not undo: waiter
 * tick load/currentLoad, the seating logs, and the seating-status counters. Cases that need
 * a successfully placed order to observe cleanly (e.g. a CASUAL group's ad-hoc table turning
 * OCCUPIED) are covered by the integration tests instead.
 */
class SeatingOutcomeTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
    }

    private fun countertop(): Countertop {
        val stock = Stock(emptyList<Ingredient>())
        return Countertop(pantry = Pantry(stock), orderQueue = ArrayDeque(), cooks = emptyList())
    }

    private fun processor(
        tables: List<Table>,
        waiters: List<Waiter>,
        customerToTable: MutableMap<CustomerGroup, List<Table>> = mutableMapOf(),
        turnedAwayGroups: MutableList<CustomerGroup> = mutableListOf()
    ): ArrivalProcessor = ArrivalProcessor(
        tables = tables,
        waiters = waiters,
        customerToTable = customerToTable,
        inHouseGroupsToWaiter = mutableMapOf(),
        turnedAwayGroups = turnedAwayGroups,
        eventGroups = mutableListOf(),
        countertop = countertop(),
        recruitWaitersForEventGroup = { _, _ -> emptyList() },
        getNextWaiterId = { 0 }
    )

    private fun regularGroup(id: Int, size: Int, visitingAt: Int = 1): RegularGroup = RegularGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = emptyList(),
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1
    )

    private fun freeWaiter(): Waiter = Waiter()

    // ---- No free waiter ----

    @Test
    fun `no free waiter on the visiting tick keeps the group queued for a retry`() {
        val processor = processor(tables = emptyList(), waiters = emptyList())
        val group = regularGroup(id = 1, size = 4, visitingAt = 1)
        Time.tick = 1

        val removeFromQueue = processor.processArrival(group, menu = emptyList())

        assertFalse(removeFromQueue)
        assertEquals(ExperienceType.NEUTRAL, group.experience)
        assertEquals(0, group.failedAttempts)
    }

    @Test
    fun `no free waiter after the retry tick turns the REGULAR group away and counts a failed attempt`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        table.status = TableStatus.RESERVED
        val group = regularGroup(id = 1, size = 4, visitingAt = 1)
        val customerToTable = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(table))
        val turnedAwayGroups = mutableListOf<CustomerGroup>()
        val processor = processor(
            tables = listOf(table),
            waiters = emptyList(),
            customerToTable = customerToTable,
            turnedAwayGroups = turnedAwayGroups
        )
        Time.tick = 2

        val removeFromQueue = processor.processArrival(group, menu = emptyList())

        assertTrue(removeFromQueue)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(turnedAwayGroups.contains(group))
        assertEquals(1, group.failedAttempts)
        assertFalse(customerToTable.containsKey(group))
    }

    @Test
    fun `logFohNoSeatingNoWaitstaff fires with the group's id when no waiter is free`() {
        val processor = processor(tables = emptyList(), waiters = emptyList())
        val group = regularGroup(id = 9, size = 4, visitingAt = 1)
        Time.tick = 1

        processor.processArrival(group, menu = emptyList())

        assertTrue(output.toString().contains("FOH No Seating (R 1): No free waitstaff available for group 9."))
    }

    // ---- Successful seating ----

    @Test
    fun `successful seating increases the waiter's SEAT tick load by the group size`() {
        // currentLoad is deliberately not asserted here: processArrival credits it during
        // seating but then debits it again for customers who didn't end up with an order
        // (customerGroup.size - customersRemainingInRestaurant), which is F18 territory since
        // this test intentionally gives no menu. SEAT tick load is unaffected by that and is
        // purely a seating-time concern.
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        table.status = TableStatus.RESERVED
        val waiter = freeWaiter()
        val group = regularGroup(id = 1, size = 4)
        val customerToTable = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(table))
        val processor = processor(listOf(table), listOf(waiter), customerToTable)

        processor.processArrival(group, menu = emptyList())

        assertEquals(4, waiter.getTickLoad(ActionType.SEAT))
    }

    @Test
    fun `a single table reservation does not log FOH Merging Tables, only FOH Seating`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        table.status = TableStatus.RESERVED
        val group = regularGroup(id = 3, size = 4)
        val customerToTable = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(table))
        val processor = processor(listOf(table), listOf(freeWaiter()), customerToTable)

        processor.processArrival(group, menu = emptyList())

        val log = output.toString()
        assertFalse(log.contains("FOH Merging Tables"))
        assertTrue(log.contains("FOH Seating (R 1): Group 3 seated at table 1"))
    }

    @Test
    fun `a merged reservation logs FOH Merging Tables before FOH Seating`() {
        val tableOne = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val tableTwo = Table(id = 1, size = 2, tableType = TableType.COMMON)
        tableOne.status = TableStatus.RESERVED
        tableTwo.status = TableStatus.RESERVED
        val group = regularGroup(id = 5, size = 4)
        val customerToTable = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(tableOne, tableTwo))
        val processor = processor(listOf(tableOne, tableTwo), listOf(freeWaiter()), customerToTable)

        processor.processArrival(group, menu = emptyList())

        val log = output.toString()
        val mergingIndex = log.indexOf("FOH Merging Tables")
        val seatingIndex = log.indexOf("FOH Seating")
        assertTrue(mergingIndex >= 0 && seatingIndex >= 0)
        assertTrue(mergingIndex < seatingIndex)
        assertTrue(log.contains("the tables 1,2 were merged into 1."))
    }

    // ---- Seating-status counters ----

    @Test
    fun `seating counters accumulate across groups seated in the same tick then reset`() {
        // groupOne fills waiter one to the action limit (10), so groupTwo is forced onto a
        // second, distinct waiter rather than the load-balancer reusing the same one.
        val tableOne = Table(id = 1, size = 10, tableType = TableType.COMMON)
        val tableTwo = Table(id = 2, size = 3, tableType = TableType.COMMON)
        tableOne.status = TableStatus.RESERVED
        tableTwo.status = TableStatus.RESERVED
        val groupOne = regularGroup(id = 1, size = 10)
        val groupTwo = regularGroup(id = 2, size = 3)
        val customerToTable = mutableMapOf<CustomerGroup, List<Table>>(
            groupOne to listOf(tableOne),
            groupTwo to listOf(tableTwo)
        )
        val tableThree = Table(id = 3, size = 2, tableType = TableType.COMMON)
        tableThree.status = TableStatus.RESERVED
        val groupThree = regularGroup(id = 3, size = 2)
        customerToTable[groupThree] = listOf(tableThree)
        val processor = processor(
            listOf(tableOne, tableTwo, tableThree),
            listOf(freeWaiter(), freeWaiter()),
            customerToTable
        )

        processor.processArrival(groupOne, menu = emptyList())
        processor.processArrival(groupTwo, menu = emptyList())
        processor.logAndResetSeatingOrderingTickStatus()

        assertTrue(output.toString().contains("FOH Seating Status (R 1): 2 waitstaff seated 13 customers on 2 tables."))

        // A third group seated after the reset (same processor instance) should only
        // contribute its own numbers, proving the counters were actually zeroed.
        processor.processArrival(groupThree, menu = emptyList())
        processor.logAndResetSeatingOrderingTickStatus()

        assertTrue(output.toString().contains("FOH Seating Status (R 1): 1 waitstaff seated 2 customers on 1 tables."))
    }
}
