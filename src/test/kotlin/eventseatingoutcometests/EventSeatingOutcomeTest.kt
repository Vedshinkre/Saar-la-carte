package eventseatingoutcometests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.ArrivalProcessor
import eventseatingoutcometests.EventSeatingFixtures.eventGroup
import eventseatingoutcometests.EventSeatingFixtures.menu
import eventseatingoutcometests.EventSeatingFixtures.reserve
import eventseatingoutcometests.EventSeatingFixtures.table
import eventseatingoutcometests.EventSeatingFixtures.waiter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val SEATING_LOG = "FOH Seating (R 1)"
private const val NO_WAITSTAFF_LOG = "FOH No Seating (R 1): No free waitstaff available for group"
private const val TOO_BIG_FOR_ONE_WAITER = 11

/**
 * Person B's unit tests for P02 (EVENT seating): what the seating attempt logs, how it counts in
 * the seating status, and what state a successful or failed attempt leaves behind. Which waiter
 * seats how many customers is Person A's half and is not asserted here beyond what a log needs.
 */
class EventSeatingOutcomeTest {

    private lateinit var output: StringWriter
    private val customerToTable: MutableMap<CustomerGroup, List<Table>> = mutableMapOf()
    private val turnedAway: MutableList<CustomerGroup> = mutableListOf()
    private val eventGroups: MutableList<EventGroup> = mutableListOf()

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
    }

    private fun processor(tables: List<Table>, waiters: List<Waiter>): ArrivalProcessor =
        EventSeatingFixtures.processor(tables, waiters, customerToTable, turnedAway, eventGroups)

    private fun log(): String = output.toString()

    // ---- Logs ----

    @Test
    fun `a successful seating logs the group, the table and the sorted waiter ids`() {
        val bigTable = table(1, 12)
        // the waiter with the higher current load seats first but has the higher id
        val first = waiter(id = 2, currentLoad = 5)
        val second = waiter(id = 1)
        val group = eventGroup(1, 12)
        reserve(customerToTable, group, bigTable)

        processor(listOf(bigTable), listOf(first, second)).processArrival(group, menu)

        assertTrue(log().contains("$SEATING_LOG: Group 1 seated at table 1 by waitstaff 1,2."))
    }

    @Test
    fun `a merged reservation logs FOH Merging Tables before FOH Seating`() {
        val tableOne = table(2, 2)
        val tableTwo = table(1, 2)
        val group = eventGroup(5, 4)
        reserve(customerToTable, group, tableOne, tableTwo)

        processor(listOf(tableOne, tableTwo), listOf(waiter())).processArrival(group, menu)

        val merging = log().indexOf("FOH Merging Tables")
        val seating = log().indexOf(SEATING_LOG)
        assertTrue(merging >= 0 && seating >= 0)
        assertTrue(merging < seating)
        assertTrue(log().contains("the tables 1,2 were merged into 1."))
    }

    @Test
    fun `a single table reservation logs no FOH Merging Tables`() {
        val singleTable = table(1, 4)
        val group = eventGroup(3, 4)
        reserve(customerToTable, group, singleTable)

        processor(listOf(singleTable), listOf(waiter())).processArrival(group, menu)

        assertFalse(log().contains("FOH Merging Tables"))
        assertTrue(log().contains("$SEATING_LOG: Group 3 seated at table 1"))
    }

    @Test
    fun `a failed seating logs only FOH No Seating, no seating and no ordering line`() {
        val bigTable = table(1, TOO_BIG_FOR_ONE_WAITER)
        val group = eventGroup(7, TOO_BIG_FOR_ONE_WAITER)
        reserve(customerToTable, group, bigTable)

        // a single waiter can only seat 10 of the 11 customers
        processor(listOf(bigTable), listOf(waiter())).processArrival(group, menu)

        assertTrue(log().contains("$NO_WAITSTAFF_LOG 7."))
        assertFalse(log().contains(SEATING_LOG))
        assertFalse(log().contains("FOH Ordering (R 1)"))
    }

    // ---- State left behind ----

    @Test
    fun `a failed seating turns the event group away and its reserved table stays reserved`() {
        val bigTable = table(1, TOO_BIG_FOR_ONE_WAITER)
        val group = eventGroup(7, TOO_BIG_FOR_ONE_WAITER)
        reserve(customerToTable, group, bigTable)

        processor(listOf(bigTable), listOf(waiter())).processArrival(group, menu)

        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(turnedAway.contains(group))
        assertFalse(eventGroups.contains(group))
        assertEquals(TableStatus.RESERVED, bigTable.status)
    }

    @Test
    fun `a successful seating puts the group into the seated event groups with an order placed`() {
        val singleTable = table(1, 4)
        val group = eventGroup(2, 4)
        reserve(customerToTable, group, singleTable)

        val removeFromQueue = processor(listOf(singleTable), listOf(waiter())).processArrival(group, menu)

        assertTrue(removeFromQueue)
        assertTrue(eventGroups.contains(group))
        assertNotNull(group.currentOrder)
        assertFalse(turnedAway.contains(group))
        assertEquals(ExperienceType.NEUTRAL, group.experience)
    }

    @Test
    fun `an event group without a reserved table does not crash the seating and logs no seating line`() {
        // Not expected in a real run (a failed reservation keeps the group out of the queue), but
        // successfulSeating() quietly returns without a table, so this pins that it does not throw.
        val group = eventGroup(4, 4)

        processor(emptyList(), listOf(waiter())).processArrival(group, menu)

        assertFalse(log().contains(SEATING_LOG))
    }

    // ---- Seating status counters ----

    @Test
    fun `the seating status counts every waiter, the customers and the table of a successful event`() {
        val bigTable = table(1, 12)
        val group = eventGroup(1, 12)
        reserve(customerToTable, group, bigTable)
        val processor = processor(listOf(bigTable), listOf(waiter(), waiter()))

        processor.processArrival(group, menu)
        processor.logAndResetSeatingOrderingTickStatus()

        assertTrue(log().contains("FOH Seating Status (R 1): 2 waitstaff seated 12 customers on 1 tables."))
    }

    @Test
    fun `a merged table of an event counts only once in the seating status`() {
        val tableOne = table(1, 2)
        val tableTwo = table(2, 2)
        val group = eventGroup(1, 4)
        reserve(customerToTable, group, tableOne, tableTwo)
        val processor = processor(listOf(tableOne, tableTwo), listOf(waiter()))

        processor.processArrival(group, menu)
        processor.logAndResetSeatingOrderingTickStatus()

        assertTrue(log().contains("FOH Seating Status (R 1): 1 waitstaff seated 4 customers on 1 tables."))
    }

    @Test
    fun `a failed event seating adds nothing to the seating status`() {
        val bigTable = table(1, TOO_BIG_FOR_ONE_WAITER)
        val group = eventGroup(1, TOO_BIG_FOR_ONE_WAITER)
        reserve(customerToTable, group, bigTable)
        val processor = processor(listOf(bigTable), listOf(waiter()))

        processor.processArrival(group, menu)
        processor.logAndResetSeatingOrderingTickStatus()

        assertTrue(log().contains("FOH Seating Status (R 1): 0 waitstaff seated 0 customers on 0 tables."))
    }
}
