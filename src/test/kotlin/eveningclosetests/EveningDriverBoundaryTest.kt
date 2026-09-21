package eveningclosetests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import eveningclosetests.EveningCloseFixtures.deliveringDriver
import eveningclosetests.EveningCloseFixtures.restaurant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * tests if resetDrivers executes always at `tick == 24` by [Restaurant.simulateTick],
 * independent of a restaurant's own `openingTickEnd`
 */
class EveningDriverBoundaryTest {

    private val closedEarlyOpeningTickEnd = 15

    @BeforeTest
    fun setUp() {
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.evening = 1
        Time.ticksElapsed = 0
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.ticksElapsed = 0
    }

    private fun simulateTickAt(tick: Int, restaurant: Restaurant) {
        Time.tick = tick
        restaurant.simulateTick()
    }

    @Test
    fun `tick 24 resets the driver even though the restaurant's own hours ended long before`() {
        val driver = deliveringDriver()
        val staff = RestaurantStaff(mutableListOf(), mutableListOf(), mutableListOf(driver))
        val restaurant = restaurant(openingTickStart = 1, openingTickEnd = closedEarlyOpeningTickEnd, staff = staff)

        simulateTickAt(24, restaurant)

        assertEquals(DriverState.IDLE, driver.state)
    }

    @Test
    fun `tick 23 does not reset the driver yet`() {
        val driver = deliveringDriver()
        val staff = RestaurantStaff(mutableListOf(), mutableListOf(), mutableListOf(driver))
        val restaurant = restaurant(openingTickStart = 1, openingTickEnd = closedEarlyOpeningTickEnd, staff = staff)

        simulateTickAt(23, restaurant)

        assertEquals(DriverState.DELIVERING, driver.state)
    }
}
