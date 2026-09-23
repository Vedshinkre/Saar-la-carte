package casualbrowsingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Tests the eligibility criteria of the BrowsingService for CasualGroups.
 */
class CasualBrowsingEligibilityTest {
    @BeforeTest
    fun setUp() {
        Time.tick = 5
        Time.evening = 1
    }

    private val water = Ingredient("water", MeasurementUnit.ML, 10, 1)
    private val meat = Ingredient("meat", MeasurementUnit.G, 10, 1)
    private val soup = Recipe(1, "soup", 1, emptyList(), mutableMapOf(water to 1), null)
    private val stew = Recipe(2, "stew", 1, emptyList(), mutableMapOf(meat to 1), null)
    private val menu = listOf(soup)
    private fun restaurantStats(
        id: Int,
        openingTickStart: Int = 1,
        openingTickEnd: Int = 24,
        positiveRatings: Int = 0,
        negativeRatings: Int = 0,
        restaurantType: RestaurantType = RestaurantType.EUROPEAN,
        menu: List<Recipe> = this.menu,
    ) = RestaurantStats(
        id,
        restaurantType,
        openingTickStart,
        openingTickEnd,
        false,
        positiveRatings,
        negativeRatings,
        menu,
    )

    private fun casualGroup(
        id: Int,
        size: Int = 2,
        distance: Int = 0,
        visitingAt: Int = 5,
        tableType: TableType = TableType.COMMON,
    ) = CasualGroup(
        id,
        size,
        tableType,
        visitingAt,
        List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        listOf(RestaurantType.EUROPEAN),
        listOf(1),
        distance,
        RatingLikelihood.ALWAYS,
    )

    private fun casualGroupExcluded(id: Int, excluded: List<Ingredient>) = CasualGroup(
        id,
        2,
        TableType.COMMON,
        5,
        List(2) { FoodPreference(excluded, emptyList(), emptyList()) },
        listOf(RestaurantType.EUROPEAN),
        listOf(1),
        0,
        RatingLikelihood.ALWAYS,
    )

    @Test
    fun `Restaurant Not Yet Open`() {
        Time.tick = 3
        val restaurantStats = restaurantStats(1, openingTickStart = 6)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableSeats[TableType.COMMON] = 20

        assertNull(browser.getEligibleRestaurants(casualGroup(1)))
    }

    @Test
    fun `Restaurant Just Opened`() {
        Time.tick = 6
        val restaurantStats = restaurantStats(1, openingTickStart = 6)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableSeats[TableType.COMMON] = 20

        assertEquals(1, browser.getEligibleRestaurants(casualGroup(1)))
    }

    @Test
    fun `Restaurant Has No Correct Table Type`() {
        val restaurantStats = restaurantStats(1)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableSeats[TableType.BAR] = 20

        assertNull(browser.getEligibleRestaurants(casualGroup(1)))
    }

    @Test
    fun `Correct Table Type Available Seat Reduced`() {
        val restaurantStats = restaurantStats(1)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableSeats[TableType.COMMON] = 10
        restaurantStats.availableSeats[TableType.BAR] = 8

        assertEquals(1, browser.getEligibleRestaurants(casualGroup(1, size = 4)))

        assertEquals(6, restaurantStats.availableSeats[TableType.COMMON])
        assertEquals(8, restaurantStats.availableSeats[TableType.BAR])
    }

    @Test
    fun `Correct Table Type Selected`() {
        val restaurantStats = restaurantStats(1)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableSeats[TableType.COMMON] = 2
        restaurantStats.availableSeats[TableType.BAR] = 20

        assertNull(browser.getEligibleRestaurants(casualGroup(1, size = 6)))
        assertEquals(1, browser.getEligibleRestaurants(casualGroup(2, size = 6, tableType = TableType.BAR)))
    }

    @Test
    fun `Restaurant Selection For Delivery Correct`() {
        val restaurantStats = restaurantStats(1, openingTickEnd = 24)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableDrivers = 3

        assertEquals(1, browser.getEligibleRestaurants(casualGroup(1, distance = 5, visitingAt = 24)))
    }

