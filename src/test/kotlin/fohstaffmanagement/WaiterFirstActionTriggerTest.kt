package fohstaffmanagement

import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.EscortingProcessor
import de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor
import fohstaffmanagement.FohStaffFixtures.LogCapture
import fohstaffmanagement.FohStaffFixtures.assignedIds
import fohstaffmanagement.FohStaffFixtures.casualGroup
import fohstaffmanagement.FohStaffFixtures.frontOfHouse
import fohstaffmanagement.FohStaffFixtures.menu
import fohstaffmanagement.FohStaffFixtures.orderWith
import fohstaffmanagement.FohStaffFixtures.tables
import fohstaffmanagement.FohStaffFixtures.waiters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 *To Test id upon action
 */
class WaiterFirstActionTriggerTest {
    private lateinit var log: LogCapture

    @BeforeEach
    fun setup() {
        log = LogCapture()
        FohStaffFixtures.resetClock()
    }

    @AfterEach
    fun tearDown() = log.release()

    @Test
    fun `SEATING is logged with id 1 for the first waiter to act`() {
        val waiters = waiters(3)
        val foh = frontOfHouse(tables(1), waiters)

        foh.processArrival(casualGroup(id = 1), menu)

        assertTrue(log.lines("FOH Seating").single().endsWith("by waitstaff 1."), log.lines.toString())
        assertEquals(listOf(1), assignedIds(waiters))
    }

    @Test
    fun `TAKING ORDERS is logged with the same id the waiter received when seating`() {
        val foh = frontOfHouse(tables(1), waiters(3))

        foh.processArrival(casualGroup(id = 1), menu)

        assertTrue(log.lines("FOH Ordering").single().endsWith("with waitstaff 1."), log.lines.toString())
    }

    @Test
    fun `no waiter has an id before anybody acted, even after tables were reserved`() {
        val waiters = waiters(3)
        val foh = frontOfHouse(tables(1), waiters)

        foh.reserveTables(FohStaffFixtures.eventGroup(id = 1, size = 4))

        assertEquals(emptyList(), assignedIds(waiters))
    }

    @Test
    fun `SERVING as a waiter's first action assigns id 1`() {
        val waiter = Waiter()
        val group = casualGroup(id = 1, size = 3)
        group.currentOrder = orderWith(DishStatus.COOKED)
        var counter = 1
        val serving = ServingProcessor(
            waiters = listOf(waiter),
            drivers = emptyList(),
            deliveryGroups = emptyList(),
            getInHouseGroups = { listOf(group) },
            waiterFor = { waiter },
            getServingPriority = { 0 },
            getAssignedTableId = { 1 },
            recruitWaitersForEventGroup = { _, _ -> emptyList() },
            getNextWaiterId = { counter++ }
        )
        assertNull(waiter.id)

        serving.processServing()

        assertEquals(1, waiter.id)
        assertTrue(log.lines("FOH Serving (R").single().contains("Waitstaff 1 serves"), log.lines.toString())
    }

    @Test
    fun `a waiter with nothing to serve does not get an id`() {
        val waiter = Waiter()
        val group = casualGroup(id = 1, size = 3)
        group.currentOrder = orderWith(DishStatus.UNCOOKED)
        var counter = 1
        val serving = ServingProcessor(
            waiters = listOf(waiter),
            drivers = emptyList(),
            deliveryGroups = emptyList(),
            getInHouseGroups = { listOf(group) },
            waiterFor = { waiter },
            getServingPriority = { 0 },
            getAssignedTableId = { 1 },
            recruitWaitersForEventGroup = { _, _ -> emptyList() },
            getNextWaiterId = { counter++ }
        )

        serving.processServing()

        assertNull(waiter.id)
        assertEquals(1, counter)
    }

    @Test
    fun `ESCORTING as a waiter's first action assigns id 1`() {
        val waiter = Waiter()
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val group = casualGroup(id = 1, size = 3)
        group.currentOrder = orderWith(DishStatus.EATEN)
        var counter = 1
        val escorting = EscortingProcessor(
            customerToTable = mutableMapOf(group to listOf(table)),
            eventGroups = mutableListOf(),
            inHouseGroupsToWaiter = mapOf(group to waiter),
            getInHouseGroups = { listOf(group) },
            getServingPriority = { 0 },
            recruitWaitersForEventGroup = { _, _ -> emptyList() },
            getNextWaiterId = { counter++ }
        )
        assertNull(waiter.id)

        escorting.processEscorting()

        assertEquals(1, waiter.id)
        assertTrue(log.lines("FOH Escorting (R").single().contains("Waitstaff 1"), log.lines.toString())
    }

    @Test
    fun `a waiter whose group is still eating does not get an id from the escorting step`() {
        val waiter = Waiter()
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val group = casualGroup(id = 1, size = 3)
        group.currentOrder = orderWith(DishStatus.SERVED)
        var counter = 1
        val escorting = EscortingProcessor(
            customerToTable = mutableMapOf(group to listOf(table)),
            eventGroups = mutableListOf(),
            inHouseGroupsToWaiter = mapOf(group to waiter),
            getInHouseGroups = { listOf(group) },
            getServingPriority = { 0 },
            recruitWaitersForEventGroup = { _, _ -> emptyList() },
            getNextWaiterId = { counter++ }
        )

        escorting.processEscorting()

        assertNull(waiter.id)
        assertEquals(1, counter)
    }
}
