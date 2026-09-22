package casualbrowsingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.RatingProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for F25's CASUAL half: [BrowsingService] combined with the real
 * rating pipeline ([RatingProcessor]) that feeds it, and confirming the browsing decision
 * stays decoupled from table state (F14/F16's job, not F25's).
 */
class CasualBrowsingIntegrationTest {

    @BeforeEach
    fun setup() {
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
    }

    private val salt = Ingredient("salt", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)
    private fun recipe(): Recipe = Recipe(
        1,
        "House Special",
        duration = 10,
        cookType = emptyList(),
        ingredients = mutableMapOf(salt to 1),
        basicDishFor = null
    )

    private fun restaurant(id: Int, seats: Int = 10): RestaurantStats {
        val stats = RestaurantStats(
            restaurantId = id,
            restaurantType = RestaurantType.EUROPEAN,
            openingTickStart = 1,
            openingTickEnd = 24,
            event = false,
            positiveRatings = 0,
            negativeRatings = 0,
            menu = listOf(recipe())
        )
        stats.availableSeats[TableType.COMMON] = seats
        return stats
    }

    private fun casualGroup(
        id: Int,
        size: Int = 2,
        ratingLikelihood: RatingLikelihood = RatingLikelihood.NEVER
    ): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = ratingLikelihood
    )

    @Test
    fun `a decision reflects ratings accumulated during the simulation via the real rating pipeline`() {
        val restaurantA = restaurant(id = 1)
        val restaurantB = restaurant(id = 2)
        val service = BrowsingService(listOf(restaurantA, restaurantB))

        // Tied at 0/0: lowest id (restaurant A) wins first.
        assertEquals(1, service.getEligibleRestaurants(casualGroup(id = 100)))

        // Restaurant A picks up a real negative rating through RatingProcessor, not a
        // manually-poked field - this is how Restaurant.simulateTick() actually updates it.
        val disappointedGroup = casualGroup(id = 101, ratingLikelihood = RatingLikelihood.SOME)
            .apply { experience = ExperienceType.NEGATIVE }
        val ratingProcessor = RatingProcessor(
            deliveryGroups = mutableListOf<CustomerGroup>(),
            turnedAwayGroups = listOf(disappointedGroup),
            eventGroups = mutableListOf<EventGroup>(),
            getInHouseGroups = { emptyList() },
            getServingPriority = { 0 },
            removeProcessedGroup = {}
        )
        val (positive, negative) = ratingProcessor.rate(
            disappointedGroup,
            restaurantA.positiveRatings,
            restaurantA.negativeRatings
        )
        restaurantA.positiveRatings = positive
        restaurantA.negativeRatings = negative

        // Restaurant B (still 0/0) now beats restaurant A (0/1).
        assertEquals(2, service.getEligibleRestaurants(casualGroup(id = 102)))
    }

    @Test
    fun `a browsing decision never reserves or occupies a table - that stays FrontOfHouse's job`() {
        val stats = restaurant(id = 1)
        val service = BrowsingService(listOf(stats))
        val tables = listOf(Table(id = 1, size = 4, tableType = TableType.COMMON))

        val decision = service.getEligibleRestaurants(casualGroup(id = 1))

        assertEquals(1, decision)
        assertTrue(tables.all { it.status == TableStatus.FREE }, "browsing must not touch real table state")
    }

    @Test
    fun `two CASUAL groups deciding in the same tick share one seat estimate - the second can be blocked`() {
        val stats = restaurant(id = 1, seats = 5)
        val service = BrowsingService(listOf(stats))
        val firstGroup = casualGroup(id = 1, size = 4)
        val secondGroup = casualGroup(id = 2, size = 4)

        val firstDecision = service.getEligibleRestaurants(firstGroup)
        val secondDecision = service.getEligibleRestaurants(secondGroup)

        assertEquals(1, firstDecision, "first group takes 4 of the 5 estimated seats")
        assertNull(secondDecision, "only 1 seat estimated left, not enough for a group of 4")
    }
}
