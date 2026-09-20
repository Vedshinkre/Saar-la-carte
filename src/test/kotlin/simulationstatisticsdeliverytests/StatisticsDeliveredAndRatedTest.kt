package simulationstatisticsdeliverytests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.helpers.DeliveryProcessor
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import simulationstatisticsdeliverytests.StatisticsFixtures.deliveryGroup
import simulationstatisticsdeliverytests.StatisticsFixtures.drivingDriver
import simulationstatisticsdeliverytests.StatisticsFixtures.eatingProcessor
import simulationstatisticsdeliverytests.StatisticsFixtures.order
import simulationstatisticsdeliverytests.StatisticsFixtures.ratingProcessor
import simulationstatisticsdeliverytests.StatisticsFixtures.restaurantMock
import simulationstatisticsdeliverytests.StatisticsFixtures.stats
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val FIRST_TICK_AFTER_HAND_OVER = 6
private const val HAND_OVER_TICK = 5
private const val TRIP_TICKS = 5
private const val MAX_DRIVING_TICKS = 8
private const val THREE = 3
private const val FIVE = 5

/**
 * Person B's unit tests for F07: the delivered-customers counter (only successful deliveries count),
 * the ratings counter, and the final statistics log written after the simulation has ended.
 */
class StatisticsDeliveredAndRatedTest {

