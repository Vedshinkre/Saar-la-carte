package simulationstatisticsdeliverytests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import eventseatingoutcometests.EventSeatingFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import simulationstatisticsdeliverytests.StatisticsFixtures.deliveryGroup
import simulationstatisticsdeliverytests.StatisticsFixtures.restaurantMock
import simulationstatisticsdeliverytests.StatisticsFixtures.stats
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val ONE_TICK = 5
private const val EIGHT_TICKS = 8
private const val FIVE_TICK_TRIP_KM = 25
private const val TICKS_IN_ONE_EVENING = 24
private const val LATEST_VISITING_TICK = 21
private const val LATE_VISITING_TICK = 9
private const val SMALLEST_EVENT = 4
private const val TICKS_IN_ONE_AND_A_QUARTER_EVENINGS = 30
private const val RATINGS_BEFORE = 10
private const val NEGATIVE_BEFORE = 20
private const val DELIVERY_GROUP_SIZE = 2

/**
 * Person B's integration tests for F07: the delivered-customers and ratings counters through the real
 * [FrontOfHouse] and [Restaurant], and the statistics step of [Simulation] after the last tick.
 */
class StatisticsDeliveredAndRatedIntegrationTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setup() {
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

    private fun frontOfHouse(): FrontOfHouse = FrontOfHouse(
        tables = emptyList(),
        waiters = listOf(Waiter()),
        drivers = listOf(Driver()),
        countertop = EventSeatingFixtures.countertop()
    )

    /** a delivery group whose real order has just been placed at the kitchen and is now cooked */
    private fun cookedDelivery(foh: FrontOfHouse, group: CasualGroup): CasualGroup {
        foh.processArrival(group, EventSeatingFixtures.menu)
        requireNotNull(group.currentOrder).dishes.forEach { it.status = DishStatus.COOKED }
        return group
    }

    private fun deliveryTick(foh: FrontOfHouse) {
        foh.clearActionLoads()
        foh.processServing()
        foh.processDelivering()
        foh.processEating()
        Time.tick += 1
    }

    // ---- Delivered customers through the FrontOfHouse ----

    @Test
    fun `a delivery is counted on the tick the driver hands the food over and never again`() {
        val foh = frontOfHouse()
        cookedDelivery(foh, deliveryGroup(id = 1, distance = ONE_TICK, visitingAt = LATEST_VISITING_TICK))

        deliveryTick(foh) // hand-over to the driver and preparation
        assertEquals(0, foh.numberOfCustomersDelivered, "food is still with the restaurant or the driver")
        deliveryTick(foh) // the driver arrives and hands over
        assertEquals(DELIVERY_GROUP_SIZE, foh.numberOfCustomersDelivered)
        deliveryTick(foh)
        assertEquals(DELIVERY_GROUP_SIZE, foh.numberOfCustomersDelivered)
    }

    @Test
    fun `a delivery the group gave up on before the driver arrived is never counted`() {
        val foh = frontOfHouse()
        // wanted at tick 9, but the meals only reach the driver at tick 9 (a busy kitchen) and a 25 km trip
        // takes 5 ticks, so the group gives up at tick 12, before the driver arrives at tick 14
        Time.tick = LATE_VISITING_TICK
        cookedDelivery(foh, deliveryGroup(id = 1, distance = FIVE_TICK_TRIP_KM, visitingAt = LATE_VISITING_TICK))

        repeat(EIGHT_TICKS) { deliveryTick(foh) }

        assertTrue(log().contains("Delivery Failed"), log())
        assertEquals(0, foh.numberOfCustomersDelivered)
    }

    // ---- Ratings through the FrontOfHouse ----

