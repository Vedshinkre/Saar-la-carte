package eveningclosetests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import eveningclosetests.EveningCloseFixtures.cook
import eveningclosetests.EveningCloseFixtures.deliveringDriver
import eveningclosetests.EveningCloseFixtures.regularGroup
import eveningclosetests.EveningCloseFixtures.restaurant
import eveningclosetests.EveningCloseFixtures.slowMenu
import eveningclosetests.EveningCloseFixtures.stock
import eveningclosetests.EveningCloseFixtures.table
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * F30 tests for Restaurant.endEvening
 */
class ClosingCompositionTest {

    private lateinit var output: StringWriter
    private val closedEarlyOpeningTickEnd = 15

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
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

    /** clears the captured output before the call, so the returned lines belong only to [tick] */
    private fun simulateTickAtIsolated(tick: Int, restaurant: Restaurant): List<String> {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Time.tick = tick
        restaurant.simulateTick()
        return output.toString().lines().filter { it.isNotBlank() }
    }

    @Test
    fun `openingTickEnd 24 - both the opening-time reset and the driver reset happen in one call`() {
        // ordering 3 ticks before closing, well inside patience (Constants.UNSERVED_WAIT_TICKS - 1 = 4),
        // so it's the force-escort at closing that removes the group, not the normal patience timeout
        val orderingTick = 21
        val group = regularGroup(id = 1, visitingAt = orderingTick)
        val driver = deliveringDriver()
        val staff = RestaurantStaff(mutableListOf(cook()), mutableListOf(Waiter()), mutableListOf(driver))
        val restaurant = restaurant(
            openingTickStart = 1,
            openingTickEnd = 24,
            staff = staff,
            tables = listOf(table(1), table(2)),
            menu = slowMenu,
            stock = stock()
        )

        restaurant.prepareForEvening(listOf(group))
        simulateTickAtIsolated(orderingTick, restaurant)
        assertEquals(2, group.customersRemainingInRestaurant, "group should still be seated before closing")

        simulateTickAtIsolated(24, restaurant)

        // opening-time reset: the still mid-meal group was force-escorted out
        // (checking that endOfOpeningTime ran alongside endEvening)
        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(
            ExperienceType.NEGATIVE,
            group.experience
        ) // being forced out by the clock is not one of the spec's "failed attempt" reasons
        assertEquals(0, group.failedAttempts) // kitchen reset: the still-cooking meal is thrown away, not left dangling
        assertEquals(
            DishStatus.ABORTED,
            group.currentOrder?.dishes?.first()?.status
        ) // evening reset: the driver was aborted and sent home
        assertEquals(DriverState.IDLE, driver.state)
    }

    @Test
    fun `openingTickEnd less than 24 - the opening-time reset does not re-fire at tick 24`() {
        val driver = deliveringDriver()
        val staff = RestaurantStaff(mutableListOf(), mutableListOf(), mutableListOf(driver))
        val restaurant = restaurant(openingTickStart = 1, openingTickEnd = closedEarlyOpeningTickEnd, staff = staff)

        val closingTickLog = simulateTickAtIsolated(closedEarlyOpeningTickEnd, restaurant)
        assertTrue(closingTickLog.any { it.contains("Kitchen Status") }, "expected the real closing tick to log")

        val eveningBoundaryLog = simulateTickAtIsolated(24, restaurant)

        assertFalse(eveningBoundaryLog.any { it.contains("Kitchen Status") }, eveningBoundaryLog.toString())
        assertFalse(
            eveningBoundaryLog.any { it.contains("FOH Escorting") },
            eveningBoundaryLog.toString()
        ) // the evening reset itself still ran
        assertEquals(DriverState.IDLE, driver.state)
    }
}
