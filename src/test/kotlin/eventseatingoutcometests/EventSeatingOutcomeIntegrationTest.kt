package eventseatingoutcometests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import eventseatingoutcometests.EventSeatingFixtures.casualGroup
import eventseatingoutcometests.EventSeatingFixtures.eventGroup
import eventseatingoutcometests.EventSeatingFixtures.frontOfHouse
import eventseatingoutcometests.EventSeatingFixtures.menu
import eventseatingoutcometests.EventSeatingFixtures.regularGroup
import eventseatingoutcometests.EventSeatingFixtures.table
import eventseatingoutcometests.EventSeatingFixtures.waiter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val EVENT_SIZE = 12
private const val LAST_TICK = 24
private const val REGULAR_SIZE = 4

/**
 * Person B's integration tests for P02: EVENT seating through the real [FrontOfHouse] (its real
 * waiter recruiting) next to REGULAR and CASUAL groups in the same tick.
 */
class EventSeatingOutcomeIntegrationTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
    }

    /** The real Restaurant comparator (REGULAR, EVENT, CASUAL, then ascending id). */
    private fun arrivalOrder(): Comparator<CustomerGroup> {
        val stats = RestaurantStats(1, RestaurantType.EUROPEAN, 1, LAST_TICK, true, 0, 0, menu)
        val staff = RestaurantStaff(mutableListOf(), mutableListOf(), mutableListOf())
        return Restaurant(stats, "restaurant", staff, emptyList(), Stock(emptyList())).arrivalOrder
    }

    private fun arrive(foh: FrontOfHouse, vararg groups: CustomerGroup): Map<CustomerGroup, Boolean> =
        groups.toList().sortedWith(arrivalOrder()).associateWith { foh.processArrival(it, menu) }

    @Test
    fun `groups of a tick are seated as REGULAR then EVENT then CASUAL whatever their ids`() {
        val tables = listOf(table(1, REGULAR_SIZE), table(2, REGULAR_SIZE), table(3, REGULAR_SIZE))
        val foh = frontOfHouse(tables, listOf(waiter(), waiter(), waiter()))
        val casual = casualGroup(1, REGULAR_SIZE)
        val event = eventGroup(2, REGULAR_SIZE)
        val regular = regularGroup(3, REGULAR_SIZE)
        foh.reserveTables(event)
        foh.reserveTables(regular)

        arrive(foh, casual, event, regular)

        val log = output.toString()
        val regularAt = log.indexOf("FOH Seating (R 1): Group 3 ")
        val eventAt = log.indexOf("FOH Seating (R 1): Group 2 ")
        val casualAt = log.indexOf("FOH Seating (R 1): Group 1 ")
        assertTrue(regularAt in 0 until eventAt)
        assertTrue(eventAt < casualAt)
    }

    @Test
    fun `a REGULAR group seated first can leave too little SEATING capacity for a later EVENT`() {
        val foh = frontOfHouse(listOf(table(1, 6), table(2, 6)), listOf(waiter()))
        val regular = regularGroup(1, 6)
        val event = eventGroup(2, 6)
        foh.reserveTables(regular)
        foh.reserveTables(event)

        arrive(foh, regular, event)

        val log = output.toString()
        assertTrue(log.contains("FOH Seating (R 1): Group 1 "))
        assertTrue(log.contains("FOH No Seating (R 1): No free waitstaff available for group 2."))
        assertEquals(ExperienceType.NEGATIVE, event.experience)
    }

    @Test
    fun `a CASUAL group waits when the EVENT before it used up all SEATING capacity`() {
        val foh = frontOfHouse(listOf(table(1, Constants.ACTION_LIMIT), table(2, REGULAR_SIZE)), listOf(waiter()))
        val event = eventGroup(1, Constants.ACTION_LIMIT)
        val casual = casualGroup(2, REGULAR_SIZE)
        foh.reserveTables(event)

        val processed = arrive(foh, event, casual)

        assertEquals(true, processed[event])
        assertEquals(false, processed[casual], "no waiter left, so the casual group stays queued")
        assertFalse(output.toString().contains("FOH Seating (R 1): Group 2 "))
    }

    @Test
    fun `the tick's seating status adds regular and event seating together and then resets`() {
        val foh = frontOfHouse(listOf(table(1, REGULAR_SIZE), table(2, EVENT_SIZE)), listOf(waiter(), waiter()))
        val regular = regularGroup(1, REGULAR_SIZE)
        val event = eventGroup(2, EVENT_SIZE)
        foh.reserveTables(regular)
        foh.reserveTables(event)

        arrive(foh, regular, event)
        foh.logAndResetSeatingOrderingTickStatus()
        foh.logAndResetSeatingOrderingTickStatus()

        val log = output.toString()
        assertTrue(log.contains("FOH Seating Status (R 1): 2 waitstaff seated 16 customers on 2 tables."))
        assertTrue(log.contains("FOH Seating Status (R 1): 0 waitstaff seated 0 customers on 0 tables."))
    }

    @Test
    fun `a failed EVENT keeps its reserved table blocked for a CASUAL group later that evening`() {
        val reservedTable = table(1, EVENT_SIZE)
        val busyWaiter = waiter().also { it.addToTickLoad(ActionType.SEAT, Constants.ACTION_LIMIT) }
        val foh = frontOfHouse(listOf(reservedTable), listOf(busyWaiter))
        val event = eventGroup(1, EVENT_SIZE)
        foh.reserveTables(event)
        foh.processArrival(event, menu)
        foh.clearActionLoads()
        Time.tick = 2
        val casual = casualGroup(2, REGULAR_SIZE)

        val removeFromQueue = foh.processArrival(casual, menu)

        assertEquals(ExperienceType.NEGATIVE, event.experience)
        assertEquals(TableStatus.RESERVED, reservedTable.status)
        assertTrue(removeFromQueue, "the casual group finds no table and leaves")
        assertEquals(ExperienceType.NEGATIVE, casual.experience)
    }
}
