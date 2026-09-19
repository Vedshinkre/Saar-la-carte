package fohstaffmanagement

import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.restaurant.Table
import fohstaffmanagement.FohStaffFixtures.LogCapture
import fohstaffmanagement.FohStaffFixtures.assignedIds
import fohstaffmanagement.FohStaffFixtures.casualGroup
import fohstaffmanagement.FohStaffFixtures.eventGroup
import fohstaffmanagement.FohStaffFixtures.frontOfHouse
import fohstaffmanagement.FohStaffFixtures.menu
import fohstaffmanagement.FohStaffFixtures.setDishes
import fohstaffmanagement.FohStaffFixtures.tables
import fohstaffmanagement.FohStaffFixtures.waiters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * EVENT groups have no assigned waiter: the manager decides ad hoc which waiters act.
 */
class WaiterIdEventGroupTest {
    private lateinit var log: LogCapture

    @BeforeEach
    fun setup() {
        log = LogCapture()
        FohStaffFixtures.resetClock()
    }

    @AfterEach
    fun tearDown() = log.release()

    @Test
    fun `an EVENT seating only gives ids to the waiters that actually seat customers`() {
        val waiters = waiters(3)
        val foh = frontOfHouse(tables(1, size = 15), waiters)
        val group = eventGroup(id = 1, size = 15)
        foh.reserveTables(group)

        foh.processArrival(group, menu)

        assertEquals(listOf(1, 2), assignedIds(waiters), "the third waiter is not needed")
        assertTrue(log.lines("FOH Seating (R").single().endsWith("by waitstaff 1,2."), log.lines.toString())
    }

    @Disabled
    @Test
    fun `a failed EVENT seating still numbers the waiters that seated part of the group`() {
        val waiters = waiters(2)
        val foh = frontOfHouse(tables(1, size = 25), waiters)
        val group = eventGroup(id = 1, size = 25)
        foh.reserveTables(group)

        foh.processArrival(group, menu)

        assertEquals(1, log.lines("FOH No Seating").size, "sanity check: the seating really failed")
        assertEquals(listOf(1, 2), assignedIds(waiters))
    }

    @Disabled
    @Test
    fun `a failed EVENT seating still counts the partial SEATING actions towards the tick load`() {
        val waiters = waiters(2)
        val foh = frontOfHouse(tables(1, size = 25), waiters)
        val group = eventGroup(id = 1, size = 25)
        foh.reserveTables(group)

        foh.processArrival(group, menu)

        assertEquals(listOf(10, 10), waiters.map { it.getTickLoad(ActionType.SEAT) })
        assertEquals(listOf(0, 0), waiters.map { it.currentLoad })
    }

    @Disabled
    @Test
    fun `after a failed EVENT seating the exhausted waiters are not free for a CASUAL group in that tick`() {
        val waiters = waiters(2)
        val foh = frontOfHouse(listOf(Table(1, 25, TableType.COMMON), Table(2, 4, TableType.COMMON)), waiters)
        val event = eventGroup(id = 1, size = 25)
        foh.reserveTables(event)
        foh.processArrival(event, menu)
        log.clear()

        foh.processArrival(casualGroup(id = 2), menu)

        assertEquals(1, log.lines("No free waitstaff").size, log.lines.toString())
        assertEquals(0, log.lines("FOH Seating (R").size, "both waiters already used up their SEATING actions")
    }

    @Disabled
    @Test
    fun `ESCORTING an EVENT group only gives ids to the waiters that escort`() {
        val waiters = waiters(3)
        val foh = frontOfHouse(tables(1), waiters)
        val group = eventGroup(id = 1, size = 4)
        foh.reserveTables(group)
        foh.processArrival(group, menu)
        assertEquals(listOf(1), assignedIds(waiters), "sanity check: one waiter seated the group")
        setDishes(group, DishStatus.EATEN)

        foh.processEscorting()

        assertEquals(1, log.lines("FOH Escorting (R").size, "one waiter escorted everybody")
        assertEquals(listOf(1), assignedIds(waiters), "idle waiters must not be numbered")
    }

    @Test
    fun `SERVING an EVENT table recruits the lowest ids first, not waiters that never acted`() {
        val waiters = waiters(3)
        val foh = frontOfHouse(tables(1, size = 15), waiters)
        val group = eventGroup(id = 1, size = 15)
        foh.reserveTables(group)
        foh.processArrival(group, menu)
        assertEquals(listOf(1, 2), assignedIds(waiters), "sanity check")
        setDishes(group, DishStatus.COOKED)

        foh.processServing()

        assertEquals(listOf(1, 2), assignedIds(waiters), "the third waiter should not have been needed")
    }

    @Disabled
    @Test
    fun `EVENT seating prefers the lowest id when waiters are tied on current load`() {
        val restaurant = FohStaffFixtures.primed(assigned = 1, total = 2, log)
        val group = eventGroup(id = 1, size = 4)
        restaurant.foh.reserveTables(group)

        restaurant.foh.processArrival(group, menu)

        assertTrue(log.lines("FOH Seating (R").single().endsWith("by waitstaff 1."), log.lines.toString())
        assertNull(restaurant.waiters.first().id, "the waiter that never acted must lose the tie")
    }

    @Disabled
    @Test
    fun `EVENT escorting prefers the lowest id when waiters are tied on current load`() {
        val restaurant = FohStaffFixtures.primed(assigned = 2, total = 3, log)
        val group = eventGroup(id = 1, size = 4)
        restaurant.foh.reserveTables(group)
        restaurant.waiters.sortBy { it.id ?: Int.MAX_VALUE }
        restaurant.foh.processArrival(group, menu)
        setDishes(group, DishStatus.EATEN)
        restaurant.foh.clearActionLoads()
        restaurant.waiters.sortWith(compareBy<Waiter> { it.id != null }.thenByDescending { it.id })
        log.clear()

        restaurant.foh.processEscorting()

        val escorting = log.lines("FOH Escorting (R")
        assertEquals(1, escorting.size, log.lines.toString())
        assertTrue(escorting.single().contains("Waitstaff 1"), escorting.single())
        assertEquals(listOf(1, 2), assignedIds(restaurant.waiters), "the third waiter never had to act")
    }
}