    private lateinit var output: StringWriter
    private var delivered = 0

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
        delivered = 0
    }

    @AfterEach
    fun tearDown() {
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    private fun log(): String = output.toString()

    /** runs the statistics step alone (the simulation is over before its first tick) */
    private fun runStatistics(vararg restaurants: Restaurant) {
        Simulation(SimulationConfig()).apply { this.restaurants = restaurants.toList() }.runSimulation()
    }

    // ---- Delivered customers ----

    @Test
    fun `a delivery counts the whole group once, on the tick the driver hands over the food`() {
        val group = deliveryGroup(id = 1, size = 2)
        group.currentOrder = order(DishStatus.SERVED).apply { deliveredAt = HAND_OVER_TICK }
        val eating = eatingProcessor(listOf(group)) { delivered += it }

        Time.tick = HAND_OVER_TICK
        eating.processEating()
        Time.tick = FIRST_TICK_AFTER_HAND_OVER
        eating.processEating()

        assertEquals(2, delivered)
    }

    @Test
    fun `deliveries handed over in the same tick add up their group sizes`() {
        val groups = listOf(deliveryGroup(id = 1, size = 2), deliveryGroup(id = 2, size = THREE))
        groups.forEach { it.currentOrder = order(DishStatus.SERVED).apply { deliveredAt = HAND_OVER_TICK } }

        Time.tick = HAND_OVER_TICK
        eatingProcessor(groups) { delivered += it }.processEating()

        assertEquals(FIVE, delivered)
    }

    @Test
    fun `a delivery rejected on arrival because its order was aborted does not count`() {
        val group = deliveryGroup(id = 1)
        val aborted = order(DishStatus.ABORTED)
        val delivering = DeliveryProcessor(listOf(drivingDriver(group, aborted, oneWayTicks = 1)), listOf(group))

        repeat(MAX_DRIVING_TICKS) {
            Time.tick += 1
            delivering.processDelivering()
            eatingProcessor(listOf(group)) { delivered += it }.processEating()
        }

        assertTrue(log().contains("Delivery Failed"), log())
        assertEquals(0, delivered)
    }

    @Test
    fun `a delivery the group gave up on does not count even though the driver still arrives`() {
        // the group wanted the food at tick 1 and gives up three ticks later, before the 5 tick trip is over
        val group = deliveryGroup(id = 1, visitingAt = 1)
        val driver = drivingDriver(group, order(DishStatus.SERVED), oneWayTicks = TRIP_TICKS)
        val delivering = DeliveryProcessor(listOf(driver), listOf(group))
        Time.tick = 1

        repeat(MAX_DRIVING_TICKS) {
            Time.tick += 1
            delivering.processDelivering()
            eatingProcessor(listOf(group)) { delivered += it }.processEating()
        }

        assertTrue(log().contains("Delivery Failed"), log())
        assertEquals(0, delivered)
    }

    @Test
    fun `a delivery aborted at the end of the evening does not count`() {
        val group = deliveryGroup(id = 1)
        val driver = drivingDriver(group, order(DishStatus.SERVED), oneWayTicks = TRIP_TICKS)
        val foh = FrontOfHouse(emptyList(), emptyList(), listOf(driver), mock())

        foh.resetDrivers()
        Time.tick = HAND_OVER_TICK
        eatingProcessor(listOf(group)) { delivered += it }.processEating()

        assertTrue(requireNotNull(group.currentOrder).dishes.all { it.status == DishStatus.ABORTED })
        assertEquals(0, delivered)
    }

    // ---- Ratings ----

    @Test
    fun `a group that rates adds one to the positive or the negative counter`() {
        val happy = deliveryGroup(id = 1, likelihood = RatingLikelihood.ALWAYS)
        val unhappy = deliveryGroup(id = 2, likelihood = RatingLikelihood.ALWAYS)
        unhappy.experience = ExperienceType.NEGATIVE

        val afterHappy = ratingProcessor().rate(happy, 0, 0)
        val afterUnhappy = ratingProcessor().rate(unhappy, 0, 0)

        assertEquals(1 to 0, afterHappy)
        assertEquals(0 to 1, afterUnhappy)
    }

    @Test
    fun `a group that never rates adds nothing to either counter`() {
        val silent = deliveryGroup(id = 1, likelihood = RatingLikelihood.NEVER)
        silent.experience = ExperienceType.NEGATIVE

        val counters = ratingProcessor().rate(silent, FIVE, THREE)

        assertEquals(FIVE to THREE, counters)
    }

    @Test
    fun `the ratings a restaurant starts with in the JSON are not part of the received count`() {
        val startedWithRatings = stats(id = 1, positive = FIVE, negative = THREE)

        runStatistics(restaurantMock(startedWithRatings, cooked = 0, served = 0, delivered = 0))

        assertTrue(log().contains("Simulation Statistics: Restaurant 1 received 0 ratings."), log())
    }

    @Test
    fun `the received count is the positive plus the negative ratings given during the simulation`() {
        val rated = stats(id = 1, positive = FIVE, negative = THREE).apply {
            simulationPositiveRatings = 2
            simulationNegativeRatings = 1
        }

        runStatistics(restaurantMock(rated, cooked = 0, served = 0, delivered = 0))

        assertTrue(log().contains("Simulation Statistics: Restaurant 1 received 3 ratings."), log())
    }

    // ---- The final log ----

    @Test
    fun `the statistics log lists restaurants by ascending id, four lines each, zeros included`() {
        val busy = restaurantMock(stats(id = 1), cooked = 7, served = FIVE, delivered = 2)
        val idle = restaurantMock(stats(id = THREE), cooked = 0, served = 0, delivered = 0)

        runStatistics(idle, busy)

        val lines = log().lines().filter { it.contains("Simulation") }.map { it.substringAfter("] ") }
        assertEquals(
            listOf(
                "Simulation Info: Simulation started.",
                "Simulation Info: Simulation statistics are calculated.",
                "Simulation Statistics: Restaurant 1 cooked 7 meals.",
                "Simulation Statistics: Restaurant 1 served 5 customers.",
                "Simulation Statistics: Restaurant 1 delivered meals to 2 customers.",
                "Simulation Statistics: Restaurant 1 received 0 ratings.",
                "Simulation Statistics: Restaurant 3 cooked 0 meals.",
                "Simulation Statistics: Restaurant 3 served 0 customers.",
                "Simulation Statistics: Restaurant 3 delivered meals to 0 customers.",
                "Simulation Statistics: Restaurant 3 received 0 ratings."
            ),
            lines
        )
    }
}
