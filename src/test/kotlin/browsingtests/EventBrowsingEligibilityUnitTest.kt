package browsingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * unit tests for F28 (Browsing Service) for which restaurants an EVENT group is offered
 * and for event seats, walk-in seats and drivers being three separate estimates
 * casual dine-in and delivery eligibility tested by Deniz
 */
class EventBrowsingEligibilityUnitTest {
    @BeforeTest
    fun setUp() {
        Time.tick = 1
        Time.evening = 1
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
    }

    private val water = Ingredient("water", MeasurementUnit.ML, 10, 1)
    private val meat = Ingredient("meat", MeasurementUnit.G, 10, 1)
    private val soup = Recipe(1, "soup", 1, emptyList(), mutableMapOf(water to 1), null)
    private val stew = Recipe(2, "stew", 1, emptyList(), mutableMapOf(meat to 1), null)
    private val menu = listOf(soup)

    private fun stats(
        id: Int,
        openingTickStart: Int = 1,
        openingTickEnd: Int = 24,
        event: Boolean = true,
        positiveRatings: Int = 0,
        negativeRatings: Int = 0,
        restaurantType: RestaurantType = RestaurantType.EUROPEAN,
        menu: List<Recipe> = this.menu,
    ) = RestaurantStats(
        restaurantId = id,
        restaurantType = restaurantType,
        openingTickStart = openingTickStart,
        openingTickEnd = openingTickEnd,
        event = event,
        positiveRatings = positiveRatings,
        negativeRatings = negativeRatings,
        menu = menu,
    )

