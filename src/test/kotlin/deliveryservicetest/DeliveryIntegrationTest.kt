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
}
