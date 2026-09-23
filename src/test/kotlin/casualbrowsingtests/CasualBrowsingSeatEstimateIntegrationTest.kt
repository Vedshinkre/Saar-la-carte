package casualbrowsingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the integration of the eligibility criteria of the BrowsingService for CasualGroups.
 */
class CasualBrowsingSeatEstimateIntegrationTest {
    private val water = Ingredient("water", MeasurementUnit.ML, 9, 1000)
    private val soup = Recipe(
        1,
        "soup",
        10,
        listOf(CookType.TOURNANT),
        mutableMapOf(water to 1),
        RestaurantType.EUROPEAN
    )

    @BeforeTest
    fun setUp() {
        Logger.setup(PrintWriter(StringWriter()))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
        Time.evening = 1
    }

    private fun restaurantStats(id: Int) = RestaurantStats(
        id,
        RestaurantType.EUROPEAN,
        1,
        24,
        false,
        0,
        0,
        listOf(soup)
    )

    private fun restaurant(
        restaurantStats: RestaurantStats,
        tableSizes: List<Int>,
        drivers: Int = 0
    ): Pair<Restaurant, List<Table>> {
        val tables = tableSizes.mapIndexed { index, size -> Table(index + 1, size, TableType.COMMON) }
        val staff = RestaurantStaff(
            mutableListOf(Cook(CookType.TOURNANT)),
            mutableListOf(Waiter()),
            MutableList(drivers) { Driver() }
        )
        return Pair(Restaurant(restaurantStats, "Soup Kitchen", staff, tables, Stock(listOf(water))), tables)
    }

    private fun casualGroup(id: Int, size: Int, distance: Int = 0) = CasualGroup(
        id,
        size,
        TableType.COMMON,
        5,
        List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        listOf(RestaurantType.EUROPEAN),
        listOf(1),
        distance,
        RatingLikelihood.NEVER
    )

    private fun regularGroup(id: Int, size: Int) = RegularGroup(
        id,
        size,
        TableType.COMMON,
        5,
        List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        1,
        1,
        id
    )

    @Test
    fun `Available Seat Estimate Is Correct`() {
        val restaurantStats = restaurantStats(1)
        val (restaurant, _) = restaurant(restaurantStats, listOf(4, 6))
        val browser = BrowsingService(listOf(restaurantStats))

        restaurant.prepareForEvening(emptyList())

        assertEquals(10, restaurantStats.availableSeats[TableType.COMMON])
        assertEquals(1, browser.getEligibleRestaurants(casualGroup(1, size = 6)))
    }

    @Test
    fun `Available Seat Estimate Takes Reservation Into Account`() {
        val restaurantStats = restaurantStats(1)
        val (restaurant, tables) = restaurant(restaurantStats, listOf(4, 4))
        val browser = BrowsingService(listOf(restaurantStats))

        restaurant.prepareForEvening(listOf(regularGroup(1, 4)))

        assertEquals(4, restaurantStats.availableSeats[TableType.COMMON])
        assertEquals(1, tables.count { it.status == TableStatus.RESERVED })
        assertEquals(1, browser.getEligibleRestaurants(casualGroup(2, size = 4)))
    }

    @Test
    fun `Available Seat Estimate Is Taken Into Account For Selection`() {
        val restaurantStats = restaurantStats(1)
        val (restaurant, _) = restaurant(restaurantStats, listOf(4, 4))
        val browser = BrowsingService(listOf(restaurantStats))
        restaurant.prepareForEvening(listOf(regularGroup(1, 4)))

        assertNull(browser.getEligibleRestaurants(casualGroup(2, size = 5)))
    }

    @Test
    fun `Seat Estimate Is Updated Correctly`() {
        val restaurantStats = restaurantStats(1)
        val (restaurant, _) = restaurant(restaurantStats, listOf(4, 4))
        val browser = BrowsingService(listOf(restaurantStats))
        restaurant.prepareForEvening(emptyList())

        assertEquals(1, browser.getEligibleRestaurants(casualGroup(1, size = 6)))
        assertEquals(2, restaurantStats.availableSeats[TableType.COMMON])
        assertNull(browser.getEligibleRestaurants(casualGroup(2, size = 6)))
    }

    @Test
    fun `Driver Estimate Is Correct`() {
        val restaurantStats = restaurantStats(1)
        val (restaurant, _) = restaurant(restaurantStats, listOf(4), drivers = 1)
        val browser = BrowsingService(listOf(restaurantStats))

        restaurant.prepareForEvening(emptyList())

        assertEquals(1, restaurantStats.availableDrivers)
        assertEquals(1, browser.getEligibleRestaurants(casualGroup(1, size = 2, distance = 10)))
        assertNull(browser.getEligibleRestaurants(casualGroup(2, size = 2, distance = 10)))
    }

    @Test
    fun `Available Driver Estimate Is Taken Into Account For Selection`() {
        val restaurantStats = restaurantStats(1)
        val (restaurant, _) = restaurant(restaurantStats, listOf(4), drivers = 0)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurant.prepareForEvening(emptyList())

        assertEquals(0, restaurantStats.availableDrivers)
        assertNull(browser.getEligibleRestaurants(casualGroup(1, size = 2, distance = 10)))
    }
}