    @Test
    fun `Delivery Driver Decremented`() {
        val restaurantStats = restaurantStats(1)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableDrivers = 1

        assertEquals(1, browser.getEligibleRestaurants(casualGroup(1, distance = 10)))
        assertEquals(0, restaurantStats.availableDrivers)
        assertNull(browser.getEligibleRestaurants(casualGroup(2, distance = 10)))
    }

    @Test
    fun `Delivery Does Not Use Seats`() {
        val restaurantStats = restaurantStats(1)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableDrivers = 2
        restaurantStats.availableSeats[TableType.COMMON] = 10

        assertEquals(
            1,
            browser.getEligibleRestaurants(casualGroup(1, distance = 10, size = 4))
        )

        assertEquals(1, restaurantStats.availableDrivers)
        assertEquals(10, restaurantStats.availableSeats[TableType.COMMON])
    }

    @Test
    fun `In-House Casual Group Does Not Use Driver`() {
        val restaurantStats = restaurantStats(1)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableDrivers = 2
        restaurantStats.availableSeats[TableType.COMMON] = 10

        assertEquals(1, browser.getEligibleRestaurants(casualGroup(1, size = 4)))

        assertEquals(2, restaurantStats.availableDrivers)
        assertEquals(6, restaurantStats.availableSeats[TableType.COMMON])
    }

    @Test
    fun `Restaurant With Better Rating Difference Is Selected - Positive`() {
        val worseRestaurantStats = restaurantStats(1, positiveRatings = 4, negativeRatings = 3)
        val betterRestaurantStats = restaurantStats(2, positiveRatings = 5, negativeRatings = 1)
        val browser = BrowsingService(listOf(worseRestaurantStats, betterRestaurantStats))
        listOf(worseRestaurantStats, betterRestaurantStats).forEach { it.availableSeats[TableType.COMMON] = 10 }

        assertEquals(
            2,
            browser.getEligibleRestaurants(casualGroup(1))
        )
    }

    @Test
    fun `Restaurant With Better Rating Difference Is Selected - Negative`() {
        val worseRestaurantStats = restaurantStats(1, positiveRatings = 0, negativeRatings = 9)
        val betterRestaurantStats = restaurantStats(2, positiveRatings = 1, negativeRatings = 3)
        val browser = BrowsingService(listOf(worseRestaurantStats, betterRestaurantStats))
        listOf(worseRestaurantStats, betterRestaurantStats).forEach { it.availableSeats[TableType.COMMON] = 10 }

        assertEquals(2, browser.getEligibleRestaurants(casualGroup(1)))
    }

    @Test
    fun `Lower Id Breaks Tie`() {
        val lowerIdRestaurantStats = restaurantStats(2, positiveRatings = 5, negativeRatings = 1)
        val higherIdRestaurantStats = restaurantStats(7, positiveRatings = 5, negativeRatings = 1)
        val browser = BrowsingService(listOf(higherIdRestaurantStats, lowerIdRestaurantStats))
        listOf(lowerIdRestaurantStats, higherIdRestaurantStats).forEach { it.availableSeats[TableType.COMMON] = 10 }

        assertEquals(2, browser.getEligibleRestaurants(casualGroup(1)))
        assertEquals(2, browser.getEligibleRestaurants(casualGroup(2)))
    }

    @Test
    fun `Keeps Track Of The Best Selection`() {
        val betterRestaurantStats = restaurantStats(1, positiveRatings = 8)
        val worseRestaurantStats = restaurantStats(2, positiveRatings = 2)
        val browser = BrowsingService(listOf(betterRestaurantStats, worseRestaurantStats))
        listOf(betterRestaurantStats, worseRestaurantStats).forEach { it.availableSeats[TableType.COMMON] = 10 }

        assertEquals(1, browser.getEligibleRestaurants(casualGroup(1)))
    }

