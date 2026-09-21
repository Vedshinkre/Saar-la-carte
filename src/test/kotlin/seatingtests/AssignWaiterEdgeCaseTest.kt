package seatingtests

import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.ActionType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import seatingtests.FohStaffFixtures.LogCapture
import seatingtests.FohStaffFixtures.assignedIds
import seatingtests.FohStaffFixtures.casualGroup
import seatingtests.FohStaffFixtures.frontOfHouse
import seatingtests.FohStaffFixtures.menu
import seatingtests.FohStaffFixtures.tables
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** some edge cases for seating waiter assignment */
class AssignWaiterEdgeCaseTest {
    private lateinit var log: LogCapture

    @BeforeEach
    fun setup() {
        log = LogCapture()
        FohStaffFixtures.resetClock()
    }

    @AfterEach
    fun tearDown() = log.release()

    @Test
    fun `a restaurant with zero waiters never seats anybody and the group retries next tick`() {
        val foh = frontOfHouse(tables(1), emptyList())

        val removeFromQueue = foh.processArrival(casualGroup(id = 1), menu)

        assertTrue(!removeFromQueue, "with no waiter at all the group is not yet resolved, it waits for a retry")
        assertTrue(log.lines("No free waitstaff available for group 1").isNotEmpty(), log.lines.toString())
        assertTrue(log.lines("FOH Seating (R").isEmpty())
    }

    @Test
    fun `a restaurant with zero waiters logs zero seated for the tick status`() {
        val foh = frontOfHouse(tables(1), emptyList())

        foh.processArrival(casualGroup(id = 1), menu)
        foh.logAndResetSeatingOrderingTickStatus()

        assertTrue(
            log.lines("FOH Seating Status (R").single().endsWith("0 waitstaff seated 0 customers on 0 tables."),
            log.lines.toString()
        )
    }

    @Test
    fun `getTickLoad falls back to 0 for an action type missing from the waiter's own map`() {
        val waiter = Waiter()
        waiter.tickLoads.remove(ActionType.SEAT)

        assertEquals(0, waiter.getTickLoad(ActionType.SEAT))
    }

    @Test
    fun `addToTickLoad on a waiter missing that action type starts counting from 0`() {
        val waiter = Waiter()
        waiter.tickLoads.remove(ActionType.SEAT)

        waiter.addToTickLoad(ActionType.SEAT, 3)

        assertEquals(3, waiter.getTickLoad(ActionType.SEAT))
    }

    @Test
    fun `waiters with a current load at or above 10 are excluded even with spare tick capacity`() {
        val busy = List(2) { Waiter().also { it.currentLoad = 20 } }
        val idle = Waiter()
        val waiters = busy + idle
        val foh = frontOfHouse(tables(1, size = 4), waiters)

        foh.processArrival(casualGroup(id = 1, size = 4), menu)

        assertEquals(listOf(1), assignedIds(waiters))
        assertEquals(1, idle.id)
        assertTrue(busy.all { it.id == null })
    }
}
