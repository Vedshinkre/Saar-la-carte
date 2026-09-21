package deliveryservicetest

import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
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
}
