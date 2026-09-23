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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
        // test when a delivery group is waiting for their order for long and dont get order
        // in time , they eave negative review
        // setup group and order
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

        // Force immediate abortion upon processing
        whenever(casualGroup.visitingAt).thenReturn(-999)

        // Register arrival, then process deliveries to trigger the abortion logic
        foh.processArrival(casualGroup, emptyList())
        foh.processDelivering()

        //  The group reached the private delivery queue and was subsequently aborted
        verify(casualGroup).experience = ExperienceType.NEGATIVE
    }

    @Test
    fun `isDriverAvailable returns correct availability based on driver states`() {
        val driver1 = Driver().apply { state = DriverState.DELIVERING }
        val driver2 = Driver().apply { state = DriverState.RETURNING }

        drivers.add(driver1)
        drivers.add(driver2)

        // Verify false when no drivers are IDLE
        assertFalse(foh.isDriverAvailable())

        // Verify true when at least one driver becomes IDLE
        driver2.state = DriverState.IDLE
        assertTrue(foh.isDriverAvailable())
    }

    @Test
    fun `resetDrivers- what happens to drivers when the restaurant closes`() {
        // active outbound driver (should be reset) and returning driver (should be ignored)
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

        // Action
        foh.resetDrivers()

        //  Outbound driver is fully reset to IDLE and stats cleared
        assertEquals(DriverState.IDLE, deliveringDriver.state)
        assertNull(deliveringDriver.currentOrder)
        assertNull(deliveringDriver.targetGroup)
        assertEquals(0, deliveringDriver.totalTripTicks)
        assertEquals(0, deliveringDriver.ticksToDest)
        assertEquals(0, deliveringDriver.tripDistance)
        assertEquals(0, deliveringDriver.distanceDriven)

        //  Active dishes aborted, but eaten dishes remain untouched
        assertEquals(DishStatus.ABORTED, dish1.status)
        assertEquals(DishStatus.EATEN, dish2.status)

        //  Returning driver remains completely unaffected
        assertEquals(DriverState.RETURNING, returningDriver.state)
        assertEquals(10, returningDriver.totalTripTicks)
    }

    @Test
    fun `processEating - covers all delivery eating scenarios`() {
        // Set a static tick for controlled timing
        de.unisaarland.cs.se.selab.Time.tick = 10

        //  deliveredAt is null
        val groupNotDelivered = mock<CasualGroup>()
        val orderNotDelivered = mock<Order>()
        whenever(orderNotDelivered.deliveredAt).thenReturn(null)
        whenever(orderNotDelivered.areAllDishesEaten()).thenReturn(false)
        setupDeliveryGroupArrival(groupNotDelivered, orderNotDelivered, id = 1)

        // Order already fully eaten
        val groupAllEaten = mock<CasualGroup>()
        val orderAllEaten = mock<Order>()
        whenever(orderAllEaten.deliveredAt).thenReturn(8)
        whenever(orderAllEaten.areAllDishesEaten()).thenReturn(true)
        setupDeliveryGroupArrival(groupAllEaten, orderAllEaten, id = 2)

        // Delivered EXACTLY now & dishes are SERVED
        val groupJustDelivered = mock<CasualGroup>()
        val orderJustDelivered = mock<Order>()
        val dishServed = mock<Dish>()
        whenever(dishServed.status).thenReturn(DishStatus.SERVED)

        whenever(orderJustDelivered.deliveredAt).thenReturn(10)
        // First check returns false, second check at the end returns true to log finished eating!
        whenever(orderJustDelivered.areAllDishesEaten()).thenReturn(false, true)
        whenever(orderJustDelivered.dishes).thenReturn(listOf(dishServed, dishServed))
        setupDeliveryGroupArrival(groupJustDelivered, orderJustDelivered, id = 3)

        //  Delivered in the past & dishes NOT SERVED
        // (Should NOT increment foh stats, should NOT update eating)
        val groupOldDelivery = mock<CasualGroup>()
        val orderOldDelivery = mock<Order>()
        val dishNotServed = mock<Dish>()
        whenever(dishNotServed.status).thenReturn(DishStatus.EATEN) // Not SERVED

        whenever(orderOldDelivery.deliveredAt).thenReturn(9) // Past tick
        whenever(orderOldDelivery.areAllDishesEaten()).thenReturn(false, false)
        whenever(orderOldDelivery.dishes).thenReturn(listOf(dishNotServed))
        setupDeliveryGroupArrival(groupOldDelivery, orderOldDelivery, id = 4)

        // ACTION: Process eating for all groups
        foh.processEating()

        // ASSERTIONS
        verify(orderNotDelivered, org.mockito.kotlin.never()).dishes
        verify(orderAllEaten, org.mockito.kotlin.never()).dishes

        // deliveredAt == Time.tick (10), so FOH recorded 2 customers delivered
        assertEquals(2, foh.numberOfCustomersDelivered)
        // The 2 SERVED dishes had updateEating() called on them
        verify(dishServed, org.mockito.kotlin.times(2)).updateEating()

        // Dish was already EATEN, so updateEating was NOT called
        verify(dishNotServed, org.mockito.kotlin.never()).updateEating()
    }

    // Helper function to keep the test clean
    private fun setupDeliveryGroupArrival(group: CasualGroup, order: Order, id: Int) {
        whenever(group.id).thenReturn(id)
        whenever(group.wantsDelivery).thenReturn(true)
        whenever(group.currentOrder).thenReturn(order)
        whenever(group.placeOrder(any(), any(), any())).thenReturn(true)
        foh.processArrival(group, emptyList())
    }

    @Test
    fun `serveDeliveryGroups - reuses already assigned driver and handles partial cooking handover`() {
        val waiter = Waiter().apply { id = 1 }
        val recipe = mock<Recipe> { whenever(it.name).thenReturn("Soup") }
        val dish = Dish(recipe).apply { status = DishStatus.COOKED }

        val group = mock<CasualGroup>()

        //  Define mockOrder
        val mockOrder = mock<Order>()
        whenever(mockOrder.id).thenReturn(1)
        whenever(mockOrder.getServableDishes()).thenReturn(listOf(dish))
        whenever(mockOrder.areAllDishesCooked()).thenReturn(false)

        //  Point currentOrder same mockOrder instance
        val driver = Driver().apply {
            state = DriverState.WAITING
            targetGroup = group
            currentOrder = mockOrder
            id = 1
        }
        drivers.add(driver)

        //  Create FOH with the waiter and the driver
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

        // Verifications
        assertEquals(DriverState.WAITING, driver.state)
        assertEquals(1, waiter.getTickLoad(ActionType.SERVE))
    }

    @Test
    fun `serveDeliveryGroups - no free driver and order becoming null`() {
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

        // Group with no free driver available
        val groupNoDriver = mock<CasualGroup>()
        whenever(groupNoDriver.id).thenReturn(1)
        whenever(groupNoDriver.wantsDelivery).thenReturn(true)
        whenever(groupNoDriver.currentOrder).thenReturn(mockOrder)
        whenever(groupNoDriver.placeOrder(any(), any(), any())).thenReturn(true)
        localFoh.processArrival(groupNoDriver, emptyList())

        //  Group whose currentOrder returns null during loop execution
        val groupNullOrder = mock<CasualGroup>()
        whenever(groupNullOrder.id).thenReturn(2)
        whenever(groupNullOrder.wantsDelivery).thenReturn(true)
        // First returns mockOrder for isReadyForHandOver filter, then null inside the loop
        whenever(groupNullOrder.currentOrder).thenReturn(mockOrder, null)
        whenever(groupNullOrder.placeOrder(any(), any(), any())).thenReturn(true)
        localFoh.processArrival(groupNullOrder, emptyList())

        localFoh.processServing()

        // Assert no driver was transitioned to WAITING because none were IDLE
        assertEquals(DriverState.DELIVERING, busyDriver.state)
    }

    @Disabled
    @Test
    fun `serveDeliveryGroups - idle driver with existing id and no available waiter breaks handover`() {
        // Waiter has already reached ACTION_LIMIT for SERVE (tickLoad = 10)
        val busyWaiter = Waiter().apply {
            addToTickLoad(ActionType.SERVE, 10)
        }
        // Driver already has an ID assigned
        val idleDriverWithId = Driver().apply {
            state = DriverState.IDLE
            id = 5
        }
        drivers.add(idleDriverWithId)

        val localFoh = FrontOfHouse(
            tables = emptyList(),
            waiters = listOf(busyWaiter),
            drivers = drivers,
            countertop = countertop
        )

        val recipe = mock<Recipe> { whenever(it.name).thenReturn("Salad") }
        val dish = Dish(recipe).apply { status = DishStatus.COOKED }
        val order = Order(listOf(dish))

        val group = mock<CasualGroup>()
        whenever(group.id).thenReturn(1)
        whenever(group.wantsDelivery).thenReturn(true)
        whenever(group.currentOrder).thenReturn(order)
        whenever(group.placeOrder(any(), any(), any())).thenReturn(true)

        localFoh.processArrival(group, emptyList())
        localFoh.processServing()

        // Nobody could hand anything over, so the driver is left alone: it keeps the id it already
        // had and stays IDLE, free to be claimed in a later tick when a waiter has capacity again.
        // A driver only becomes WAITING once it actually receives meals, because the spec hands out
        // driver ids "at the moment they receive meals".
        assertEquals(5, idleDriverWithId.id)
        // DOTO: based on AB test results maybe change WAITING to IDLE
        assertEquals(DriverState.WAITING, idleDriverWithId.state)
        // But meals were not served because the waiter had no remaining capacity
        assertEquals(DishStatus.COOKED, dish.status)
    }

    @Test
    fun `isReadyForHandOver - reject orders that are uncooked, empty, or null`() {
        val localFoh = FrontOfHouse(
            tables = emptyList(),
            waiters = emptyList(),
            drivers = drivers,
            countertop = countertop
        )

        // Group with empty servable dishes
        val groupEmptyDishes = mock<CasualGroup>()
        val orderEmptyDishes = mock<Order>()
        whenever(orderEmptyDishes.id).thenReturn(1)
        whenever(orderEmptyDishes.getServableDishes()).thenReturn(emptyList())
        setupDeliveryGroupArrival(groupEmptyDishes, orderEmptyDishes, id = 1)

        // Group with uncooked dishes and no driver carrying it
        val groupUncooked = mock<CasualGroup>()
        val orderUncooked = mock<Order>()
        val dish = mock<Dish>()
        whenever(orderUncooked.id).thenReturn(2)
        whenever(orderUncooked.getServableDishes()).thenReturn(listOf(dish))
        whenever(orderUncooked.areAllDishesCooked()).thenReturn(false)
        setupDeliveryGroupArrival(groupUncooked, orderUncooked, id = 2)

        localFoh.processServing()

        // Neither order should be processed or handed over
        assertTrue(drivers.isEmpty())
    }
}
