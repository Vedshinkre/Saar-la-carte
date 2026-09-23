package deliveryservicetest

import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Delivery Integration Test F 20
 * DeliveryIntegrationTest validates the complete delivery cycle between FrontOfHouse and DeliveryProcessor:
 * Order Reception & Handover: Verifies driver assignment during serving, partial handoffs for multi-dish orders,
 * and rejection of incomplete or unassignable orders.
 * Driver State Transitions: Confirms drivers transition correctly between IDLE, WAITING, and DELIVERING,
 * while releasing stranded drivers when orders are aborted.
 * Delivery Execution & Eating: Ensures distance-based travel ticks are calculated accurately,
 * delivery timeouts abort orders with negative ratings, customer eating states update on arrival,
 * and drivers reset properly upon restaurant closure.
 */
class DeliveryIntegrationTest {

    private lateinit var foh: FrontOfHouse
    private val drivers: MutableList<Driver> = mutableListOf()
    private lateinit var countertop: Countertop

    @BeforeEach
    fun setup() {
        Logger.setup(LogLevel.DEBUG)

        drivers.clear()
        countertop = mock()
        foh = FrontOfHouse(
            tables = emptyList(),
            waiters = emptyList(),
            drivers = drivers,
            countertop = countertop
        )
    }

    @Test
    fun `processArrival - delivery group successfully and add to delivery system`() {
        // a delivery group waiting beyond the allowed tick threshold aborts with negative experience
        val casualGroup = mock<CasualGroup>()
        val order = Order(dishes = emptyList())

        whenever(casualGroup.wantsDelivery).thenReturn(true)
        whenever(casualGroup.id).thenReturn(1)
        whenever(casualGroup.currentOrder).thenReturn(order)
        whenever(
            casualGroup.placeOrder(
                waiters = any<List<Waiter>>(),
                menu = any<List<Recipe>>(),
                countertop = eq(countertop)
            )
        ).thenReturn(true)

        whenever(casualGroup.visitingAt).thenReturn(-999)

        foh.processArrival(casualGroup, emptyList())
        foh.processDelivering()

        verify(casualGroup).experience = ExperienceType.NEGATIVE
    }

    @Test
    fun `isDriverAvailable returns correct availability based on driver states`() {
        //  driver availability returns true only when at least one driver is in the IDLE state
        val driver1 = Driver().apply { state = DriverState.DELIVERING }
        val driver2 = Driver().apply { state = DriverState.RETURNING }

        drivers.add(driver1)
        drivers.add(driver2)

        assertFalse(foh.isDriverAvailable())

        driver2.state = DriverState.IDLE
        assertTrue(foh.isDriverAvailable())
    }

    @Test
    fun `resetDrivers- what happens to drivers when the restaurant closes`() {
        //  closing the restaurant resets delivering drivers to IDLE while leaving returning drivers unaffected
        val recipe = mock<Recipe>()
        val dish1 = Dish(recipe).apply { status = DishStatus.COOKED }
        val dish2 = Dish(recipe).apply { status = DishStatus.EATEN }
        val order = Order(listOf(dish1, dish2))

        val deliveringDriver = Driver().apply {
            state = DriverState.DELIVERING
            currentOrder = order
            targetGroup = mock<CasualGroup>()
            totalTripTicks = 10
            ticksToDest = 5
            tripDistance = 20
            distanceDriven = 10
        }

        val returningDriver = Driver().apply {
            state = DriverState.RETURNING
            totalTripTicks = 10
        }

        drivers.add(deliveringDriver)
        drivers.add(returningDriver)

        foh.resetDrivers()

        assertEquals(DriverState.IDLE, deliveringDriver.state)
        assertNull(deliveringDriver.currentOrder)
        assertNull(deliveringDriver.targetGroup)
        assertEquals(0, deliveringDriver.totalTripTicks)
        assertEquals(0, deliveringDriver.ticksToDest)
        assertEquals(0, deliveringDriver.tripDistance)
        assertEquals(0, deliveringDriver.distanceDriven)

        assertEquals(DishStatus.ABORTED, dish1.status)
        assertEquals(DishStatus.EATEN, dish2.status)

        assertEquals(DriverState.RETURNING, returningDriver.state)
        assertEquals(10, returningDriver.totalTripTicks)
    }

