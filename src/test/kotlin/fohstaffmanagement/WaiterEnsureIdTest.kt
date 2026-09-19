package fohstaffmanagement

import de.unisaarland.cs.se.selab.actors.Waiter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Unit tests for the lazy id assignment of a single [Waiter] via [Waiter.ensureId]. */
class WaiterEnsureIdTest {

    @Test
    fun `a new waiter starts without an id`() {
        assertNull(Waiter().id)
    }

    @Test
    fun `ensureId assigns the id handed out by the counter to an unassigned waiter`() {
        val waiter = Waiter()

        val id = waiter.ensureId { 1 }

        assertEquals(1, id)
        assertEquals(1, waiter.id)
    }

    @Test
    fun `ensureId does not ask the counter again once the waiter has an id`() {
        val waiter = Waiter()
        var counter = 1
        val nextId = { counter++ }

        val first = waiter.ensureId(nextId)
        val second = waiter.ensureId(nextId)
        val third = waiter.ensureId(nextId)

        assertEquals(1, first)
        assertEquals(1, second)
        assertEquals(1, third)
        assertEquals(2, counter, "the counter was consumed exactly once")
    }

    @Test
    fun `ids are handed out sequentially in the order of the first action, not of creation`() {
        val waiters = List(3) { Waiter() }
        var counter = 1
        val nextId = { counter++ }

        waiters[2].ensureId(nextId)
        waiters[0].ensureId(nextId)
        waiters[1].ensureId(nextId)

        assertEquals(1, waiters[2].id)
        assertEquals(2, waiters[0].id)
        assertEquals(3, waiters[1].id)
    }

    @Test
    fun `a waiter that never acts never draws from the counter`() {
        val acting = Waiter()
        val inactive = Waiter()
        var counter = 1

        acting.ensureId { counter++ }

        assertNull(inactive.id)
        assertEquals(2, counter)
    }

    @Test
    fun `resetting the per-tick action loads keeps the assigned id`() {
        val waiter = Waiter()
        waiter.ensureId { 7 }

        waiter.resetActionLoads()

        assertEquals(7, waiter.id)
    }
}
