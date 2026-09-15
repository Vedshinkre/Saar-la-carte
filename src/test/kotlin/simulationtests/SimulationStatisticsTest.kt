package simulationtests

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class SimulationStatisticsTest {
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
        val output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.IMPORTANT)

        invokeCalculateStatistics(simulation)

        val lines = output.toString().trim().lines()
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
            lines
        )
        verify(firstRestaurant).getRestaurantStats()
        verify(firstRestaurant).getNumberOfCookedMeals()
        verify(firstRestaurant).getNumberOfCustomersServed()
        verify(firstRestaurant).getNumberOfCustomersDelivered()
        verify(secondRestaurant).getRestaurantStats()
        verify(secondRestaurant).getNumberOfCookedMeals()
        verify(secondRestaurant).getNumberOfCustomersServed()
        verify(secondRestaurant).getNumberOfCustomersDelivered()
    }

    private fun invokeCalculateStatistics(simulation: Simulation) {
        val method = Simulation::class.java.getDeclaredMethod("calculateStatistics")
        method.isAccessible = true
        method.invoke(simulation)
    }

    private fun stats(id: Int) = RestaurantStats(
        restaurantId = id,
        restaurantType = RestaurantType.ASIAN,
        openingTickStart = 1,
        openingTickEnd = 24,
        event = false,
        positiveRatings = 0,
        negativeRatings = 0,
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
