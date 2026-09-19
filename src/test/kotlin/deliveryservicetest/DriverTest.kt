package deliveryservicetest

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.Logger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DriverTest {

    @BeforeEach
    fun setup() {
        Time.tick = 0
        Logger.setup(LogLevel.DEBUG)
    }

    private fun setupDriver(state: DriverState, ticksToDest: Int): Driver {
        return Driver().apply {
            id = 1
            this.state = state
            targetGroup = CasualGroup(
                id = 1, size = 2, tableType = TableType.COMMON, visitingAt = 10,
                foodPreferences = emptyList(), restaurantTypes = emptyList(),
                visitingEvenings = listOf(1), deliveryDistance = 5, ratingLikelihood = RatingLikelihood.NEVER
            )
            currentOrder = Order(emptyList())
            this.ticksToDest = ticksToDest
            totalTripTicks = 10
            tripDistance = 5
            distanceDriven = 0
        }
    }

    // Null / Early Return Branch Tests

    @Test
    fun `driver - return early when id is null`() {
        val driver = setupDriver(DriverState.DELIVERING, 5)
        driver.id = null

        // should abort when id is null
        driver.driveTowardsCustomer()
        driver.logArrival()
        driver.handOverToCustomer()

        // Assert nothing changed, driver still drives to the customer, logs nothing delivered and comes to restaurant
        assertEquals(5, driver.ticksToDest)
        assertEquals(DriverState.DELIVERING, driver.state)
    }

    @Test
    fun `logArrival and handOverToCustomer - when targetGroup is null`() {
        val driver = setupDriver(DriverState.DELIVERING, 5)

        // Remove the target group to trigger the early return safety check
        driver.targetGroup = null
        driver.logArrival()
        driver.handOverToCustomer()

        // driver still drives to the customer, logs nothing delivered and comes to restaurant
        assertEquals(DriverState.DELIVERING, driver.state)
    }

    @Test
    fun `logArrival and handOverToCustomer - when currentOrder is null`() {
        val driver = setupDriver(DriverState.DELIVERING, 5)

        // Remove the order to trigger the early return safety check
        driver.currentOrder = null
        driver.logArrival()
        driver.handOverToCustomer()

        // driver still drives to the customer, logs nothing delivered and comes to restaurant
        assertEquals(DriverState.DELIVERING, driver.state)
    }

    @Test
    fun `driveTowardsCustomer- if ticksToDest is zero or less`() {
        val driver = setupDriver(DriverState.DELIVERING, 0)

        driver.driveTowardsCustomer()

        // Ticks and distance should remain completely untouched
        assertEquals(0, driver.ticksToDest)
        assertEquals(0, driver.distanceDriven)
    }

    @Test
    fun `driveTowardsRestaurant- if ticksToDest is zero or less`() {
        val driver = setupDriver(DriverState.RETURNING, 0)

        driver.driveTowardsRestaurant()

        assertEquals(0, driver.ticksToDest)
    }

    // Edge Cases

    @Test
    fun `handOverToCustomer - pre-existing NEGATIVE experience is maintained regardless of arrival time`() {
        val driver = setupDriver(DriverState.DELIVERING, 0)
        val group = driver.targetGroup as? CasualGroup ?: error("targetGroup is null or wrong type")

        // Group is already mad
        group.experience = ExperienceType.NEGATIVE

        // Arrive early
        Time.tick = 1

        driver.handOverToCustomer()

        // Experience must stay NEGATIVE
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertEquals(DriverState.RETURNING, driver.state)
    }

    // The Return Trip

    @Test
    fun `finishReturnTrip - reset all driver stats to idle`() {
        val driver = setupDriver(DriverState.RETURNING, 0)

        driver.finishReturnTrip()

        assertEquals(DriverState.IDLE, driver.state)
        assertNull(driver.currentOrder)
        assertNull(driver.targetGroup)
        assertEquals(0, driver.totalTripTicks)
        assertEquals(0, driver.ticksToDest)
        assertEquals(0, driver.tripDistance)
        assertEquals(0, driver.distanceDriven)
    }

    @Test
    fun `finishReturnTrip- even if id is null`() {
        val driver = setupDriver(DriverState.RETURNING, 0)

        // somehow magically
        driver.id = null

        driver.finishReturnTrip()

        // still he is resetted to normal
        assertEquals(DriverState.IDLE, driver.state)
        assertNull(driver.currentOrder)
        assertNull(driver.targetGroup)
        assertEquals(0, driver.totalTripTicks)
        assertEquals(0, driver.ticksToDest)
        assertEquals(0, driver.tripDistance)
        assertEquals(0, driver.distanceDriven)
    }
}
