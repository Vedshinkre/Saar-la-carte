package casualcustomerdeliverytests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** CasualCustomerDelivery F 24
 * CasualCustomerDelivery validates the behaviors of casual delivery groups and their interaction with BrowsingService.
 * It verifies that rating generation accurately maps experience levels based on RatingLikelihood (NEVER, SOME, ALWAYS);
 * ensures the delivery group are added in the right tick and ceiling-divided travel distance;
 * confirms that BrowsingService selects restaurants based on net ratings, available drivers, and
 * dietary compatibility while decrementing available driver capacity;
 * and validates dish selection fallbacks when available menus conflict with customer food preferences.
 */
class CasualCustomerDelivery {

    @BeforeEach
    fun setup() {
        // Ensures global state is reset before every test to prevent flaky failures
        Time.tick = 1
    }

    // helper Methods

    private fun createGroup(
        visitingAt: Int,
        deliveryDistance: Int,
        ratingLikelihood: RatingLikelihood
    ): CasualGroup {
        return CasualGroup(
            id = 1,
            size = 2,
            tableType = TableType.COMMON,
            visitingAt = visitingAt,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = deliveryDistance,
            ratingLikelihood = ratingLikelihood
        )
    }

    private fun createRestaurant(
        id: Int,
        positive: Int,
        negative: Int,
        drivers: Int = 1,
        menuList: List<Recipe> = emptyList()
    ): RestaurantStats {
        return RestaurantStats(
            restaurantId = id,
            restaurantType = RestaurantType.EUROPEAN,
            openingTickStart = 1,
            openingTickEnd = 24,
            event = false,
            positiveRatings = positive,
            negativeRatings = negative,
            menu = menuList
        ).apply { availableDrivers = drivers }
    }

    private fun createMockDeliveryGroup(
        foodPrefs: List<FoodPreference> = emptyList()
    ): CasualGroup {
        return mock {
            whenever(it.wantsDelivery).thenReturn(true)
            whenever(it.restaurantTypes).thenReturn(listOf(RestaurantType.EUROPEAN))
            whenever(it.foodPreferences).thenReturn(foodPrefs)
        }
    }

    //  determineRating()

    @Test
    fun `determineRating-NEVER rating likelihood -always returns NO_RATING`() {
        //  a casual group with NEVER rating likelihood always produces NO_RATING across all experiences
        val group = createGroup(10, 10, RatingLikelihood.NEVER)

        group.experience = ExperienceType.POSITIVE
        assertEquals(RatingType.NO_RATING, group.determineRating())

        group.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.NO_RATING, group.determineRating())

