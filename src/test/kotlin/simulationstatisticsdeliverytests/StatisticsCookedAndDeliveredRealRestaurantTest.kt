package simulationstatisticsdeliverytests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import eventseatingoutcometests.EventSeatingFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val GROUP_SIZE = 2
private const val OPENING_TICK_END = 24
private const val ORDER_TICK = 1
private const val HAND_OVER_TICK = 2

/** a 5 km trip is one tick each way; ordering 3 (cooking) + 1 (drive) ticks before visitingAt = 5 lands on tick 1 */
private const val DELIVERY_VISITING_AT = 5

/**
 * F07 through a real [Restaurant] instead of mocks: an actual [Cook] cooks the dish (not a stand-in
 * with statuses forced to COOKED) and a real [Driver] drives it (not a bare FrontOfHouse). Same idea
 * as [StatisticsServedTest], just for [Restaurant.getNumberOfCookedMeals] and
 * [Restaurant.getNumberOfCustomersDelivered].
 */
class StatisticsCookedAndDeliveredRealRestaurantTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
        Order.resetIds()
    }

    @AfterEach
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    private fun log(): String = output.toString()

    /** Needs no pantry stock, so ordering never depends on ingredient availability or planning. */
    private val instantRecipe = Recipe(
        id = 1,
        name = "Instant",
        duration = 1,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(),
        basicDishFor = null
    )

    private fun statsFor(id: Int): RestaurantStats = RestaurantStats(
        restaurantId = id,
        restaurantType = RestaurantType.EUROPEAN,
        openingTickStart = 1,
        openingTickEnd = OPENING_TICK_END,
        event = false,
        positiveRatings = 0,
        negativeRatings = 0,
        menu = listOf(instantRecipe)
    )

    /** A dine-in restaurant with one idle, qualified cook who is free to actually cook the order. */
    private fun realCookingRestaurant(): Restaurant {
        val table = Table(id = 1, size = GROUP_SIZE, tableType = TableType.COMMON)
        val cook = Cook(CookType.TOURNANT)
        val staff = RestaurantStaff(mutableListOf(cook), mutableListOf(Waiter()), mutableListOf())
        return Restaurant(statsFor(1), "Real Kitchen", staff, listOf(table), Stock(emptyList()))
    }

    /** A delivery-only restaurant (no tables) with one idle cook, waiter and driver. */
    private fun realDeliveringRestaurant(): Restaurant {
        val cook = Cook(CookType.TOURNANT)
        val staff = RestaurantStaff(mutableListOf(cook), mutableListOf(Waiter()), mutableListOf(Driver()))
        return Restaurant(statsFor(1), "Real Kitchen", staff, emptyList(), Stock(emptyList()))
    }

    // ---- cooked meals, through a real kitchen

    @Test
    fun `a real, free cook finishing a dish increases getNumberOfCookedMeals by the dishes cooked`() {
        val restaurant = realCookingRestaurant()
        val regular = EventSeatingFixtures.regularGroup(id = 1, size = GROUP_SIZE)
        restaurant.prepareForEvening(listOf(regular))

        // the 1-tick recipe is ordered, assigned to the free cook and finished in this same tick
        Time.tick = ORDER_TICK
        restaurant.simulateTick()

        assertEquals(GROUP_SIZE, restaurant.getNumberOfCookedMeals())
    }

    @Test
    fun `a real restaurant's cooked count feeds the final simulation statistics log`() {
        val restaurant = realCookingRestaurant()
        val regular = EventSeatingFixtures.regularGroup(id = 1, size = GROUP_SIZE)
        restaurant.prepareForEvening(listOf(regular))
        Time.tick = ORDER_TICK
        restaurant.simulateTick()

        Simulation(SimulationConfig()).apply { restaurants = listOf(restaurant) }.runSimulation()

        assertTrue(
            log().contains("Simulation Statistics: Restaurant 1 cooked $GROUP_SIZE meals."),
            log()
        )
    }

    // ---- delivered customers, through a real driver

    @Test
    fun `a real driver's hand-over increases getNumberOfCustomersDelivered, not the preparation tick`() {
        val restaurant = realDeliveringRestaurant()
        val group = StatisticsFixtures.deliveryGroup(id = 1, size = GROUP_SIZE, visitingAt = DELIVERY_VISITING_AT)
        restaurant.prepareForEvening(emptyList())
        restaurant.addToCustomerQueue(group)

        // ordered, cooked, served to the driver and prepared for the trip, all in this same tick
        Time.tick = ORDER_TICK
        restaurant.simulateTick()
        assertEquals(0, restaurant.getNumberOfCustomersDelivered(), "food is still with the restaurant or the driver")

        // the one-tick trip out and the hand-over
        Time.tick = HAND_OVER_TICK
        restaurant.simulateTick()
        assertEquals(GROUP_SIZE, restaurant.getNumberOfCustomersDelivered())
    }

    @Test
    fun `a real restaurant's delivered count feeds the final simulation statistics log`() {
        val restaurant = realDeliveringRestaurant()
        val group = StatisticsFixtures.deliveryGroup(id = 1, size = GROUP_SIZE, visitingAt = DELIVERY_VISITING_AT)
        restaurant.prepareForEvening(emptyList())
        restaurant.addToCustomerQueue(group)

        Time.tick = ORDER_TICK
        restaurant.simulateTick()
        Time.tick = HAND_OVER_TICK
        restaurant.simulateTick()

        Simulation(SimulationConfig()).apply { restaurants = listOf(restaurant) }.runSimulation()

        assertTrue(
            log().contains("Simulation Statistics: Restaurant 1 delivered meals to $GROUP_SIZE customers."),
            log()
        )
    }
}
