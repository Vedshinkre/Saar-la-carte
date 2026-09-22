package fohstaffmanagement

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import fohstaffmanagement.FohStaffFixtures.LogCapture
import fohstaffmanagement.FohStaffFixtures.assignedIds
import fohstaffmanagement.FohStaffFixtures.casualGroup
import fohstaffmanagement.FohStaffFixtures.frontOfHouse
import fohstaffmanagement.FohStaffFixtures.menu
import fohstaffmanagement.FohStaffFixtures.setDishes
import fohstaffmanagement.FohStaffFixtures.tables
import fohstaffmanagement.FohStaffFixtures.waiters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for a waiter's *workload* bookkeeping
 */
class WaiterWorkloadIntegrationTest {
    private lateinit var log: LogCapture

    @BeforeEach
    fun setup() {
        log = LogCapture()
        FohStaffFixtures.resetClock()
    }

    @AfterEach
    fun tearDown() = log.release()

    @Test
    fun `SEAT tick load resets every tick, but currentLoad keeps accumulating across the evening`() {
        val waiters = waiters(1)
        val foh = frontOfHouse(tables(2, size = 8), waiters)
        val waiter = waiters.single()

        foh.processArrival(casualGroup(id = 1, size = 8), menu)
        assertEquals(8, waiter.currentLoad)
        assertEquals(8, waiter.getTickLoad(ActionType.SEAT))

        foh.clearActionLoads()
        Time.tick = 2
        foh.processArrival(casualGroup(id = 2, size = 8), menu)

        assertEquals(16, waiter.currentLoad, "the waiter is still being waited on for both groups")
        assertEquals(8, waiter.getTickLoad(ActionType.SEAT), "this tick's load reflects only the second group")
        assertEquals(listOf(1), assignedIds(waiters), "sanity check: the same waiter handled both")
    }

    @Test
    fun `a full seat-to-escort cycle drains currentLoad to zero and records the ESCORT tick load`() {
        val waiters = waiters(1)
        val foh = frontOfHouse(tables(1, size = 4), waiters)
        val waiter = waiters.single()
        val group = casualGroup(id = 1, size = 4)

        foh.processArrival(group, menu)
        assertEquals(4, waiter.currentLoad)

        setDishes(group, DishStatus.EATEN)
        foh.processEscorting()

        assertEquals(0, waiter.currentLoad, "every customer of the group has now left")
        assertEquals(4, waiter.getTickLoad(ActionType.ESCORT), "the ESCORT tick load records how many left")
    }

    @Test
    fun `a group abandoned during eating frees its waiter's currentLoad for a brand new group later on`() {
        val waiters = waiters(1)
        val foh = frontOfHouse(tables(1, size = 4), waiters)
        val waiter = waiters.single()
        val group = casualGroup(id = 1, size = 3)

        foh.processArrival(group, menu)
        assertEquals(3, waiter.currentLoad)

        // dishes are never cooked, so once the group's patience (UNSERVED_WAIT_TICKS) runs out it gives up
        Time.tick = 1 + Constants.UNSERVED_WAIT_TICKS - 1
        foh.processEating()

        assertEquals(0, waiter.currentLoad, "customers who gave up waiting are no longer waited on")
        assertTrue(
            log.lines("Restaurant No Eating").single().endsWith("leave table 1 due to not being served."),
            log.lines.toString()
        )

        foh.processEscorting() // nobody is left to physically escort out, but the now-empty table is freed
        foh.clearActionLoads()
        Time.tick += 1

        foh.processArrival(casualGroup(id = 2, size = 3), menu)

        assertEquals(listOf(1), assignedIds(waiters), "sanity check: the same, now-free waiter took it")
        assertEquals(3, waiter.currentLoad, "a fresh group is being waited on again")
    }

    @Test
    fun `closing the opening time zeroes a waiter's currentLoad even if their group never finished eating`() {
        val waiters = waiters(1)
        val foh = frontOfHouse(tables(1, size = 4), waiters)
        val waiter = waiters.single()
        foh.processArrival(casualGroup(id = 1, size = 4), menu)
        assertEquals(4, waiter.currentLoad)

        foh.endFohOpeningTime()

        assertEquals(0, waiter.currentLoad, "closing time clears outstanding workload along with the id")
        assertNull(waiter.id)
    }
}
