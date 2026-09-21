package planningtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * REGULAR groups (F22): the kitchen plans for the orders of the last three visits, and the group only comes on the
 * evenings given by `visitingStart` and `visitingPeriod`, until it has failed twice in a row.
 */
class RegularGroupTest {
    private fun group(visitingStart: Int = 3, visitingPeriod: Int = 2) = RegularGroup(
        id = 1,
        size = 2,
        tableType = TableType.COMMON,
        visitingAt = 4,
        foodPreferences = emptyList(),
        visitingStart = visitingStart,
        visitingPeriod = visitingPeriod,
        restaurantId = 1
    )

    @BeforeEach
    fun setUp() {
        Order.resetIds()
        Time.tick = 1
    }

    private fun visitingOn(group: RegularGroup, evening: Int): Boolean {
        Time.evening = evening
        return group.isVisitingTonight()
    }

    // ---- the last three visits

    @Test
    fun `a group without visits has no order history`() {
        assertTrue(group().orderHistory.isEmpty())
    }

    @Test
    fun `fewer than three visits are all kept`() {
        val group = group()
        val orders = List(2) { Order(emptyList()) }

        orders.forEach { group.addOrderToHistory(it) }

        assertEquals(orders, group.orderHistory.toList())
    }

    @Test
    fun `exactly three visits are all kept`() {
        val group = group()
        val orders = List(3) { Order(emptyList()) }

        orders.forEach { group.addOrderToHistory(it) }

        assertEquals(orders, group.orderHistory.toList())
    }

    @Test
    fun `only the last three visits are kept and the oldest one is dropped`() {
        val group = group()
        val orders = List(5) { Order(emptyList()) }

        orders.forEach { group.addOrderToHistory(it) }

        assertEquals(orders.takeLast(3), group.orderHistory.toList())
    }

    // ---- the visiting evenings

    @Test
    fun `the group visits on its first evening and then every period`() {
        val group = group(visitingStart = 3, visitingPeriod = 2)

        assertEquals(listOf(3, 5, 7, 9), (1..10).filter { visitingOn(group, it) })
    }

    @Test
    fun `a period of one means every evening from the first one on`() {
        val group = group(visitingStart = 2, visitingPeriod = 1)

        assertEquals(listOf(2, 3, 4, 5), (1..5).filter { visitingOn(group, it) })
    }

    @Test
    fun `the group does not come before its first evening`() {
        assertFalse(visitingOn(group(visitingStart = 4, visitingPeriod = 1), 3))
    }

    @Test
    fun `after two failed attempts the group never visits again`() {
        val group = group(visitingStart = 1, visitingPeriod = 1)
        group.failedAttempts = 1
        assertTrue(visitingOn(group, 3))

        group.failedAttempts = 2

        assertFalse(visitingOn(group, 3))
        assertFalse(visitingOn(group, 4))
    }
}
