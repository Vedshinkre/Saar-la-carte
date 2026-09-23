package browsingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * integration tests for F28 (Browsing Service)
 * event seat estimate using an actual [Restaurant] and its tables
 * casual seat and driver estimates tested by Deniz
 */
class EventBrowsingSeatEstimateIntegrationTest {
    private val water = Ingredient("water", MeasurementUnit.ML, bestBefore = 9, initialPackagingVolume = 1000)
    private val soup = Recipe(
        1,
        "soup",
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(water to 1),
        basicDishFor = RestaurantType.EUROPEAN
    )

    @BeforeTest
    fun setUp() {
        Logger.setup(PrintWriter(StringWriter()))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
        Time.evening = 1
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
    }

    private fun stats(id: Int, positiveRatings: Int = 0, event: Boolean = true) = RestaurantStats(
        restaurantId = id,
        restaurantType = RestaurantType.EUROPEAN,
        openingTickStart = 1,
        openingTickEnd = 24,
        event = event,
        positiveRatings = positiveRatings,
        negativeRatings = 0,
        menu = listOf(soup)
    )

    private fun restaurant(stats: RestaurantStats, tableSizes: List<Int>): Pair<Restaurant, List<Table>> {
        val tables = tableSizes.mapIndexed { index, size -> Table(index + 1, size, TableType.COMMON) }
        val staff = RestaurantStaff(
            cooks = mutableListOf(Cook(CookType.TOURNANT)),
            waiters = mutableListOf(Waiter()),
            drivers = mutableListOf()
        )
        return Restaurant(stats, "Soup Kitchen ${stats.restaurantId}", staff, tables, Stock(listOf(water))) to tables
    }

    private fun event(id: Int, size: Int, eventEvening: Int = 4) = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = eventEvening,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "soup")
    )

    /** the event estimate is published from the restaurant's actual free tables */
    @Test
    fun `the event seat estimate comes from the restaurant's real tables`() {
        val stats = stats(1)
        val (restaurant, _) = restaurant(stats, listOf(4, 6))

        restaurant.prepareForEvening(emptyList())

        assertEquals(10, stats.availableEventSeats[TableType.COMMON])
        assertEquals(1, BrowsingService(listOf(stats)).getEligibleRestaurants(event(1, size = 8)))
    }

    /** a restaurant that does not host events publishes no event seats at all */
    @Test
    fun `a restaurant that does not host events publishes no event seats`() {
        val stats = stats(1, event = false)
        val (restaurant, _) = restaurant(stats, listOf(4, 6))

        restaurant.prepareForEvening(emptyList())

        assertEquals(null, stats.availableEventSeats[TableType.COMMON])
        assertNull(BrowsingService(listOf(stats)).getEligibleRestaurants(event(1, size = 4)))
    }

    /** the first booking consumes the estimate the next event group sees */
    @Test
    fun `a booking reduces the estimate the next event group browses on`() {
        val stats = stats(1)
        val (restaurant, _) = restaurant(stats, listOf(6, 6))
        restaurant.prepareForEvening(emptyList())
        val service = BrowsingService(listOf(stats))

        assertEquals(1, service.getEligibleRestaurants(event(1, size = 8)))
        assertEquals(4, stats.availableEventSeats[TableType.COMMON])
        assertNull(service.getEligibleRestaurants(event(2, size = 6)))
        assertEquals(1, service.getEligibleRestaurants(event(3, size = 4)))
    }

    /** a booking made through browsing becomes a real table reservation on the event evening */
    @Test
    fun `a booked event group gets real tables reserved on its event evening`() {
        val stats = stats(1)
        val (restaurant, tables) = restaurant(stats, listOf(4, 4))
        restaurant.prepareForEvening(emptyList())
        val group = event(1, size = 6)

        assertEquals(1, BrowsingService(listOf(stats)).getEligibleRestaurants(group))
        // the decision step hands the group to the restaurant and records the type it picked
        group.currentRestaurantType = RestaurantType.EUROPEAN
        restaurant.eventCustomers.add(group)

        Time.evening = 4
        restaurant.prepareForEvening(emptyList())

        assertEquals(listOf(TableStatus.RESERVED, TableStatus.RESERVED), tables.map { it.status })
        assertEquals(0, stats.availableEventSeats[TableType.COMMON], "the reserved tables are no longer free")
    }

    /** with several restaurants the group books the best rated one that has the seats */
    @Test
    fun `an event group books the best rated restaurant that has real room for it`() {
        val bestButSmall = stats(1, positiveRatings = 9)
        val roomy = stats(2, positiveRatings = 3)
        restaurant(bestButSmall, listOf(4)).first.prepareForEvening(emptyList())
        restaurant(roomy, listOf(6, 6)).first.prepareForEvening(emptyList())

        val chosen = BrowsingService(listOf(bestButSmall, roomy)).getEligibleRestaurants(event(1, size = 8))

        assertEquals(2, chosen)
        assertEquals(4, bestButSmall.availableEventSeats[TableType.COMMON], "the small one keeps its estimate")
        assertEquals(4, roomy.availableEventSeats[TableType.COMMON])
    }

    /** event bookings do not affect the walk-in estimate of the same real restaurant */
    @Test
    fun `an event booking leaves the walk-in estimate of the real restaurant alone`() {
        val stats = stats(1)
        val (restaurant, _) = restaurant(stats, listOf(6, 6))
        restaurant.prepareForEvening(emptyList())

        assertEquals(1, BrowsingService(listOf(stats)).getEligibleRestaurants(event(1, size = 8)))

        assertEquals(4, stats.availableEventSeats[TableType.COMMON])
        assertEquals(12, stats.availableSeats[TableType.COMMON], "the dining room estimate is untouched")
    }
}
