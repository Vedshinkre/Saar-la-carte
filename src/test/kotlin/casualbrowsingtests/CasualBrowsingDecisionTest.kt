package casualbrowsingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for F25's CASUAL half: [BrowsingService.getEligibleRestaurants] for dine-in
 * and delivery [CasualGroup]s, plus the decision-timing logic in [CasualGroup] itself.
 * EVENT decisions are out of scope - see the F25 split notes.
 */
class CasualBrowsingDecisionTest {

    @BeforeEach
    fun setup() {
        Time.tick = 5
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
        openingTicks: IntRange = 1..24,
        positiveRatings: Int = 0,
        negativeRatings: Int = 0,
        menu: List<Recipe> = listOf(recipe(1, "House Special", salt)),
        seats: Int = 10,
        drivers: Int = 5,
        tableType: TableType = TableType.COMMON
    ): RestaurantStats {
        val stats = RestaurantStats(
            restaurantId = id,
            restaurantType = type,
            openingTickStart = openingTicks.first,
            openingTickEnd = openingTicks.last,
            event = false,
            positiveRatings = positiveRatings,
            negativeRatings = negativeRatings,
            menu = menu
        )
        stats.availableSeats[tableType] = seats
        stats.availableDrivers = drivers
        return stats
    }

    private fun dineInGroup(
        id: Int = 1,
        size: Int = 4,
        tableType: TableType = TableType.COMMON,
        restaurantTypes: List<RestaurantType> = listOf(RestaurantType.EUROPEAN),
        foodPreferences: List<FoodPreference> = emptyList(),
        visitingAt: Int = 5
    ): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = tableType,
        visitingAt = visitingAt,
        foodPreferences = foodPreferences,
        restaurantTypes = restaurantTypes,
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    private fun deliveryGroup(
        id: Int = 1,
        size: Int = 4,
        restaurantTypes: List<RestaurantType> = listOf(RestaurantType.EUROPEAN),
        deliveryDistance: Int = 5,
        visitingAt: Int = 10
    ): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = emptyList(),
        restaurantTypes = restaurantTypes,
        visitingEvenings = listOf(1),
        deliveryDistance = deliveryDistance,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    // ---- Opening window ----

    @Test
    fun `a restaurant within the last 3 ticks of its opening time is excluded for dine-in`() {
        val closingSoon = restaurant(id = 1, openingTicks = 1..8) // open window ends at tick 5
        Time.tick = 6 // within the last 3 ticks (6,7,8)
        val service = BrowsingService(listOf(closingSoon))

        val result = service.getEligibleRestaurants(dineInGroup(visitingAt = 6))

        assertNull(result)
    }

    @Test
    fun `a restaurant exactly at the opening-window boundary is still eligible for dine-in`() {
        val restaurant = restaurant(id = 1, openingTicks = 1..8)
        Time.tick = 5 // openingTickEnd - 3, the last eligible tick
        val service = BrowsingService(listOf(restaurant))

        val result = service.getEligibleRestaurants(dineInGroup(visitingAt = 5))

        assertEquals(1, result)
    }

    // ---- Restaurant type ----

    @Test
    fun `a restaurant of a different type than requested is excluded`() {
        val wrongType = restaurant(id = 1, type = RestaurantType.ASIAN)
        val service = BrowsingService(listOf(wrongType))

        val result = service.getEligibleRestaurants(dineInGroup(restaurantTypes = listOf(RestaurantType.EUROPEAN)))

        assertNull(result)
    }

    // ---- Dine-in seat availability ----

    @Test
    fun `dine-in is rejected when the estimated available seats are fewer than the group size`() {
        val tooFull = restaurant(id = 1, seats = 3)
        val service = BrowsingService(listOf(tooFull))

        val result = service.getEligibleRestaurants(dineInGroup(size = 4))

        assertNull(result)
    }

    @Test
    fun `dine-in is accepted when available seats exactly match the group size`() {
        val exactFit = restaurant(id = 1, seats = 4)
        val service = BrowsingService(listOf(exactFit))

        val result = service.getEligibleRestaurants(dineInGroup(size = 4))

        assertEquals(1, result)
    }

    // ---- Delivery driver availability ----

    @Test
    fun `delivery is rejected when no driver is available regardless of everything else`() {
        val noDrivers = restaurant(id = 1, drivers = 0, positiveRatings = 100)
        val service = BrowsingService(listOf(noDrivers))

        val result = service.getEligibleRestaurants(deliveryGroup())

        assertNull(result)
    }