    @Test
    fun `processEating - covers all delivery eating scenarios`() {
        //  customer eating updates and delivery count increments across all deliveredAt and dish status combinations
        de.unisaarland.cs.se.selab.Time.tick = 10

        val groupNotDelivered = mock<CasualGroup>()
        val orderNotDelivered = mock<Order>()
        whenever(orderNotDelivered.deliveredAt).thenReturn(null)
        whenever(orderNotDelivered.areAllDishesEaten()).thenReturn(false)
        setupDeliveryGroupArrival(groupNotDelivered, orderNotDelivered, id = 1)

        val groupAllEaten = mock<CasualGroup>()
        val orderAllEaten = mock<Order>()
        whenever(orderAllEaten.deliveredAt).thenReturn(8)
        whenever(orderAllEaten.areAllDishesEaten()).thenReturn(true)
        setupDeliveryGroupArrival(groupAllEaten, orderAllEaten, id = 2)

        val groupJustDelivered = mock<CasualGroup>()
        val orderJustDelivered = mock<Order>()
        val dishServed = mock<Dish>()
        whenever(dishServed.status).thenReturn(DishStatus.SERVED)

        whenever(orderJustDelivered.deliveredAt).thenReturn(10)
        whenever(orderJustDelivered.areAllDishesEaten()).thenReturn(false, true)
        whenever(orderJustDelivered.dishes).thenReturn(listOf(dishServed, dishServed))
        setupDeliveryGroupArrival(groupJustDelivered, orderJustDelivered, id = 3)

        val groupOldDelivery = mock<CasualGroup>()
        val orderOldDelivery = mock<Order>()
        val dishNotServed = mock<Dish>()
        whenever(dishNotServed.status).thenReturn(DishStatus.EATEN)

        whenever(orderOldDelivery.deliveredAt).thenReturn(9)
        whenever(orderOldDelivery.areAllDishesEaten()).thenReturn(false, false)
        whenever(orderOldDelivery.dishes).thenReturn(listOf(dishNotServed))
        setupDeliveryGroupArrival(groupOldDelivery, orderOldDelivery, id = 4)

        foh.processEating()

        verify(orderNotDelivered, never()).dishes
        verify(orderAllEaten, never()).dishes

        assertEquals(2, foh.numberOfCustomersDelivered)
        verify(dishServed, times(2)).updateEating()
        verify(dishNotServed, never()).updateEating()
    }

    private fun setupDeliveryGroupArrival(group: CasualGroup, order: Order, id: Int) {
        whenever(group.id).thenReturn(id)
        whenever(group.wantsDelivery).thenReturn(true)
        whenever(group.currentOrder).thenReturn(order)
        whenever(group.placeOrder(any(), any(), any())).thenReturn(true)
        foh.processArrival(group, emptyList())
    }

