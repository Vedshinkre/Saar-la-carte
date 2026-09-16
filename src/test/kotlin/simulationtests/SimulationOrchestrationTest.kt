package simulationtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class SimulationOrchestrationTest {
    private lateinit var output: StringWriter

    @BeforeTest
    fun setUp() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 1
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
    }

    @Test
    fun `runSimulation applies current evening incidents in id order and stops at max ticks`() {
        val laterIncident = incident(id = 2, evening = 1)
        val earlierIncident = incident(id = 1, evening = 1)
        val futureIncident = incident(id = 3, evening = 2)
        val simulation = Simulation(
            SimulationConfig().apply {
                incidents = listOf(laterIncident, futureIncident, earlierIncident)
            }
        )

        simulation.runSimulation()

        inOrder(earlierIncident, laterIncident) {
            verify(earlierIncident).apply()
            verify(laterIncident).apply()
        }
        verify(futureIncident, never()).apply()
        assertContains(output.toString(), "[IMPORTANT] Serving: Serving of evening 1 starts.")
        assertFalse(output.toString().contains("Serving of evening 1 ends."))
        assertContains(output.toString(), "[IMPORTANT] Simulation Info: Simulation statistics are calculated.")
    }

    @Test
    fun `future event group is reserved at its eligible restaurant`() {
        val eventGroup = EventGroup(
            id = 4,
            size = 2,
            tableType = TableType.COMMON,
            visitingAt = 3,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.ASIAN),
            eventEvening = 4,
            eventDishes = emptyMap()
        )
        val stats = RestaurantStats(
            restaurantId = 1,
            restaurantType = RestaurantType.ASIAN,
            openingTickStart = 1,
            openingTickEnd = 24,
            event = true,
            positiveRatings = 0,
            negativeRatings = 0,
            menu = emptyList()
        )
        stats.availableEventSeats[TableType.COMMON] = 2
        val reservedGroups = mutableListOf<EventGroup>()
        val restaurant = mock<Restaurant> {
            on { getRestaurantStats() } doReturn stats
            on { eventCustomers } doReturn reservedGroups
        }
        val browser = mock<BrowsingService> {
            on { getEligibleRestaurants(eventGroup) } doReturn 1
        }
        val simulation = Simulation(SimulationConfig()).apply {
            this.browser = browser
            restaurants = listOf(restaurant)
        }

        invokePrivate(simulation, "reserveForEventGroupsInAdvance", listOf(eventGroup))

        assertEquals(listOf(eventGroup), reservedGroups)
        assertSame(RestaurantType.ASIAN, eventGroup.currentRestaurantType)
    }

    private fun incident(id: Int, evening: Int): Incident = mock {
        on { this.id } doReturn id
        on { this.evening } doReturn evening
    }

    private fun invokePrivate(simulation: Simulation, methodName: String, argument: Any) {
        val method = Simulation::class.java.getDeclaredMethod(methodName, List::class.java)
        method.isAccessible = true
        method.invoke(simulation, argument)
    }
}
