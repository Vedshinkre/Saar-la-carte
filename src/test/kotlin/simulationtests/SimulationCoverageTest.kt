package simulationtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains

class SimulationCoverageTest {

    private lateinit var output: StringWriter

    @BeforeTest
    fun setUp() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 1
        Time.ticksElapsed = 0
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    @Test
    fun `regular group is filtered correctly among multiple restaurants`() {
        val regularGroup = mock<RegularGroup> {
            on { id } doReturn 77
            on { restaurantId } doReturn 2
            on { isVisitingTonight() } doReturn true
        }
        val stats1 = RestaurantStats(1, RestaurantType.AFRICAN, 1, 24, false, 0, 0, emptyList())
        val rest1 = mock<Restaurant> { on { getRestaurantStats() } doReturn stats1 }

        val stats2 = RestaurantStats(2, RestaurantType.EUROPEAN, 1, 24, false, 0, 0, emptyList())
        val rest2 = mock<Restaurant> { on { getRestaurantStats() } doReturn stats2 }

        val simulation = Simulation(
            SimulationConfig().apply { customers = listOf(regularGroup) }
        ).apply {
            this.restaurants = listOf(rest1, rest2)
            this.browser = mock()
        }

        Time.maxTicks = 1
        simulation.runSimulation()

        verify(rest1).prepareForEvening(emptyList())
        verify(rest2).prepareForEvening(listOf(regularGroup))
    }

    @Test
    fun `simulation end is exactly at the end of an evening`() {
        Time.maxTicks = 24
        val stats = RestaurantStats(1, RestaurantType.AMERICAN, 1, 24, false, 0, 0, emptyList())
        val rest = mock<Restaurant> { on { getRestaurantStats() } doReturn stats }

        val simulation = Simulation(SimulationConfig()).apply {
            this.restaurants = listOf(rest)
            this.browser = mock()
        }

        simulation.runSimulation()

        assertContains(output.toString(), "[IMPORTANT] Serving: Serving of evening 1 ends.")
    }

    @Test
    fun `casual group - no eligible restaurant `() {
        val casualGroup = mock<CasualGroup> {
            on { id } doReturn 47
            on { isVisitingTonight() } doReturn true
            on { isVisitingThisTick() } doReturn true
        }
        val browser = mock<BrowsingService> {
            on { getEligibleRestaurants(casualGroup) } doReturn null
        }

        val stats = RestaurantStats(1, RestaurantType.ASIAN, 1, 24, false, 0, 0, emptyList())
        val restaurant = mock<Restaurant> {
            on { getRestaurantStats() } doReturn stats
        }

        val simulation = Simulation(
            SimulationConfig().apply { customers = listOf(casualGroup) }
        ).apply {
            this.browser = browser
            this.restaurants = listOf(restaurant)
        }

        Time.maxTicks = 1
        simulation.runSimulation()

        assertContains(output.toString(), "[DEBUG] Restaurant No Decision: Group 47 could not decide for a restaurant.")
    }

    @Test
    fun `getRestaurantById- find second restaurant for event group`() {
        val eventGroup = de.unisaarland.cs.se.selab.customer.EventGroup(
            id = 10,
            size = 4,
            tableType = TableType.COMMON,
            visitingAt = 5,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.AFRICAN),
            eventEvening = 4,
            eventDishes = emptyMap()
        )

        // restaurant mismatch
        val stats1 = RestaurantStats(1, RestaurantType.ASIAN, 1, 24, true, 0, 0, emptyList())
        val rest1 = mock<Restaurant> { on { getRestaurantStats() } doReturn stats1 }

        // restaurant match
        val stats2 = RestaurantStats(2, RestaurantType.AFRICAN, 1, 24, true, 0, 0, emptyList())
        val rest2 = mock<Restaurant> { on { getRestaurantStats() } doReturn stats2 }

        val browser = mock<BrowsingService> {
            on { getEligibleRestaurants(eventGroup) } doReturn 2
        }

        val simulation = Simulation(
            SimulationConfig().apply {
                customers = listOf(eventGroup)
                incidents = emptyList()
            }
        ).apply {
            this.restaurants = listOf(rest1, rest2)
            this.browser = browser
        }

        Time.maxTicks = 1
        simulation.runSimulation()

        assertContains(output.toString(), "[DEBUG] Restaurant Decision: Group 10 decided on restaurant 2.")
    }

    @Test
    fun `simulation with zero max ticks`() {
        Time.maxTicks = 0
        val simulation = Simulation(SimulationConfig()).apply {
            this.restaurants = emptyList()
            this.browser = mock()
        }

        simulation.runSimulation()

        // Verifies the simulation ran and calculated stats
        assertContains(output.toString(), "[IMPORTANT] Simulation Info: Simulation statistics are calculated.")
    }
}