    @Test
    fun `serveDeliveryGroups - reuses already assigned driver and handles partial cooking handover`() {
        //  a partially cooked delivery order retains its assigned waiting driver during partial meal handoffs
        val waiter = Waiter().apply { id = 1 }
        val recipe = mock<Recipe> { whenever(it.name).thenReturn("Soup") }
        val dish = Dish(recipe).apply { status = DishStatus.COOKED }

        val group = mock<CasualGroup>()

        val mockOrder = mock<Order>()
        whenever(mockOrder.id).thenReturn(1)
        whenever(mockOrder.getServableDishes()).thenReturn(listOf(dish))
        whenever(mockOrder.areAllDishesCooked()).thenReturn(false)

        val driver = Driver().apply {
            state = DriverState.WAITING
            targetGroup = group
            currentOrder = mockOrder
            id = 1
        }
        drivers.add(driver)

        val localFoh = FrontOfHouse(
            tables = emptyList(),
            waiters = listOf(waiter),
            drivers = drivers,
            countertop = countertop
        )

        whenever(group.id).thenReturn(1)
        whenever(group.wantsDelivery).thenReturn(true)
        whenever(group.currentOrder).thenReturn(mockOrder)
        whenever(group.placeOrder(any(), any(), any())).thenReturn(true)

        localFoh.processArrival(group, emptyList())
        localFoh.processServing()

        assertEquals(DriverState.WAITING, driver.state)
        assertEquals(1, waiter.getTickLoad(ActionType.SERVE))
    }

    @Test
    fun `serveDeliveryGroups - no free driver and order becoming null`() {
        //  serving aborts when no idle drivers exist or when an order evaluates to null
        val busyDriver = Driver().apply { state = DriverState.DELIVERING }
        drivers.add(busyDriver)

        val localFoh = FrontOfHouse(
            tables = emptyList(),
            waiters = listOf(Waiter()),
            drivers = drivers,
            countertop = countertop
        )

        val mockOrder = mock<Order>()
        val dish = mock<Dish>()
        whenever(mockOrder.id).thenReturn(1)
        whenever(mockOrder.getServableDishes()).thenReturn(listOf(dish))
        whenever(mockOrder.areAllDishesCooked()).thenReturn(true)

        val groupNoDriver = mock<CasualGroup>()
        whenever(groupNoDriver.id).thenReturn(1)
        whenever(groupNoDriver.wantsDelivery).thenReturn(true)
        whenever(groupNoDriver.currentOrder).thenReturn(mockOrder)
        whenever(groupNoDriver.placeOrder(any(), any(), any())).thenReturn(true)
        localFoh.processArrival(groupNoDriver, emptyList())

        val groupNullOrder = mock<CasualGroup>()
        whenever(groupNullOrder.id).thenReturn(2)
        whenever(groupNullOrder.wantsDelivery).thenReturn(true)
        whenever(groupNullOrder.currentOrder).thenReturn(mockOrder, null)
        whenever(groupNullOrder.placeOrder(any(), any(), any())).thenReturn(true)
        localFoh.processArrival(groupNullOrder, emptyList())

        localFoh.processServing()

        assertEquals(DriverState.DELIVERING, busyDriver.state)
    }

    @Test
    fun `isReadyForHandOver - reject orders that are uncooked, empty, or null`() {
        //  delivery orders with uncooked dishes, empty dishes, or missing drivers are rejected from handover
        val localFoh = FrontOfHouse(
            tables = emptyList(),
            waiters = emptyList(),
            drivers = drivers,
            countertop = countertop
        )

        val groupEmptyDishes = mock<CasualGroup>()
        val orderEmptyDishes = mock<Order>()
        whenever(orderEmptyDishes.id).thenReturn(1)
        whenever(orderEmptyDishes.getServableDishes()).thenReturn(emptyList())
        setupDeliveryGroupArrival(groupEmptyDishes, orderEmptyDishes, id = 1)

        val groupUncooked = mock<CasualGroup>()
        val orderUncooked = mock<Order>()
        val dish = mock<Dish>()
        whenever(orderUncooked.id).thenReturn(2)
        whenever(orderUncooked.getServableDishes()).thenReturn(listOf(dish))
        whenever(orderUncooked.areAllDishesCooked()).thenReturn(false)
        setupDeliveryGroupArrival(groupUncooked, orderUncooked, id = 2)

        localFoh.processServing()

        assertTrue(drivers.isEmpty())
    }

