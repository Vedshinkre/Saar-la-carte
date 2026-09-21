package eveningclosetests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.Logger
import eveningclosetests.EveningCloseFixtures.regularGroup
import eveningclosetests.EveningCloseFixtures.restaurant
import eveningclosetests.EveningCloseFixtures.slowRecipe
import eveningclosetests.EveningCloseFixtures.table
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * F30 integration tests that span two evenings, checking if the evening end (not just opening-time end)
 * leaves the restaurant in a clean state for the next one
 */
class EveningToEveningIntegrationTest {

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

    private fun walkInGroup(id: Int) = CasualGroup(
        id = id,
        size = 2,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1, 2),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    @Test
    fun `a DELIVERING driver is aborted at evening's end, a RETURNING one finishes its trip in evening 2`() {
        val order = Order(listOf(Dish(slowRecipe).apply { status = DishStatus.COOKED }))
        val stillOutDriver = Driver().apply {
            id = 1
            state = DriverState.DELIVERING
            currentOrder = order
            ticksToDest = EveningCloseFixtures.LARGE_TRIP
        }
        val onTheWayHomeDriver = EveningCloseFixtures.returningDriver(id = 2, ticksToDest = 2)
        val staff = RestaurantStaff(mutableListOf(), mutableListOf(), mutableListOf(stillOutDriver, onTheWayHomeDriver))
        val restaurant = restaurant(openingTickStart = 1, openingTickEnd = 24, staff = staff)

        // evening 1 ends
        Time.tick = 24
        restaurant.simulateTick()

        assertEquals(DriverState.IDLE, stillOutDriver.state)
        assertNull(stillOutDriver.currentOrder)
        assertEquals(DishStatus.ABORTED, order.dishes.single().status)
        // still on its way home, one leg short of the restaurant
        assertEquals(DriverState.RETURNING, onTheWayHomeDriver.state)

        // evening 2's serving phase: the returning driver's trip resumes and completes here
        Time.evening = 2
        Time.tick = 1
        restaurant.simulateTick()

        assertEquals(DriverState.IDLE, onTheWayHomeDriver.state)
    }

    @Test
    fun `cook, waiter and driver ids all reset together, and reassignment in evening 2 restarts at 1`() {
        val cook = Cook(CookType.TOURNANT).apply { id = 5 }
        val waiter = Waiter().apply { id = 3 }
        val driver = Driver().apply {
            id = 7
            state = DriverState.DELIVERING
        }
        val staff = RestaurantStaff(mutableListOf(cook), mutableListOf(waiter), mutableListOf(driver))
        val restaurant = restaurant(openingTickStart = 1, openingTickEnd = 24, staff = staff)

        // evening 1 ends: FOH's waiter reset, the kitchen's cook reset and the driver reset all
        // execute this single call (endOfOpeningTime + endEvening at tick 24)
        Time.tick = 24
        restaurant.simulateTick()

        assertNull(cook.id, "cook id should be cleared by the kitchen reset")
        assertNull(waiter.id, "waiter id should be cleared by the FOH reset")
        assertNull(driver.id, "driver id should be cleared by the driver reset")

        // evening 2: a fresh arrival shows the waiter id counter restarted at 1, not only that old id was cleared
        Time.evening = 2
        Time.tick = 1
        restaurant.addToCustomerQueue(walkInGroup(id = 1))
        restaurant.simulateTick()

        assertEquals(1, waiter.id)
    }

    @Test
    fun `a table occupied at evening 1's close is free and reservable for evening 2's REGULAR pass`() {
        val sharedTable = table(id = 1)
        val staff = RestaurantStaff(mutableListOf(), mutableListOf(Waiter()), mutableListOf())
        val restaurant = restaurant(
            openingTickStart = 1,
            openingTickEnd = 24,
            staff = staff,
            tables = listOf(sharedTable)
        )
        val evening1Group = regularGroup(id = 1)

        restaurant.prepareForEvening(listOf(evening1Group))
        assertEquals(TableStatus.RESERVED, sharedTable.status)

        Time.tick = 1
        restaurant.simulateTick()
        // seated (a waiter is assigned) even though the restaurant's empty menu means it can never
        // order; a REGULAR's table is only freed by the closing reset, unlike a CASUAL's
        assertEquals(TableStatus.RESERVED, sharedTable.status)

        Time.tick = 24
        restaurant.simulateTick()
        assertEquals(TableStatus.FREE, sharedTable.status)

        // evening 2's reservation pass can now reserve the same table for a new REGULAR group
        Time.evening = 2
        val evening2Group = regularGroup(id = 2)
        restaurant.prepareForEvening(listOf(evening2Group))

        assertEquals(TableStatus.RESERVED, sharedTable.status)
    }
}
