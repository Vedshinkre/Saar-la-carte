package fohstaffmanagement

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
 * Lifecycle of the lazily assigned waiter ids over an evening and across evenings,
 */
class WaiterIdLifecycleTest {
    private lateinit var log: LogCapture

    @BeforeEach
    fun setup() {
        log = LogCapture()
        FohStaffFixtures.resetClock()
    }

    @AfterEach
    fun tearDown() = log.release()

    @Test
    fun `waiters start unassigned`() {
        val waiters = waiters(4)

        frontOfHouse(tables(1), waiters)

        assertTrue(waiters.all { it.id == null })
    }

    @Test
    fun `ids 1, 2, 3 are handed out in the order in which waiters first act`() {
        val waiters = waiters(3)
        val foh = frontOfHouse(tables(5), waiters)

        foh.processArrival(casualGroup(id = 1), menu) // waiter A: 4
        foh.processArrival(casualGroup(id = 2), menu) // waiter A: 8
        assertEquals(listOf(1), assignedIds(waiters), "A alone handled the first two groups")

        foh.processArrival(casualGroup(id = 3), menu) // A would exceed 10 -> waiter B
        assertEquals(listOf(1, 2), assignedIds(waiters))

        foh.processArrival(casualGroup(id = 4), menu) // B has the most customers below 10 -> B: 8
        assertEquals(listOf(1, 2), assignedIds(waiters))

        foh.processArrival(casualGroup(id = 5), menu) // A and B are both saturated -> waiter C
        assertEquals(listOf(1, 2, 3), assignedIds(waiters))
    }

    @Test
    fun `the seating log shows the ids in the order of first action`() {
        val foh = frontOfHouse(tables(3), waiters(3))

        foh.processArrival(casualGroup(id = 1), menu)
        foh.processArrival(casualGroup(id = 2), menu)
        foh.processArrival(casualGroup(id = 3), menu)

        val seating = log.lines("FOH Seating (R")
        assertTrue(seating[0].endsWith("by waitstaff 1."), seating[0])
        assertTrue(seating[1].endsWith("by waitstaff 1."), seating[1])
        assertTrue(seating[2].endsWith("by waitstaff 2."), seating[2])
    }

    @Test
    fun `an assigned id is kept through serving, the per-tick reset and escorting`() {
        val waiters = waiters(2)
        val foh = frontOfHouse(tables(2), waiters)
        val group = casualGroup(id = 1)
        foh.processArrival(group, menu)
        val waiter = waiters.single { it.id != null }
        assertEquals(1, waiter.id)

        setDishes(group, DishStatus.COOKED)
        foh.processServing()
        assertEquals(1, waiter.id, "after serving")

        foh.clearActionLoads()
        assertEquals(1, waiter.id, "after the tick loads were cleared")

        setDishes(group, DishStatus.EATEN)
        foh.processEscorting()
        assertEquals(1, waiter.id, "after escorting")
        assertEquals(listOf(1), assignedIds(waiters), "the second waiter never acted")

        assertTrue(log.lines("FOH Serving (R").single().contains("Waitstaff 1 serves"))
        assertTrue(log.lines("FOH Escorting (R").single().contains("Waitstaff 1"))
    }

    @Test
    fun `waiters that never act stay unassigned and do not consume ids`() {
        val waiters = waiters(5)
        val foh = frontOfHouse(tables(3), waiters)

        foh.processArrival(casualGroup(id = 1), menu)
        foh.processArrival(casualGroup(id = 2), menu)
        foh.processArrival(casualGroup(id = 3), menu) // forces a second waiter

        assertEquals(listOf(1, 2), assignedIds(waiters))
        assertEquals(3, waiters.count { it.id == null })
    }

    @Test
    fun `an evening in which nobody acts leaves every waiter unassigned`() {
        val waiters = waiters(3)
        val foh = frontOfHouse(tables(1), waiters)

        foh.endFohOpeningTime()

        assertTrue(waiters.all { it.id == null })
    }

    @Test
    fun `at the end of the evening all ids are reset to unassigned`() {
        val waiters = waiters(3)
        val foh = frontOfHouse(tables(3), waiters)
        foh.processArrival(casualGroup(id = 1), menu)
        foh.processArrival(casualGroup(id = 2), menu)
        foh.processArrival(casualGroup(id = 3), menu)
        assertEquals(listOf(1, 2), assignedIds(waiters))

        foh.endFohOpeningTime()

        assertTrue(waiters.all { it.id == null })
    }

    @Test
    fun `the counter starts at 1 again in the next evening`() {
        val waiters = waiters(3)
        val foh = frontOfHouse(tables(3), waiters)
        foh.processArrival(casualGroup(id = 1), menu)
        foh.processArrival(casualGroup(id = 2), menu)
        foh.processArrival(casualGroup(id = 3), menu)
        assertEquals(listOf(1, 2), assignedIds(waiters))
        foh.endFohOpeningTime()

        FohStaffFixtures.resetClock()
        foh.processArrival(casualGroup(id = 4), menu)

        assertEquals(listOf(1), assignedIds(waiters), "a single waiter acted in evening 2, so exactly id 1")
        assertTrue(log.lines("FOH Seating (R").last().endsWith("by waitstaff 1."))
    }

    @Test
    fun `every evening hands out the ids afresh`() {
        val waiters = waiters(2)
        val foh = frontOfHouse(tables(3), waiters)

        repeat(3) { evening ->
            assertTrue(waiters.all { it.id == null }, "evening ${evening + 1} starts unassigned")
            foh.processArrival(casualGroup(id = 3 * evening + 1), menu)
            foh.processArrival(casualGroup(id = 3 * evening + 2), menu)
            foh.processArrival(casualGroup(id = 3 * evening + 3), menu)
            assertEquals(listOf(1, 2), assignedIds(waiters), "evening ${evening + 1}")
            foh.endFohOpeningTime()
        }
    }

    @Test
    fun `ids are counted per restaurant`() {
        val firstWaiters = waiters(2)
        val secondWaiters = waiters(2)
        val first = frontOfHouse(tables(3), firstWaiters)
        val second = frontOfHouse(tables(3), secondWaiters)

        first.processArrival(casualGroup(id = 1), menu)
        first.processArrival(casualGroup(id = 2), menu)
        first.processArrival(casualGroup(id = 3), menu)
        second.processArrival(casualGroup(id = 4), menu)

        assertEquals(listOf(1, 2), assignedIds(firstWaiters))
        assertEquals(listOf(1), assignedIds(secondWaiters))
    }

    @Test
    fun `closing one restaurant does not reset another restaurant's ids`() {
        val firstWaiters = waiters(1)
        val secondWaiters = waiters(1)
        val first = frontOfHouse(tables(1), firstWaiters)
        val second = frontOfHouse(tables(1), secondWaiters)
        first.processArrival(casualGroup(id = 1), menu)
        second.processArrival(casualGroup(id = 2), menu)

        first.endFohOpeningTime()

        assertNull(firstWaiters.single().id)
        assertEquals(1, secondWaiters.single().id)
    }
}