    private fun event(
        id: Int,
        size: Int = 4,
        visitingAt: Int = 5,
        tableType: TableType = TableType.COMMON,
    ) = EventGroup(
        id = id,
        size = size,
        tableType = tableType,
        visitingAt = visitingAt,
        foodPreferences = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = 4,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "soup"),
    )

    /** An event group whose guests all refuse [excluded]. */
    private fun eventExcluding(id: Int, excluded: List<Ingredient>) = EventGroup(
        id = id,
        size = 4,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = List(4) { FoodPreference(excluded, emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = 4,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "soup"),
    )

    private fun casual(id: Int, size: Int = 2) = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.ALWAYS,
    )

    // the opening time of the event evening

    /** new guests are refused in the last 3 opening ticks, so arriving exactly before that still works */
    @Test
    fun `an event group is offered a restaurant it reaches on the last tick before the cut-off`() {
        val restaurant = stats(1, openingTickEnd = 20)
        restaurant.availableEventSeats[TableType.COMMON] = 10

        assertEquals(1, BrowsingService(listOf(restaurant)).getEligibleRestaurants(event(1, visitingAt = 17)))
    }

    @Test
    fun `an event group arriving inside the last three opening ticks is not offered the restaurant`() {
        val restaurant = stats(1, openingTickEnd = 20)
        restaurant.availableEventSeats[TableType.COMMON] = 10

        assertNull(BrowsingService(listOf(restaurant)).getEligibleRestaurants(event(1, visitingAt = 18)))
    }

    @Test
    fun `an event group arriving before the restaurant opens is not offered it`() {
        val restaurant = stats(1, openingTickStart = 8)
        restaurant.availableEventSeats[TableType.COMMON] = 10

        assertNull(BrowsingService(listOf(restaurant)).getEligibleRestaurants(event(1, visitingAt = 5)))
    }

    // the event seat estimate

    @Test
    fun `a reservation takes exactly the group size out of the event seats`() {
        val restaurant = stats(1)
        restaurant.availableEventSeats[TableType.COMMON] = 10

        assertEquals(1, BrowsingService(listOf(restaurant)).getEligibleRestaurants(event(1, size = 4)))

        assertEquals(6, restaurant.availableEventSeats[TableType.COMMON])
    }

    @Test
    fun `event groups of different table types draw on their own seat pools`() {
        val restaurant = stats(1)
        restaurant.availableEventSeats[TableType.COMMON] = 4
        restaurant.availableEventSeats[TableType.SEPARATED] = 4
        val service = BrowsingService(listOf(restaurant))

        assertEquals(1, service.getEligibleRestaurants(event(1, size = 4)))
        assertEquals(1, service.getEligibleRestaurants(event(2, size = 4, tableType = TableType.SEPARATED)))

        assertEquals(0, restaurant.availableEventSeats[TableType.COMMON])
        assertEquals(0, restaurant.availableEventSeats[TableType.SEPARATED])
    }

    // the three estimates are kept apart

    /** events are booked against their own seat pool, so the walk-in estimate must not move */
    @Test
    fun `an event reservation leaves the dining-room seats and the drivers alone`() {
        val restaurant = stats(1)
        restaurant.availableEventSeats[TableType.COMMON] = 10
        restaurant.availableSeats[TableType.COMMON] = 12
        restaurant.availableDrivers = 2

        assertEquals(1, BrowsingService(listOf(restaurant)).getEligibleRestaurants(event(1, size = 4)))

        assertEquals(6, restaurant.availableEventSeats[TableType.COMMON])
        assertEquals(12, restaurant.availableSeats[TableType.COMMON])
        assertEquals(2, restaurant.availableDrivers)
    }

    /** other way round: a walk-in group must not take up event reservations */
    @Test
    fun `a walk-in decision leaves the event seats alone`() {
        Time.tick = 5
        val restaurant = stats(1)
        restaurant.availableEventSeats[TableType.COMMON] = 10
        restaurant.availableSeats[TableType.COMMON] = 12

        assertEquals(1, BrowsingService(listOf(restaurant)).getEligibleRestaurants(casual(1, size = 2)))

        assertEquals(10, restaurant.availableEventSeats[TableType.COMMON])
        assertEquals(10, restaurant.availableSeats[TableType.COMMON])
    }

    /** event group decides based on the event seats even when plenty of other seats available */
    @Test
    fun `plenty of walk-in seats do not help an event group without event seats`() {
        val restaurant = stats(1)
        restaurant.availableSeats[TableType.COMMON] = 50
        restaurant.availableEventSeats[TableType.COMMON] = 2

        assertNull(BrowsingService(listOf(restaurant)).getEligibleRestaurants(event(1, size = 8)))
        assertEquals(50, restaurant.availableSeats[TableType.COMMON])
    }

    // what the event guests can eat

    @Test
    fun `a restaurant of a type the event did not consider is not offered`() {
        val asian = stats(1, restaurantType = RestaurantType.ASIAN)
        asian.availableEventSeats[TableType.COMMON] = 20

        assertNull(BrowsingService(listOf(asian)).getEligibleRestaurants(event(1)))
    }

    /** a restaurant that lists no event seats at all for the wanted table type is out */
    @Test
    fun `a restaurant with no event seats of the table type is not offered`() {
        val restaurant = stats(1)
        restaurant.availableEventSeats[TableType.BAR] = 20

        assertNull(BrowsingService(listOf(restaurant)).getEligibleRestaurants(event(1)))
    }

    @Test
    fun `a restaurant whose only dish the event guests refuse is not offered`() {
        val restaurant = stats(1)
        restaurant.availableEventSeats[TableType.COMMON] = 20

        assertNull(BrowsingService(listOf(restaurant)).getEligibleRestaurants(eventExcluding(1, listOf(water))))
    }

    @Test
    fun `a restaurant is offered when one dish avoids what the guests refuse`() {
        val restaurant = stats(1, menu = listOf(soup, stew))
        restaurant.availableEventSeats[TableType.COMMON] = 20

        assertEquals(1, BrowsingService(listOf(restaurant)).getEligibleRestaurants(eventExcluding(1, listOf(water))))
    }

    @Test
    fun `a restaurant with an empty menu is never offered to an event`() {
        val restaurant = stats(1, menu = emptyList())
        restaurant.availableEventSeats[TableType.COMMON] = 20

        assertNull(BrowsingService(listOf(restaurant)).getEligibleRestaurants(eventExcluding(1, listOf(meat))))
    }

    // choosing between restaurants

    /** with several restaurants to choose from, the one with best rating difference is chosen  */
    @Test
    fun `the best rated of several eligible restaurants is booked`() {
        val worse = stats(1, positiveRatings = 4, negativeRatings = 3)
        val better = stats(2, positiveRatings = 5, negativeRatings = 1)
        listOf(worse, better).forEach { it.availableEventSeats[TableType.COMMON] = 20 }

        assertEquals(2, BrowsingService(listOf(worse, better)).getEligibleRestaurants(event(1)))
        assertEquals(20, worse.availableEventSeats[TableType.COMMON], "the loser keeps its estimate")
    }

    @Test
    fun `an equal rating difference is broken by the lowest restaurant id`() {
        val low = stats(3, positiveRatings = 5, negativeRatings = 1)
        val high = stats(8, positiveRatings = 9, negativeRatings = 5)
        listOf(low, high).forEach { it.availableEventSeats[TableType.COMMON] = 20 }

        assertEquals(3, BrowsingService(listOf(high, low)).getEligibleRestaurants(event(1)))
        assertEquals(3, BrowsingService(listOf(low, high)).getEligibleRestaurants(event(2)))
    }

    /** a worse restaurant later in the list never displaces the one already in front */
    @Test
    fun `a worse host later in the list does not displace the leader`() {
        val better = stats(1, positiveRatings = 8)
        val worse = stats(2, positiveRatings = 2)
        listOf(better, worse).forEach { it.availableEventSeats[TableType.COMMON] = 20 }

        assertEquals(1, BrowsingService(listOf(better, worse)).getEligibleRestaurants(event(1)))
    }

    @Test
    fun `the event group takes the best rated restaurant that still has event seats`() {
        val bestButFull = stats(1).also {
            it.positiveRatings = 9
            it.availableEventSeats[TableType.COMMON] = 2
        }
        val roomy = stats(2).also {
            it.positiveRatings = 3
            it.availableEventSeats[TableType.COMMON] = 10
        }

        assertEquals(2, BrowsingService(listOf(bestButFull, roomy)).getEligibleRestaurants(event(1, size = 6)))
        assertEquals(2, bestButFull.availableEventSeats[TableType.COMMON], "the full one keeps its estimate")
    }

    @Test
    fun `a restaurant that does not host events is never offered even when it is the best`() {
        val noEvents = stats(1, event = false).also {
            it.positiveRatings = 20
            it.availableEventSeats[TableType.COMMON] = 50
        }
        val hostsEvents = stats(2).also { it.availableEventSeats[TableType.COMMON] = 10 }

        assertEquals(2, BrowsingService(listOf(noEvents, hostsEvents)).getEligibleRestaurants(event(1)))
    }
}
