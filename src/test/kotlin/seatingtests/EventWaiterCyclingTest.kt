package seatingtests

import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import seatingtests.FohStaffFixtures.LogCapture
import seatingtests.FohStaffFixtures.eventGroup
import seatingtests.FohStaffFixtures.frontOfHouse
import seatingtests.FohStaffFixtures.menu
import seatingtests.FohStaffFixtures.tables
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** test the recruited waiter cycling for event groups */
class EventWaiterCyclingTest {
    private lateinit var log: LogCapture

    @BeforeEach
    fun setup() {
        log = LogCapture()
        FohStaffFixtures.resetClock()
    }

    @AfterEach
    fun tearDown() = log.release()

    @Test
    fun `the spec worked example splits a 15-customer group 10,4,1 across waiters in descending load order`() {
        // spec: "the first waiter, who has a current load of 15 customers but SEATING tick load
        // of 0 will seat 10 of the customers. The next waiter, who has a current load of 11
        // customers and a SEATING tick load of 6 will seat the next 4, and a last waiter with no
        // current load or tick load will seat the last one."
        val first = Waiter().also { it.currentLoad = 15 }
        val second = Waiter().also {
            it.currentLoad = 11
            it.tickLoads[ActionType.SEAT] = 6
        }
        val third = Waiter()
        val foh = frontOfHouse(tables(1, size = 15), listOf(first, second, third))
        val group = eventGroup(id = 1, size = 15)
        foh.reserveTables(group)

        val removedFromQueue = foh.processArrival(group, menu)

        assertTrue(removedFromQueue)
        assertEquals(10, first.getTickLoad(ActionType.SEAT))
        assertEquals(10, second.getTickLoad(ActionType.SEAT))
        assertEquals(1, third.getTickLoad(ActionType.SEAT))
        assertTrue(
            log.lines("FOH Seating (R").single().endsWith("by waitstaff ${first.id!!},${second.id!!},${third.id!!}."),
            log.lines.toString()
        )
    }

    @Test
    fun `a group that exactly exhausts the last waiter's capacity does not recruit a further waiter`() {
        val first = Waiter().also { it.currentLoad = 6 }
        val second = Waiter()
        val foh = frontOfHouse(tables(1, size = 10), listOf(first, second))
        val group = eventGroup(id = 1, size = 10)
        foh.reserveTables(group)

        foh.processArrival(group, menu)

        assertEquals(10, first.getTickLoad(ActionType.SEAT))
        assertEquals(0, second.getTickLoad(ActionType.SEAT))
        assertNull(second.id, "the second waiter was never needed and must stay unassigned")
    }

    @Test
    fun `when the available waiters together cannot cover the group, the whole seating attempt fails`() {
        // two waiters can seat at most 10 each = 20, which is below the group's 25
        val waiters = List(2) { Waiter() }
        val foh = frontOfHouse(tables(1, size = 25), waiters)
        val group = eventGroup(id = 1, size = 25)
        foh.reserveTables(group)

        val removedFromQueue = foh.processArrival(group, menu)

        assertTrue(removedFromQueue, "the attempt is settled this tick, whether it succeeded or not")
        assertTrue(
            log.lines("FOH No Seating (R").single().endsWith("No free waitstaff available for group 1."),
            log.lines.toString()
        )
        assertTrue(log.lines("FOH Seating (R").isEmpty(), "a failed EVENT attempt must not be logged as a seating")
        assertNull(group.currentOrder, "the group's order is never placed once the seating attempt failed")
    }

    @Test
    fun `a failed EVENT seating leaves the group with a negative experience and no meal ordered`() {
        val waiters = List(1) { Waiter() }
        val foh = frontOfHouse(tables(1, size = 25), waiters)
        val group = eventGroup(id = 1, size = 25)
        foh.reserveTables(group)

        foh.processArrival(group, menu)

        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertEquals(25, group.customersRemainingInRestaurant, "nobody in the group was ever actually seated")
    }
}