        group.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NO_RATING, group.determineRating())
    }

    @Test
    fun `determineRating-SOME rating likelihood-returns correct ratings`() {
        // a casual group with SOME rating likelihood maps experiences to POSITIVE, NO_RATING, and NEGATIVE ratings
        val group = createGroup(15, 15, RatingLikelihood.SOME)

        group.experience = ExperienceType.POSITIVE
        assertEquals(RatingType.POSITIVE, group.determineRating())

        group.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.NO_RATING, group.determineRating())

        group.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NEGATIVE, group.determineRating())
    }

    @Test
    fun `determineRating-ALWAYS rating likelihood-returns correct ratings`() {
        //  a casual group with ALWAYS rating likelihood promotes NEUTRAL and POSITIVE experiences to POSITIVE ratings
        val group = createGroup(12, 7, RatingLikelihood.ALWAYS)

        group.experience = ExperienceType.POSITIVE
        assertEquals(RatingType.POSITIVE, group.determineRating())

        group.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.POSITIVE, group.determineRating())

        group.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NEGATIVE, group.determineRating())
    }

    // isVisitingThisTick() and getDeliveryOrderTick()

    @Test
    fun `isVisitingThisTick-no delivery uses-visitingAt directly`() {
        //  an in-restaurant group without delivery visit status visit  directly at visitingAt
        val group = createGroup(visitingAt = 10, deliveryDistance = 0, RatingLikelihood.NEVER)
        assertFalse(group.wantsDelivery)

        Time.tick = 9
        assertFalse(group.isVisitingThisTick())

        Time.tick = 10
        assertTrue(group.isVisitingThisTick())
    }

    @Test
    fun `isVisitingThisTick- delivery distance 5 uses-correct ordering tick`() {
//  a delivery group calculates ordering tick by subtracting cooking ticks and exact travel ticks from visitingAt
        val group = createGroup(visitingAt = 12, deliveryDistance = 5, RatingLikelihood.NEVER)
        assertTrue(group.wantsDelivery)

        Time.tick = 7
        assertFalse(group.isVisitingThisTick())

        Time.tick = 8
        assertTrue(group.isVisitingThisTick())
    }

    @Test
    fun `isVisitingThisTick- delivery distance 7-test private ceil function`() {
        //  delivery ordering tick calculations happens correctly, ceiling non-multiple travel distances
        val group = createGroup(visitingAt = 15, deliveryDistance = 7, RatingLikelihood.NEVER)

        Time.tick = 9
        assertFalse(group.isVisitingThisTick())

        Time.tick = 10
        assertTrue(group.isVisitingThisTick())
    }

    //  BrowsingService getEligibleRestaurants()

    @Test
    fun `getEligibleRestaurants - casual delivery- highest rating and decrement driver`() {
        // browsing selects the highest net-rated restaurant with available drivers and decrements its driver count
        val rest1 = createRestaurant(id = 1, positive = 10, negative = 2, drivers = 1) // Net: +8
        val rest2 = createRestaurant(id = 2, positive = 50, negative = 0, drivers = 0) // Net: +50, but no drivers

        val browsingService = BrowsingService(listOf(rest1, rest2))
        val group = createMockDeliveryGroup()

        val selectedId = browsingService.getEligibleRestaurants(group)

        assertEquals(1, selectedId)
        assertEquals(0, rest1.availableDrivers) // Verifies the driver was decremented
    }

    @Test
    fun `getEligibleRestaurants - casual delivery - null when no drivers available`() {
        //   browsing returns null when candidate restaurants have zero available delivery drivers
        val rest = createRestaurant(id = 1, positive = 10, negative = 2, drivers = 0)
        val browsingService = BrowsingService(listOf(rest))

        val selectedId = browsingService.getEligibleRestaurants(createMockDeliveryGroup())

        assertEquals(null, selectedId)
    }

    @Test
    fun `getEligibleRestaurants - casual delivery compares ratings and handles tie breaker`() {
        //  browsing compares net ratings between restaurants with available drivers to pick the higher-rated option.
        val rest1 = createRestaurant(id = 1, positive = 5, negative = 0, drivers = 2) // Net: +5
        val rest2 = createRestaurant(id = 2, positive = 15, negative = 5, drivers = 2) // Net: +10

        val browsingService = BrowsingService(listOf(rest1, rest2))
        val selectedId = browsingService.getEligibleRestaurants(createMockDeliveryGroup())

        // Rest2 has a net rating of +10 vs Rest1's +5
        assertEquals(2, selectedId)
    }

    @Test
    fun `getEligibleRestaurants - casual delivery filters out restaurants violating dietary preferences`() {
        //  browsing eliminates restaurants whose menus contain ingredients excluded by the group's food preferences
        val ingredientX = mock<Ingredient> { whenever(it.name).thenReturn("Peanuts") }

        val safeRecipe = mock<Recipe> {
            whenever(it.ingredients).thenReturn(mutableMapOf<Ingredient, Int>())
        }
        val unsafeRecipe = mock<Recipe> {
            whenever(it.ingredients).thenReturn(mutableMapOf(ingredientX to 10))
        }

        val rest1 = createRestaurant(id = 1, positive = 10, negative = 0, menuList = listOf(unsafeRecipe))
        val rest2 = createRestaurant(id = 2, positive = 5, negative = 0, menuList = listOf(safeRecipe))

        val browsingService = BrowsingService(listOf(rest1, rest2))

        // Create food pref that excludes Peanuts
        val foodPref = mock<FoodPreference> { whenever(it.excludedIngredients).thenReturn(listOf(ingredientX)) }
        val group = createMockDeliveryGroup(foodPrefs = listOf(foodPref))

        val selectedId = browsingService.getEligibleRestaurants(group)

        // Rest1 is filtered out by dietary check, so Rest2 wins despite lower rating
        assertEquals(2, selectedId)
    }

    @Test
    fun `getEligibleRestaurants throws exception for unsupported group type`() {
//  browsing throws an IllegalArgumentException when called with an unsupported customer group type
        val group = mock<RegularGroup>()
        val browsingService = BrowsingService(emptyList())

        assertThrows<IllegalArgumentException> {
            browsingService.getEligibleRestaurants(group)
        }
    }

    @Test
    fun `getEligibleRestaurants - casual delivery - rejects restaurant with an empty menu`() {
        // Restaurant with good ratings but zero recipes
        val emptyRest = createRestaurant(
            id = 1,
            positive = 10,
            negative = 0,
            drivers = 1,
            menuList = emptyList()
        )

        val browsingService = BrowsingService(listOf(emptyRest))

        // Group with at least one food preference
        val pref = mock<FoodPreference> {
            whenever(it.excludedIngredients).thenReturn(emptyList())
        }
        val group = createMockDeliveryGroup(foodPrefs = listOf(pref))

        val selectedId = browsingService.getEligibleRestaurants(group)

        // Must return null because the empty menu immediately fails the matchPreference check
        assertEquals(null, selectedId)
    }

    @Test
    fun `decideDish - delivery group excludes all available recipes returns null`() {
        //  dish selection returns null when all candidate recipes contain excluded ingredients for delivery customers
        val mushroom = mock<Ingredient> { whenever(it.name).thenReturn("Mushroom") }
        val mushroomSoup = mock<Recipe> {
            whenever(it.ingredients).thenReturn(mutableMapOf(mushroom to 10))
        }

        // Delivery customer excludes Mushrooms
        val pref = FoodPreference(
            excludedIngredients = listOf(mushroom),
            preferredIngredients = emptyList(),
            favouriteDishes = emptyList()
        )

        // The menu only has Mushroom Soup, so menu become empty becomes empty
        val dish = pref.decideDish(listOf(mushroomSoup), "", RestaurantType.EUROPEAN)

        assertEquals(null, dish)
    }

    @Test
    fun `decideDish - delivery group - matches favourite dish`() {
        //  dish selection selects personal favorite recipes over non-favorites for delivery customers
        val pizzaRecipe = mock<Recipe> {
            whenever(it.name).thenReturn("Pizza")
            whenever(it.ingredients).thenReturn(mutableMapOf())
        }
        val saladRecipe = mock<Recipe> {
            whenever(it.name).thenReturn("Salad")
            whenever(it.ingredients).thenReturn(mutableMapOf())
        }

        // Delivery customer specifically wants Pizza
        val pref = FoodPreference(
            excludedIngredients = emptyList(),
            preferredIngredients = emptyList(),
            favouriteDishes = listOf("Pizza")
        )

        val dish = pref.decideDish(listOf(saladRecipe, pizzaRecipe), "", RestaurantType.EUROPEAN)

        // should select pizza over salad
        assertEquals("Pizza", dish?.recipe?.name)
    }

    @Test
    fun `isVisitingThisTick - delivery distance with exact multiple `() {
        //  delivery ordering ticks strictly trigger at the exact tick
        // To arrive at visitingTick 12, the group must order at tick 7 (12 - 2 - 3).
        val group = createGroup(visitingAt = 12, deliveryDistance = 10, RatingLikelihood.NEVER)

        // Tick 6: Too early
        Time.tick = 6
        assertFalse(group.isVisitingThisTick())

        // Tick 7: The exact tick the order should be placed
        Time.tick = 7
        assertTrue(group.isVisitingThisTick())

        // Tick 8: Too late
        Time.tick = 8
        assertFalse(group.isVisitingThisTick())
    }
}