    // ---- Dietary compatibility ----

    @Test
    fun `a restaurant is excluded when a customer's excluded ingredients rule out every dish`() {
        val onlySaltyDish = restaurant(id = 1, menu = listOf(recipe(1, "Salty Dish", salt)))
        val allergicToSalt = FoodPreference(
            excludedIngredients = listOf(salt),
            preferredIngredients = emptyList(),
            favouriteDishes = emptyList()
        )
        val service = BrowsingService(listOf(onlySaltyDish))

        val result = service.getEligibleRestaurants(dineInGroup(foodPreferences = listOf(allergicToSalt)))

        assertNull(result)
    }

    @Test
    fun `a restaurant is eligible when at least one dish avoids every customer's excluded ingredients`() {
        val saltFreeOption = restaurant(
            id = 1,
            menu = listOf(recipe(1, "Salty Dish", salt), recipe(2, "Plain Bread", flour))
        )
        val allergicToSalt = FoodPreference(
            excludedIngredients = listOf(salt),
            preferredIngredients = emptyList(),
            favouriteDishes = emptyList()
        )
        val service = BrowsingService(listOf(saltFreeOption))

        val result = service.getEligibleRestaurants(dineInGroup(foodPreferences = listOf(allergicToSalt)))

        assertEquals(1, result)
    }

    // ---- Rating / id tie-break ----

    @Test
    fun `the restaurant with the highest positive minus negative rating difference is chosen`() {
        val worse = restaurant(id = 1, positiveRatings = 5, negativeRatings = 5) // diff 0
        val better = restaurant(id = 2, positiveRatings = 10, negativeRatings = 2) // diff 8
        val service = BrowsingService(listOf(worse, better))

        val result = service.getEligibleRestaurants(dineInGroup())

        assertEquals(2, result)
    }

    @Test
    fun `a rating tie is broken by the lowest restaurant id`() {
        val higherId = restaurant(id = 9, positiveRatings = 5, negativeRatings = 0)
        val lowerId = restaurant(id = 3, positiveRatings = 5, negativeRatings = 0)
        val service = BrowsingService(listOf(higherId, lowerId))

        val result = service.getEligibleRestaurants(dineInGroup())

        assertEquals(3, result)
    }

    // ---- Estimate side effects ----

    @Test
    fun `a successful dine-in decision decrements only that restaurant's available seats`() {
        val chosen = restaurant(id = 1, seats = 10)
        val other = restaurant(id = 2, seats = 10, positiveRatings = -1) // rated lower, won't be picked
        val service = BrowsingService(listOf(chosen, other))

        service.getEligibleRestaurants(dineInGroup(size = 4, tableType = TableType.COMMON))

        assertEquals(6, chosen.availableSeats[TableType.COMMON])
        assertEquals(10, other.availableSeats[TableType.COMMON])
    }

    @Test
    fun `a successful delivery decision decrements the chosen restaurant's available drivers by one`() {
        val chosen = restaurant(id = 1, drivers = 3)
        val service = BrowsingService(listOf(chosen))

        service.getEligibleRestaurants(deliveryGroup())

        assertEquals(2, chosen.availableDrivers)
    }

    // ---- No eligible restaurant ----

    @Test
    fun `no eligible restaurant yields null instead of throwing`() {
        val service = BrowsingService(emptyList())

        val result = service.getEligibleRestaurants(dineInGroup())

        assertNull(result)
    }

    // ---- CasualGroup decision timing ----

    @Test
    fun `a dine-in group decides exactly at its visitingTick`() {
        val group = dineInGroup(visitingAt = 10)

        Time.tick = 9
        val early = group.isVisitingThisTick()
        Time.tick = 10
        val onTime = group.isVisitingThisTick()

        assertEquals(false, early)
        assertEquals(true, onTime)
    }

    @Test
    fun `a delivery group decides visitingTick minus ceil(distance div 5) minus 3`() {
        // distance 7 -> ceil(7/5) = 2, so decision tick = visitingAt - 2 - 3
        val group = deliveryGroup(visitingAt = 10, deliveryDistance = 7)

        Time.tick = 5
        val decisionTick = group.isVisitingThisTick()
        Time.tick = 4
        val tooEarly = group.isVisitingThisTick()
        Time.tick = 10
        val actualVisitingTick = group.isVisitingThisTick()

        assertEquals(true, decisionTick)
        assertEquals(false, tooEarly)
        assertEquals(false, actualVisitingTick, "delivery groups don't decide again at their visitingTick")
    }
}
