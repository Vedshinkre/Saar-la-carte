package eventbrowsingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the EVENT half of the restaurant decision made at the start of the first tick,
 * three evenings before the event: [BrowsingService.getEligibleRestaurants] for an [EventGroup]
 * plus the timing helpers on [EventGroup] itself.
 */
class EventBrowsingDecisionTest {

    @BeforeEach
    fun setup() {
        Time.tick = 1
        Time.evening = 1
    }

    @AfterEach
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
    }

    private val salt = Ingredient("salt", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)
    private val flour = Ingredient("flour", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)

    private fun recipe(id: Int, name: String, vararg ingredients: Ingredient): Recipe = Recipe(
        id,
        name,
        duration = 10,
        cookType = emptyList(),
        ingredients = ingredients.associateWith { 1 }.toMutableMap(),
        basicDishFor = null
    )

    private fun restaurant(
        id: Int,
        type: RestaurantType = RestaurantType.EUROPEAN,
        event: Boolean = true,
        openingTicks: IntRange = 1..24,
        positiveRatings: Int = 0,
        negativeRatings: Int = 0,
        menu: List<Recipe> = listOf(recipe(1, "House Special", salt)),
        eventSeats: Int = 10,
        normalSeats: Int = 10
    ): RestaurantStats {
        val stats = RestaurantStats(
            restaurantId = id,
            restaurantType = type,
            openingTickStart = openingTicks.first,
            openingTickEnd = openingTicks.last,
            event = event,
            positiveRatings = positiveRatings,
            negativeRatings = negativeRatings,
            menu = menu
        )
        stats.availableEventSeats[TableType.COMMON] = eventSeats
        stats.availableSeats[TableType.COMMON] = normalSeats
        return stats
    }

    private fun eventGroup(
        id: Int = 1,
        size: Int = 6,
        tableType: TableType = TableType.COMMON,
        visitingAt: Int = 5,
        restaurantTypes: List<RestaurantType> = listOf(RestaurantType.EUROPEAN),
        foodPreferences: List<FoodPreference> = emptyList(),
        eventEvening: Int = 4
    ): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = tableType,
        visitingAt = visitingAt,
        foodPreferences = foodPreferences,
        restaurantTypes = restaurantTypes,
        eventEvening = eventEvening,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "House Special")
    )

    private fun excluding(vararg ingredients: Ingredient) =
        FoodPreference(ingredients.toList(), emptyList(), emptyList())

    // ---- Restaurant filters ----

    @Test
    fun `a restaurant that does not host events is excluded`() {
        val service = BrowsingService(listOf(restaurant(id = 1, event = false)))

        assertNull(service.getEligibleRestaurants(eventGroup()))
    }

    @Test
    fun `a restaurant of a type the group does not consider is excluded`() {
        val service = BrowsingService(listOf(restaurant(id = 1, type = RestaurantType.ASIAN)))

        assertNull(service.getEligibleRestaurants(eventGroup(restaurantTypes = listOf(RestaurantType.EUROPEAN))))
    }

    @Test
    fun `a group considering several types can be placed in any of them`() {
        val service = BrowsingService(listOf(restaurant(id = 1, type = RestaurantType.ASIAN)))

        val result = service.getEligibleRestaurants(
            eventGroup(restaurantTypes = listOf(RestaurantType.EUROPEAN, RestaurantType.ASIAN))
        )

        assertEquals(1, result)
    }

    // ---- Opening window is checked at the group's visitingTick, not at the current tick ----

    @Test
    fun `a restaurant is checked against the visiting tick of the group and not the current tick`() {
        Time.tick = 20
        val service = BrowsingService(listOf(restaurant(id = 1, openingTicks = 1..10)))

        assertEquals(1, service.getEligibleRestaurants(eventGroup(visitingAt = 7)))
    }

    @Test
    fun `a restaurant is excluded when the visiting tick lies within its last 3 opening ticks`() {
        val service = BrowsingService(listOf(restaurant(id = 1, openingTicks = 1..10)))

        assertNull(service.getEligibleRestaurants(eventGroup(visitingAt = 8)))
    }

    @Test
    fun `a restaurant is excluded when the visiting tick is before it opens`() {
        val service = BrowsingService(listOf(restaurant(id = 1, openingTicks = 4..24)))

        assertNull(service.getEligibleRestaurants(eventGroup(visitingAt = 3)))
    }

    // ---- Seats ----

    @Test
    fun `an event is rejected when the event seats are fewer than the group size`() {
        val service = BrowsingService(listOf(restaurant(id = 1, eventSeats = 5, normalSeats = 50)))

        assertNull(service.getEligibleRestaurants(eventGroup(size = 6)))
    }

    @Test
    fun `an event is accepted when the event seats exactly match the group size`() {
        val service = BrowsingService(listOf(restaurant(id = 1, eventSeats = 6, normalSeats = 0)))

        assertEquals(1, service.getEligibleRestaurants(eventGroup(size = 6)))
    }

    @Test
    fun `a restaurant without seats of the requested table type is excluded`() {
        val barOnly = restaurant(id = 1).apply {
            availableEventSeats.clear()
            availableEventSeats[TableType.BAR] = 10
        }
        val service = BrowsingService(listOf(barOnly))

        assertNull(service.getEligibleRestaurants(eventGroup(tableType = TableType.COMMON)))
    }

    @Test
    fun `a successful event decision reduces only the event seats of the chosen restaurant`() {
        val chosen = restaurant(id = 1, eventSeats = 10, normalSeats = 10)
        val other = restaurant(id = 2, eventSeats = 10, normalSeats = 10, negativeRatings = 5)
        val service = BrowsingService(listOf(chosen, other))

        service.getEligibleRestaurants(eventGroup(size = 6))

        assertEquals(4, chosen.availableEventSeats[TableType.COMMON])
        assertEquals(10, chosen.availableSeats[TableType.COMMON])
        assertEquals(10, other.availableEventSeats[TableType.COMMON])
    }

    @Test
    fun `a second event group cannot use seats that the first one already took`() {
        val service = BrowsingService(listOf(restaurant(id = 1, eventSeats = 10)))

        assertEquals(1, service.getEligibleRestaurants(eventGroup(id = 1, size = 6)))
        assertNull(service.getEligibleRestaurants(eventGroup(id = 2, size = 6)))
    }

    // ---- Dietary preferences ----

    @Test
    fun `a restaurant is excluded when excluded ingredients rule out every dish`() {
        val menu = listOf(recipe(1, "Salty", salt), recipe(2, "Salty Bread", salt, flour))
        val service = BrowsingService(listOf(restaurant(id = 1, menu = menu)))
        val group = eventGroup(foodPreferences = listOf(excluding(salt)))

        assertNull(service.getEligibleRestaurants(group))
    }

    @Test
    fun `a restaurant is eligible when at least one dish avoids the excluded ingredients`() {
        val menu = listOf(recipe(1, "Salty", salt), recipe(2, "Plain Bread", flour))
        val service = BrowsingService(listOf(restaurant(id = 1, menu = menu)))
        val group = eventGroup(foodPreferences = listOf(excluding(salt)))

        assertEquals(1, service.getEligibleRestaurants(group))
    }

    // ---- Ranking ----

    @Test
    fun `the restaurant with the highest positive minus negative rating difference is chosen`() {
        val service = BrowsingService(
            listOf(
                restaurant(id = 1, positiveRatings = 5, negativeRatings = 4),
                restaurant(id = 2, positiveRatings = 10, negativeRatings = 5),
                restaurant(id = 3, positiveRatings = 100, negativeRatings = 99)
            )
        )

        assertEquals(2, service.getEligibleRestaurants(eventGroup()))
    }

    @Test
    fun `a rating tie is broken by the lowest restaurant id`() {
        val service = BrowsingService(
            listOf(
                restaurant(id = 7, positiveRatings = 3, negativeRatings = 1),
                restaurant(id = 2, positiveRatings = 3, negativeRatings = 1),
                restaurant(id = 5, positiveRatings = 3, negativeRatings = 1)
            )
        )

        assertEquals(2, service.getEligibleRestaurants(eventGroup()))
    }

    @Test
    fun `a better rated restaurant without enough seats loses against a worse rated one with seats`() {
        val service = BrowsingService(
            listOf(
                restaurant(id = 1, positiveRatings = 9, eventSeats = 2),
                restaurant(id = 2, positiveRatings = 0, eventSeats = 10)
            )
        )

        assertEquals(2, service.getEligibleRestaurants(eventGroup(size = 6)))
    }

    @Test
    fun `a restaurant with a negative rating balance still wins when it is the only eligible one`() {
        val service = BrowsingService(
            listOf(
                restaurant(id = 1, negativeRatings = 8),
                restaurant(id = 2, event = false, positiveRatings = 50)
            )
        )

        assertEquals(1, service.getEligibleRestaurants(eventGroup()))
    }

    @Test
    fun `a less negative balance beats a more negative one`() {
        val service = BrowsingService(
            listOf(
                restaurant(id = 1, positiveRatings = 1, negativeRatings = 6),
                restaurant(id = 2, positiveRatings = 0, negativeRatings = 2)
            )
        )

        assertEquals(2, service.getEligibleRestaurants(eventGroup()))
    }

    @Test
    fun `the second event group falls back to the next best restaurant once the best one is full`() {
        val service = BrowsingService(
            listOf(
                restaurant(id = 1, positiveRatings = 5, eventSeats = 6),
                restaurant(id = 2, positiveRatings = 1, eventSeats = 6)
            )
        )

        assertEquals(1, service.getEligibleRestaurants(eventGroup(id = 1, size = 6)))
        assertEquals(2, service.getEligibleRestaurants(eventGroup(id = 2, size = 6)))
        assertNull(service.getEligibleRestaurants(eventGroup(id = 3, size = 6)))
    }

    @Test
    fun `a rejected event group leaves the seats of every restaurant untouched`() {
        val small = restaurant(id = 1, eventSeats = 3)
        val service = BrowsingService(listOf(small))

        assertNull(service.getEligibleRestaurants(eventGroup(size = 6)))

        assertEquals(3, small.availableEventSeats[TableType.COMMON])
    }

    @Test
    fun `an empty menu makes a restaurant ineligible`() {
        val service = BrowsingService(listOf(restaurant(id = 1, menu = emptyList())))

        assertNull(service.getEligibleRestaurants(eventGroup(foodPreferences = listOf(excluding(salt)))))
    }

    @Test
    fun `every member's exclusions must leave a dish on the menu`() {
        val menu = listOf(recipe(1, "Salty", salt), recipe(2, "Plain Bread", flour))
        val service = BrowsingService(listOf(restaurant(id = 1, menu = menu)))
        val group = eventGroup(foodPreferences = listOf(excluding(salt), excluding(flour, salt)))

        assertNull(service.getEligibleRestaurants(group))
    }

    @Test
    fun `members with different exclusions are served by different dishes of the same menu`() {
        val menu = listOf(recipe(1, "Salty", salt), recipe(2, "Plain Bread", flour))
        val service = BrowsingService(listOf(restaurant(id = 1, menu = menu)))
        val group = eventGroup(foodPreferences = listOf(excluding(salt), excluding(flour)))

        assertEquals(1, service.getEligibleRestaurants(group))
    }

    // ---- Timing and event dish ----

    @Test
    fun `an event group decides exactly three evenings before its event`() {
        val group = eventGroup(eventEvening = 4)

        Time.evening = 1
        assertTrue(group.visitingInThreeEvenings())
        Time.evening = 2
        assertFalse(group.visitingInThreeEvenings())
        Time.evening = 0
        assertFalse(group.visitingInThreeEvenings())
    }

    @Test
    fun `an event group is visiting tonight only on its event evening`() {
        val group = eventGroup(eventEvening = 4)

        Time.evening = 3
        assertFalse(group.isVisitingTonight())
        Time.evening = 4
        assertTrue(group.isVisitingTonight())
    }

    @Test
    fun `the favorite dish is only known once a restaurant type has been assigned`() {
        val group = eventGroup()
        assertNull(group.getCurrentEventDish())

        group.currentRestaurantType = RestaurantType.EUROPEAN

        assertEquals("House Special", group.getCurrentEventDish())
    }
}
