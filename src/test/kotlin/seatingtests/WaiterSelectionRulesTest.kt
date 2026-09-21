package seatingtests

import de.unisaarland.cs.se.selab.Constants
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
import seatingtests.FohStaffFixtures.primed
import seatingtests.FohStaffFixtures.tables
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `assignWaiter` rules test coverage */
class WaiterSelectionRulesTest {
    private lateinit var log: LogCapture

    @BeforeEach
    fun setup() {
        log = LogCapture()
        FohStaffFixtures.resetClock()
    }

    @AfterEach
    fun tearDown() = log.release()

    private fun seatedBy(): String = log.lines("FOH Seating (R").single().substringAfter("by waitstaff ")

    // --- 1. SEAT tick load + group size must not exceed the action limit ---

    @Test
    fun `1 - a waiter whose remaining SEAT capacity is below the group size is excluded, leaving the other one`() {
        val busy = Waiter().also { it.tickLoads[ActionType.SEAT] = 8 }
        val free = Waiter()
        val foh = frontOfHouse(tables(1, size = 4), listOf(busy, free))

        foh.processArrival(casualGroup(id = 1, size = 4), menu)

        assertTrue(seatedBy().startsWith("${free.id}"), log.lines.toString())
        assertNull(busy.id, "the waiter without enough remaining SEAT capacity must never be considered")
    }

    @Test
    fun `1 - when every waiter would exceed the limit, no waiter is assigned and the group waits for a retry`() {
        val waiters = List(3) { Waiter().also { it.tickLoads[ActionType.SEAT] = 9 } }
        val foh = frontOfHouse(tables(1, size = 10), waiters)

        val removedFromQueue = foh.processArrival(casualGroup(id = 1, size = 4), menu)

        assertTrue(!removedFromQueue, "the group must be retried next tick, not turned away yet")
        assertTrue(log.lines("No free waitstaff available for group 1").isNotEmpty(), log.lines.toString())
        assertTrue(waiters.all { it.id == null })
    }

    // --- 2: among waiters with current load below the limit, the highest load one is recruited ---

    @Test
    fun `2 - among waiters below a load of 10 the one with the most customers is chosen`() {
        val lessLoaded = Waiter().also { it.currentLoad = 3 }
        val mostLoaded = Waiter().also { it.currentLoad = 7 }
        val foh = frontOfHouse(tables(1, size = 4), listOf(lessLoaded, mostLoaded))

        foh.processArrival(casualGroup(id = 1, size = 4), menu)

        assertTrue(mostLoaded.id != null, "the winning candidate must have been assigned an id")
        assertTrue(seatedBy().startsWith("${mostLoaded.id}"), log.lines.toString())
        assertEquals(11, mostLoaded.currentLoad)
        assertNull(lessLoaded.id, "the less loaded candidate lost and must stay unassigned")
        assertEquals(3, lessLoaded.currentLoad, "an unpicked candidate's load must not change")
    }

    @Test
    fun `2 - tie-break - equal current load below 10 goes to the lowest id`() {
        val restaurant = primed(assigned = 2, total = 2, log)

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("1.", seatedBy())
    }

    @Test
    fun `2 tie-break - a waiter who already has an id beats one that never acted, at equal load`() {
        val restaurant = primed(assigned = 1, total = 2, log)

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("1.", seatedBy())
        assertNull(restaurant.waiters.first().id, "the waiter listed first in the roster never acted")
    }

    // --- 3: once everybody is at or above a load of 10, the least loaded wins ---

    @Test
    fun `3 - with every waiter at or above a load of 10 the least loaded one is chosen`() {
        val restaurant = primed(assigned = 2, total = 2, log)
        restaurant.waiters.single { it.id == 1 }.currentLoad = Constants.ACTION_LIMIT + 5
        restaurant.waiters.single { it.id == 2 }.currentLoad = Constants.ACTION_LIMIT

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("2.", seatedBy())
    }

    @Test
    fun `3 - tie-break - an equal load at or above 10 goes to the lowest id, not to an unassigned waiter`() {
        val restaurant = primed(assigned = 1, total = 2, log)
        restaurant.waiters.forEach { it.currentLoad = Constants.ACTION_LIMIT }

        restaurant.foh.processArrival(casualGroup(id = 1), menu)

        assertEquals("1.", seatedBy())
        assertNull(restaurant.waiters.first().id)
    }

    // --- a waiter only gets a new id the moment they are actually chosen ---

    @Test
    fun `a waiter only gets a fresh id when actually chosen, not for every candidate in the pool`() {
        val waiters = listOf(
            Waiter().also { it.currentLoad = 1 },
            Waiter().also { it.currentLoad = 9 },
            Waiter().also { it.currentLoad = 5 }
        )
        val foh = frontOfHouse(tables(1, size = 4), waiters)

        foh.processArrival(casualGroup(id = 1, size = 4), menu)

        assertEquals(listOf(1), assignedIds(waiters), "only the winning candidate should ever draw an id")
        assertEquals(1, waiters[1].id, "the candidate with the most customers under 10 must be the winner")
    }
}