    @Test
    fun `Restaurant With Not Enough Seats Is Not Selected`() {
        val betterRestaurantStats = restaurantStats(1, positiveRatings = 20)
        val worseRestaurantStats = restaurantStats(2, positiveRatings = 1)
        val browser = BrowsingService(listOf(betterRestaurantStats, worseRestaurantStats))
        betterRestaurantStats.availableSeats[TableType.COMMON] = 1
        worseRestaurantStats.availableSeats[TableType.COMMON] = 10

        assertEquals(2, browser.getEligibleRestaurants(casualGroup(1, size = 4)))
    }

    @Test
    fun `Restaurant Type Is Filtered For Selection`() {
        val restaurantStats = restaurantStats(1, restaurantType = RestaurantType.ASIAN)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableSeats[TableType.COMMON] = 20

        assertNull(browser.getEligibleRestaurants(casualGroup(1)))
    }

    @Test
    fun `Dish Exclusion Is Taken Into Account`() {
        val restaurantStats = restaurantStats(1)
        val browser = BrowsingService(listOf(restaurantStats))
        val casualGroupExcluded = casualGroupExcluded(1, listOf(water))
        restaurantStats.availableSeats[TableType.COMMON] = 20

        assertNull(browser.getEligibleRestaurants(casualGroupExcluded))
    }

    @Test
    fun `Dish Availability Is Taken Into Account`() {
        val restaurantStats = restaurantStats(1, menu = listOf(soup, stew))
        val browser = BrowsingService(listOf(restaurantStats))
        val casualGroupExcluded = casualGroupExcluded(1, listOf(water))
        restaurantStats.availableSeats[TableType.COMMON] = 20

        assertEquals(1, browser.getEligibleRestaurants(casualGroupExcluded))
    }

    @Test
    fun `Dish Exclusion Works With Just One Customer`() {
        val restaurantStats = restaurantStats(1, menu = listOf(soup, stew))
        val browser = BrowsingService(listOf(restaurantStats))
        val casualGroup = CasualGroup(
            1,
            2,
            TableType.COMMON,
            5,
            listOf(
                FoodPreference(listOf(water), emptyList(), emptyList()),
                FoodPreference(listOf(water, meat), emptyList(), emptyList())
            ),
            listOf(RestaurantType.EUROPEAN),
            listOf(1),
            0,
            RatingLikelihood.ALWAYS,
        )
        restaurantStats.availableSeats[TableType.COMMON] = 20

        assertNull(browser.getEligibleRestaurants(casualGroup))
    }

    @Test
    fun `Restaurant With Empty Menu Is Not Selected`() {
        val restaurantStats = restaurantStats(1, menu = emptyList())
        val browser = BrowsingService(listOf(restaurantStats))
        val casualGroupExcluded = casualGroupExcluded(1, listOf(meat))
        restaurantStats.availableSeats[TableType.COMMON] = 20

        assertNull(browser.getEligibleRestaurants(casualGroupExcluded))
    }

    @Test
    fun `Browser Refuses RegularGroup`() {
        val restaurantStats = restaurantStats(1)
        val browser = BrowsingService(listOf(restaurantStats))
        val regularGroup = RegularGroup(
            1,
            2,
            TableType.COMMON,
            5,
            emptyList(),
            1,
            1,
            1
        )
        restaurantStats.availableSeats[TableType.COMMON] = 20

        assertFailsWith<IllegalArgumentException> {
            browser.getEligibleRestaurants(regularGroup)
        }
    }

    @Test
    fun `Estimates Stay Same After No Selection`() {
        val restaurantStats = restaurantStats(1)
        val browser = BrowsingService(listOf(restaurantStats))
        restaurantStats.availableSeats[TableType.COMMON] = 3
        restaurantStats.availableDrivers = 0

        assertNull(browser.getEligibleRestaurants(casualGroup(1, size = 8)))
        assertNull(browser.getEligibleRestaurants(casualGroup(2, size = 2, distance = 10)))
        assertEquals(3, restaurantStats.availableSeats[TableType.COMMON])
        assertEquals(0, restaurantStats.availableDrivers)
    }
}
