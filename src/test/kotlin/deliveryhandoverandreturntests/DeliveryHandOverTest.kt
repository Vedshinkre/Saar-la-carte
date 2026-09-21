package deliveryhandoverandreturntests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * F20 hand-over: the waiter gives a delivery order's meals to a driver. Unit tests of the delivery
 * half of [de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor]. The outbound trip that
 * follows is in `deliveryoutboundtests`.
 */
class DeliveryHandOverTest {
    private lateinit var fx: DeliveryFixtures

    @BeforeEach
    fun setup() {
        fx = DeliveryFixtures()
    }

    private fun waiter(id: Int, serveLoad: Int = 0) = Waiter().apply {
        this.id = id
        addToTickLoad(ActionType.SERVE, serveLoad)
    }

    @Test
    fun `an order with an uncooked dish stays with the kitchen and gets no driver`() {
        val group = fx.deliveryGroup(1)
        group.currentOrder!!.dishes.first().status = DishStatus.UNCOOKED
        val driver = Driver()

        fx.serving(listOf(waiter(1)), listOf(driver), listOf(group)).processServing()

        assertEquals(DriverState.IDLE, driver.state)
        assertNull(driver.id, "a driver only gets an id when it receives meals")
        assertTrue(fx.logLinesContaining("FOH Delivery").isEmpty())
        assertEquals(listOf(DishStatus.UNCOOKED, DishStatus.COOKED), group.currentOrder!!.dishes.map { it.status })
    }

    @Test
    fun `a complete order is served to the driver, who then waits for the delivery to be prepared`() {
        val group = fx.deliveryGroup(1, dishCount = 2)
        val order = group.currentOrder!!
        val driver = Driver()

        fx.serving(listOf(waiter(1)), listOf(driver), listOf(group)).processServing()

        assertEquals(DriverState.WAITING, driver.state)
        assertEquals(1, driver.id)
        assertTrue(driver.targetGroup === group)
        assertTrue(driver.currentOrder === order)
        assertTrue(order.dishes.all { it.status == DishStatus.SERVED })
        val line = fx.logLinesContaining("FOH Delivery").single()
        assertTrue(line.startsWith("[IMPORTANT] FOH Delivery (R 1): Waitstaff 1 serves "), line)
        assertTrue(line.endsWith("meals to driver 1 for order ${order.id}."), line)
    }

    @Test
    fun `drivers get ids 1 and 2 in the order the orders are handed over, not by group id`() {
        val first = fx.deliveryGroup(7)
        val second = fx.deliveryGroup(3)
        val drivers = listOf(Driver(), Driver())

        fx.serving(listOf(waiter(1)), drivers, listOf(first, second)).processServing()

        // orders are handed over in ascending order id, and order ids follow creation
        assertEquals(1, drivers[0].id)
        assertTrue(drivers[0].targetGroup === first)
        assertEquals(2, drivers[1].id)
        assertTrue(drivers[1].targetGroup === second)
    }

    @Test
    fun `a driver that already has an id keeps it and does not use up a new one`() {
        val returned = Driver().apply { id = 1 }
        val fresh = Driver()
        val group = fx.deliveryGroup(1)
        val serving = fx.serving(listOf(waiter(1)), listOf(returned, fresh), listOf(group))

        serving.processServing()

        assertEquals(1, returned.id)
        assertNull(fresh.id)
    }

    @Test
    fun `resetting the driver id counter restarts driver ids at 1 for the next evening`() {
        val eveningOneDriver = Driver()
        val serving = fx.serving(listOf(waiter(1)), listOf(eveningOneDriver), listOf(fx.deliveryGroup(1)))
        serving.processServing()
        assertEquals(1, eveningOneDriver.id)

        val eveningTwoDriver = Driver()
        val nextServing = fx.serving(listOf(waiter(1)), listOf(eveningTwoDriver), listOf(fx.deliveryGroup(2)))
        nextServing.resetDriverIdCounter()
        nextServing.processServing()

        assertEquals(1, eveningTwoDriver.id)
    }

    @Test
    fun `without a driver that is idle nothing is handed over`() {
        val group = fx.deliveryGroup(1)
        val busy = Driver().apply { state = DriverState.RETURNING }

        fx.serving(listOf(waiter(1)), listOf(busy), listOf(group)).processServing()

        assertTrue(group.currentOrder!!.dishes.all { it.status == DishStatus.COOKED })
        assertTrue(fx.logLinesContaining("FOH Delivery").isEmpty())
    }

    @Test
    fun `without a waiter with free serving capacity the meals stay cooked`() {
        val group = fx.deliveryGroup(1)
        val fullWaiter = waiter(1, serveLoad = Constants.ACTION_LIMIT)

        fx.serving(listOf(fullWaiter), listOf(Driver()), listOf(group)).processServing()

        assertTrue(group.currentOrder!!.dishes.all { it.status == DishStatus.COOKED })
        assertTrue(fx.logLinesContaining("FOH Delivery").isEmpty())
    }

    @Test
    fun `the waiter with the smallest id and free capacity serves the driver`() {
        val group = fx.deliveryGroup(1)
        val busy = waiter(1, serveLoad = Constants.ACTION_LIMIT)
        val free = waiter(2)
        val alsoFree = waiter(3)

        fx.serving(listOf(alsoFree, busy, free), listOf(Driver()), listOf(group)).processServing()

        assertEquals(1, fx.logLinesContaining("Waitstaff 2 serves").size)
        assertTrue(fx.logLinesContaining("Waitstaff 3 serves").isEmpty())
    }

    @Test
    fun `an order larger than one waiter's capacity is split over the waiters with one log each`() {
        val group = fx.deliveryGroup(1, dishCount = Constants.ACTION_LIMIT + 2)

        fx.serving(listOf(waiter(1), waiter(2)), listOf(Driver()), listOf(group)).processServing()

        assertTrue(group.currentOrder!!.dishes.all { it.status == DishStatus.SERVED })
        val lines = fx.logLinesContaining("FOH Delivery")
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("Waitstaff 1 serves"), lines[0])
        assertTrue(lines[1].contains("Waitstaff 2 serves"), lines[1])
    }

    @Test
    fun `meals no waiter could hand over this tick follow in the next tick to the same driver`() {
        val group = fx.deliveryGroup(1, dishCount = Constants.ACTION_LIMIT + 2)
        val other = fx.deliveryGroup(2)
        val onlyWaiter = waiter(1)
        val driver = Driver()
        val serving = fx.serving(listOf(onlyWaiter), listOf(driver), listOf(group, other))

        serving.processServing()

        val dishes = group.currentOrder!!.dishes
        assertEquals(Constants.ACTION_LIMIT, dishes.count { it.status == DishStatus.SERVED })
        assertEquals(2, dishes.count { it.status == DishStatus.COOKED })

        onlyWaiter.resetActionLoads()
        serving.processServing()

        assertTrue(dishes.all { it.status == DishStatus.SERVED })
        assertTrue(driver.currentOrder === group.currentOrder, "a driver carries one group's order only")
        assertTrue(other.currentOrder!!.dishes.all { it.status == DishStatus.COOKED }, "no second driver for group 2")
    }
}
