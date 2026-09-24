package fohstaffmanagement

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.enums.ActionType
import fohstaffmanagement.FohStaffFixtures.LogCapture
import fohstaffmanagement.FohStaffFixtures.casualGroup
import fohstaffmanagement.FohStaffFixtures.menu
import fohstaffmanagement.FohStaffFixtures.primed
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the priority rules the manager uses to pick which waiter seats an arriving group
 * (F17, FOH - Staff Management): skip a waiter who would exceed the SEATING action limit for the
 * tick (rule 1), otherwise prefer the waiter with the most customers below a current load of 10
 * (rule 2), fall back to the least-loaded waiter once everybody is at or above 10 (rule 3), and
 * break every remaining tie by the lowest id, with a waiter that already has an id beating one
 * that has never acted (rule 4).
 */
class WaiterAssignmentTieBreakTest {
    private lateinit var log: LogCapture

    @BeforeEach
    fun setup() {
        log = LogCapture()
        FohStaffFixtures.resetClock()
    }

    @AfterEach
    fun tearDown() = log.release()

    private fun seatedBy(): String = log.lines("FOH Seating (R").single().substringAfter("by waitstaff ")

    @Test
    fun `rule 4 - with equal current load the waiter with the lowest id seats the group`() {
        val restaurant = primed(assigned = 2, total = 2, log)

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("1.", seatedBy())
    }

    @Test
    fun `rule 4 - a waiter that already has an id wins a tie against one that has not acted yet`() {
        val restaurant = primed(assigned = 1, total = 2, log)

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("1.", seatedBy())
        assertNull(restaurant.waiters.first().id, "the waiter listed first never acted")
    }

    @Test
    fun `rule 2 - the waiter with the most customers below a load of 10 beats a lower id`() {
        val restaurant = primed(assigned = 2, total = 2, log)
        restaurant.waiters.single { it.id == 2 }.currentLoad = 6

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("2.", seatedBy())
    }

    @Test
    fun `rule 3 - with everybody at a load of 10 or more the least loaded waiter is chosen`() {
        val restaurant = primed(assigned = 2, total = 2, log)
        restaurant.waiters.single { it.id == 1 }.currentLoad = Constants.ACTION_LIMIT + 5
        restaurant.waiters.single { it.id == 2 }.currentLoad = Constants.ACTION_LIMIT

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("2.", seatedBy())
    }

    @Test
    fun `rule 3 - a tie on the least load goes to the lowest id, not to a waiter that never acted`() {
        val restaurant = primed(assigned = 1, total = 2, log)
        restaurant.waiters.forEach { it.currentLoad = Constants.ACTION_LIMIT }

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("1.", seatedBy())
        assertNull(restaurant.waiters.first().id)
    }

    @Test
    fun `rule 1 - a waiter that would exceed 10 SEATING actions is skipped and the next waiter gets the next id`() {
        val restaurant = primed(assigned = 1, total = 2, log)
        val busy = restaurant.waiters.single { it.id == 1 }
        busy.currentLoad = 8
        busy.addToTickLoad(ActionType.SEAT, 8)

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("2.", seatedBy())
        assertTrue(restaurant.waiters.first().id == 2)
    }
}
