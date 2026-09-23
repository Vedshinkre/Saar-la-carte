package customertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * integration tests for P04 (Customer - Events) for an EVENT group booking three evenings ahead
 * and turning up on the event evening, driven by a real [Simulation]
 * ordering through the front of house tested by Deniz
 */
class EventCustomerScheduleIntegrationTest {
    private lateinit var output: StringWriter

    private val water = Ingredient("water", MeasurementUnit.ML, bestBefore = 9, initialPackagingVolume = 1000)
    private val soup = Recipe(
        1,
        "soup",
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(water to 1),
        basicDishFor = RestaurantType.EUROPEAN
    )

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Time.tick = 1
        Time.evening = 1
        Time.ticksElapsed = 0
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    private val tickHeader = Regex("""Tick (\d+) \((\d+)\) started""")

    /** The (evening, tick-in-evening) at which the first line containing [marker] was logged. */
    private fun whenLogged(marker: String): Pair<Int, Int>? {
        var evening = 0
        var tickInEvening = 0
        for (line in output.toString().lines()) {
            val header = tickHeader.find(line)
            if (header != null) {
                tickInEvening = header.groupValues[1].toInt()
                evening = header.groupValues[2].toInt()
            }
            if (line.contains(marker)) return evening to tickInEvening
        }
        return null
    }

    private fun run(groups: List<EventGroup>, evenings: Int, hostsEvents: Boolean = true) {
        val stats = RestaurantStats(
            restaurantId = 1,
            restaurantType = RestaurantType.EUROPEAN,
            openingTickStart = 1,
            openingTickEnd = 24,
            event = hostsEvents,
            positiveRatings = 0,
            negativeRatings = 0,
            menu = listOf(soup)
        )
        val staff = RestaurantStaff(
            cooks = mutableListOf(Cook(CookType.TOURNANT)),
            waiters = mutableListOf(Waiter(), Waiter()),
            drivers = mutableListOf()
        )
        val restaurant = Restaurant(
            stats,
            "Soup Kitchen",
            staff,
            listOf(Table(1, 8, TableType.COMMON)),
            Stock(listOf(water))
        )
        Time.maxTicks = evenings * 24
        Simulation(
            SimulationConfig().apply {
                customers = groups
                restaurantStats = listOf(stats)
                restaurants = listOf(restaurant)
                stock = Stock(listOf(water))
            }
        ).runSimulation()
    }

    private fun event(id: Int, size: Int = 6, eventEvening: Int = 4, visitingAt: Int = 5) = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = eventEvening,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "soup")
    )

    /** The reservation is made in the first tick of the evening three before the event. */
    @Test
    fun `an event group books in the first tick three evenings before the event`() {
        run(listOf(event(1, eventEvening = 4)), evenings = 4)

        assertEquals(1 to 1, whenLogged("Group 1 decided on restaurant 1"))
    }

    /** ... and only turns up on the event evening itself, at its visiting tick. */
    @Test
    fun `an event group arrives on its event evening at its visiting tick`() {
        run(listOf(event(1, eventEvening = 4, visitingAt = 5)), evenings = 4)

        assertEquals(4 to 5, whenLogged("Group 1 arrived at restaurant 1"))
    }

    @Test
    fun `an event group arrives exactly once over the whole simulation`() {
        run(listOf(event(1, eventEvening = 4)), evenings = 4)

        val arrivals = output.toString().lines().count { it.contains("Group 1 arrived at restaurant 1") }
        assertEquals(1, arrivals)
    }

    /** A later event is booked later: the booking evening follows the event evening. */
    @Test
    fun `a later event is booked three evenings before that later evening`() {
        run(listOf(event(1, eventEvening = 6)), evenings = 4)

        assertEquals(3 to 1, whenLogged("Group 1 decided on restaurant 1"))
        assertNull(whenLogged("Group 1 arrived"), "the event evening is not reached in four evenings")
    }

    /** Without a restaurant that hosts events the group books nowhere and never turns up. */
    @Test
    fun `an event group that finds no host never arrives`() {
        run(listOf(event(1, eventEvening = 4)), evenings = 4, hostsEvents = false)

        assertTrue(
            output.toString().lines().any { it.contains("Group 1 could not decide for a restaurant") },
            "the group should report that it found nothing"
        )
        assertNull(whenLogged("Group 1 arrived"))
    }

    /** The table reserved for the event is held from the start of the event evening. */
    @Test
    fun `the event group is seated on the table reserved for it`() {
        run(listOf(event(1, size = 6, eventEvening = 4)), evenings = 4)

        assertTrue(
            output.toString().lines().any { it.contains("FOH Seating (R 1): Group 1 seated at table 1") },
            "the group should be seated on its reserved table"
        )
    }
}
