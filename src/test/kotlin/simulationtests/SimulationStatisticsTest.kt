package simulationtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SimulationStatisticsTest {
    private lateinit var output: StringWriter

    @BeforeTest
    fun setUp() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 1
        Time.ticksElapsed = 0
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.IMPORTANT)
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    @Test
    fun `statistics are logged in ascending restaurant order`() {
        val firstStats = stats(1)
        val secondStats = stats(2)
        val firstRestaurant = restaurant(firstStats, cooked = 4, served = 3, delivered = 1)
        val secondRestaurant = restaurant(secondStats, cooked = 8, served = 6, delivered = 2)
        val simulation = Simulation(
            SimulationConfig().apply {
                restaurants = mutableListOf(secondRestaurant, firstRestaurant)
            }
        )

        // calculateStatistics() is private; runSimulation() is the only public entry point,
        // and it always ends by calculating and logging statistics once the max ticks are reached.
        simulation.runSimulation()

        val statisticsLines = output.toString().trim().lines().filter {
            it.startsWith("[IMPORTANT] Simulation Info: Simulation statistics") ||
                it.startsWith("[IMPORTANT] Simulation Statistics:")
        }
        assertEquals(
            listOf(
                "[IMPORTANT] Simulation Info: Simulation statistics are calculated.",
                "[IMPORTANT] Simulation Statistics: Restaurant 1 cooked 4 meals.",
                "[IMPORTANT] Simulation Statistics: Restaurant 1 served 3 customers.",
                "[IMPORTANT] Simulation Statistics: Restaurant 1 delivered meals to 1 customers.",
                "[IMPORTANT] Simulation Statistics: Restaurant 1 received 0 ratings.",
                "[IMPORTANT] Simulation Statistics: Restaurant 2 cooked 8 meals.",
                "[IMPORTANT] Simulation Statistics: Restaurant 2 served 6 customers.",
                "[IMPORTANT] Simulation Statistics: Restaurant 2 delivered meals to 2 customers.",
                "[IMPORTANT] Simulation Statistics: Restaurant 2 received 0 ratings."
            ),
            statisticsLines
        )
    }

    private fun stats(id: Int) = RestaurantStats(
        restaurantId = id,
        restaurantType = RestaurantType.ASIAN,
        openingTickStart = 1,
        openingTickEnd = 24,
        event = false,
        positiveRatings = 5,
        negativeRatings = 2,
        menu = emptyList()
    )

    private fun restaurant(
        stats: RestaurantStats,
        cooked: Int,
        served: Int,
        delivered: Int
    ): Restaurant = mock {
        on { getRestaurantStats() } doReturn stats
        on { getNumberOfCookedMeals() } doReturn cooked
        on { getNumberOfCustomersServed() } doReturn served
        on { getNumberOfCustomersDelivered() } doReturn delivered
    }
}