    @Test
    fun `releaseStrandedDrivers - releases driver when order has aborted dishes`() {
        //  a waiting driver is released back to IDLE when any dish in their assigned order is aborted
        val strandedDriver = Driver().apply {
            state = DriverState.WAITING
            id = 1
        }

        val recipe = mock<Recipe>()
        val abortedDish = Dish(recipe).apply { status = DishStatus.ABORTED }
        val cookedDish = Dish(recipe).apply { status = DishStatus.COOKED }
        val order = Order(listOf(abortedDish, cookedDish))

        val casualGroup = mock<CasualGroup>()
        strandedDriver.currentOrder = order
        strandedDriver.targetGroup = casualGroup
        drivers.add(strandedDriver)

        foh.processDelivering()

        assertEquals(DriverState.IDLE, strandedDriver.state)
        assertNull(strandedDriver.currentOrder)
        assertNull(strandedDriver.targetGroup)
    }

    @Test
    fun `releaseStrandedDrivers - skips drivers with null order or without aborted dishes`() {
        // waiting drivers without aborted dishes or with null orders remain waiting
        val driverNullOrder = Driver().apply {
            state = DriverState.WAITING
            currentOrder = null
        }

        val recipe = mock<Recipe>()
        val normalDish = Dish(recipe).apply { status = DishStatus.COOKED }
        val orderNormal = Order(listOf(normalDish))

        val driverNormalOrder = Driver().apply {
            state = DriverState.WAITING
            currentOrder = orderNormal
        }

        drivers.add(driverNullOrder)
        drivers.add(driverNormalOrder)

        foh.processDelivering()

        assertEquals(DriverState.WAITING, driverNullOrder.state)
        assertEquals(DriverState.WAITING, driverNormalOrder.state)
    }

    @Test
    fun `prepareDelivery - safely returns when driver fields are missing`() {
        // delivery preparation returns early without state changes when driver attributes are missing
        val driverNoId = Driver().apply {
            state = DriverState.WAITING
            id = null
            val mockOrder = mock<Order>()
            whenever(mockOrder.areAllDishesServed()).thenReturn(true)
            currentOrder = mockOrder
            targetGroup = mock<CasualGroup>()
        }

        val driverNoGroup = Driver().apply {
            state = DriverState.WAITING
            id = 2
            val mockOrder = mock<Order>()
            whenever(mockOrder.areAllDishesServed()).thenReturn(true)
            currentOrder = mockOrder
            targetGroup = null
        }

        val driverNoOrder = Driver().apply {
            state = DriverState.WAITING
            id = 3
            currentOrder = null
            targetGroup = mock<CasualGroup>()
        }

        drivers.add(driverNoId)
        drivers.add(driverNoGroup)
        drivers.add(driverNoOrder)

        foh.processDelivering()

        assertEquals(DriverState.WAITING, driverNoId.state)
        assertEquals(DriverState.WAITING, driverNoGroup.state)
        assertEquals(DriverState.WAITING, driverNoOrder.state)
    }

    @Test
    fun `prepareDelivery - successfully prepares delivery for valid waiting driver`() {
        // a valid waiting driver with a fully served order transitions to DELIVERING with correct trip calculations.
        val casualGroup = mock<CasualGroup>()
        whenever(casualGroup.id).thenReturn(10)
        whenever(casualGroup.deliveryDistance).thenReturn(15)

        val mockOrder = mock<Order>()
        whenever(mockOrder.id).thenReturn(20)
        whenever(mockOrder.areAllDishesServed()).thenReturn(true)

        val readyDriver = Driver().apply {
            state = DriverState.WAITING
            id = 1
            targetGroup = casualGroup
            currentOrder = mockOrder
        }
        drivers.add(readyDriver)

        foh.processDelivering()

        assertEquals(DriverState.DELIVERING, readyDriver.state)
        assertEquals(15, readyDriver.tripDistance)
        assertEquals(3, readyDriver.ticksToDest)
        assertEquals(6, readyDriver.totalTripTicks)
    }
}