    /**
     * A delivery group rates once, in the tick it gives up (its wanted tick plus the three ticks it
     * waits), and not because its meals are aborted.
     */
    @Test
    fun `turned away, event and given-up delivery groups each add one rating, groups that never rate add none`() {
        val foh = frontOfHouse()
        val regular = RegularGroup(1, 2, TableType.COMMON, 1, emptyList(), 1, 1, 1)
        val event = EventSeatingFixtures.eventGroup(2, SMALLEST_EVENT)
        foh.reserveTables(regular) // no tables at all, so both reservations fail and the groups are turned away
        foh.reserveTables(event)
        val rating = cookedDelivery(foh, deliveryGroup(id = 3, likelihood = RatingLikelihood.ALWAYS))
        val silent = cookedDelivery(foh, deliveryGroup(id = 4, likelihood = RatingLikelihood.NEVER))
        listOf(rating, silent).forEach { group ->
            group.experience = ExperienceType.NEGATIVE
            requireNotNull(group.currentOrder).deliveryGivenUp = true
        }
        Time.tick = rating.visitingAt + Constants.CUSTOMER_DELIVERY_WAIT_TICKS

        val (positive, negative) = foh.processRatings(RATINGS_BEFORE, NEGATIVE_BEFORE)

        assertEquals(RATINGS_BEFORE, positive)
        assertEquals(NEGATIVE_BEFORE + 3, negative)

        Time.tick += 1
        assertEquals(positive to negative, foh.processRatings(positive, negative), "every group rates only once")
    }

    // ---- Ratings through a real Restaurant ----

    @Test
    fun `a restaurant counts only the ratings of the simulation once, whatever it started with`() {
        val startedWith = stats(id = 1, positive = RATINGS_BEFORE, negative = NEGATIVE_BEFORE)
        val staff = RestaurantStaff(mutableListOf(), mutableListOf(), mutableListOf())
        val restaurant = Restaurant(startedWith, "Fire", staff, emptyList(), Stock(emptyList()))
        // no table exists, so this regular's reservation fails and they leave with a negative experience
        val regular = RegularGroup(1, 2, TableType.COMMON, 1, emptyList(), 1, 1, 1)
        restaurant.prepareForEvening(listOf(regular))

        restaurant.simulateTick()
        Time.tick = 2
        restaurant.simulateTick()

        assertEquals(NEGATIVE_BEFORE + 1, startedWith.negativeRatings)
        assertEquals(RATINGS_BEFORE, startedWith.positiveRatings)
        assertEquals(1, startedWith.simulationNegativeRatings, "the rating is only counted in the tick it is given")
        assertEquals(0, startedWith.simulationPositiveRatings)
    }

    // ---- The statistics step of the simulation ----

    @Test
    fun `after a whole evening the statistics follow the serving log with no further evening`() {
        Time.maxTicks = TICKS_IN_ONE_EVENING
        val restaurant = restaurantMock(stats(id = 1), cooked = 0, served = 0, delivered = 0)

        Simulation(SimulationConfig()).apply { restaurants = listOf(restaurant) }.runSimulation()

        val log = log()
        val servingEnds = log.indexOf("Serving of evening 1 ends.")
        val statistics = log.indexOf("Simulation Info: Simulation statistics are calculated.")
        assertTrue(servingEnds in 0 until statistics, log)
        assertFalse(log.contains("evening 2"), log)
    }

    @Test
    fun `a simulation ending mid evening reports its statistics right after the last restaurant end`() {
        Time.maxTicks = TICKS_IN_ONE_AND_A_QUARTER_EVENINGS
        val restaurant = restaurantMock(stats(id = 1), cooked = 0, served = 0, delivered = 0)

        Simulation(SimulationConfig()).apply { restaurants = listOf(restaurant) }.runSimulation()

        val log = log()
        val lastRestaurantEnd = log.lastIndexOf("Restaurant End")
        val statistics = log.indexOf("Simulation Info: Simulation statistics are calculated.")
        assertTrue(lastRestaurantEnd in 0 until statistics, log)
        assertFalse(log.contains("Serving of evening 2 ends."), log)
    }
}
