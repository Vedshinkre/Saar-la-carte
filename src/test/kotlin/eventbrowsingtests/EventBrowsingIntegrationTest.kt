package eventbrowsingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
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
import kotlin.test.assertFalse

/**
 * Integration test for the restaurant decision at the beginning of the simulation, using real
 * collaborators end to end: [Simulation] triggers the decision on tick 1 of evening 1, the real
 * [de.unisaarland.cs.se.selab.restaurant.BrowsingService] ranks the real restaurants using the
 * event seats that [Restaurant.prepareForEvening] published into their [RestaurantStats], the
 * chosen [Restaurant] keeps the group, and three evenings later [Restaurant.prepareForEvening]
 * reserves the real [Table]s for exactly the groups that were placed.
 */
class EventBrowsingIntegrationTest {
    private lateinit var output: StringWriter

    private val rice = Ingredient("rice", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)
    private val riceBowl = Recipe(
        1,
        "Rice Bowl",
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(rice to 10),
        basicDishFor = RestaurantType.EUROPEAN
    )

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

    private data class Setup(val stats: RestaurantStats, val restaurant: Restaurant, val tables: List<Table>)

    private fun realRestaurant(id: Int, tableSizes: List<Int>, positiveRatings: Int): Setup {
        val stats = RestaurantStats(
            restaurantId = id,
            restaurantType = RestaurantType.EUROPEAN,
            openingTickStart = 1,
            openingTickEnd = 24,
            event = true,
            positiveRatings = positiveRatings,
            negativeRatings = 0,
            menu = listOf(riceBowl)
        )
        val tables = tableSizes.mapIndexed { index, size -> Table(index + 1, size, TableType.COMMON) }
        val staff = RestaurantStaff(
            cooks = mutableListOf(Cook(1, null, CookType.TOURNANT, null, 0, false)),
            waiters = mutableListOf(Waiter().apply { this.id = 1 }),
            drivers = mutableListOf()
        )
        val restaurant = Restaurant(stats, "Restaurant $id", staff, tables, Stock(listOf(rice)))
        return Setup(stats, restaurant, tables)
    }

    private fun eventGroup(id: Int, size: Int, eventEvening: Int = 4): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 3,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = eventEvening,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "Rice Bowl")
    )

    @Test
    fun `event groups are placed on evening 1 by real seat data and get their tables reserved on the event evening`() {
        val best = realRestaurant(id = 1, tableSizes = listOf(4, 4), positiveRatings = 5)
        val other = realRestaurant(id = 2, tableSizes = listOf(4), positiveRatings = 0)
        val bigGroup = eventGroup(id = 1, size = 6)
        val fitsOnlyInOther = eventGroup(id = 2, size = 4)
        val noRoomLeft = eventGroup(id = 3, size = 4)
        val farAway = eventGroup(id = 4, size = 4, eventEvening = 9)

        val simulation = Simulation(
            SimulationConfig().apply {
                customers = listOf(farAway, noRoomLeft, bigGroup, fitsOnlyInOther)
                restaurantStats = listOf(best.stats, other.stats)
                restaurants = listOf(best.restaurant, other.restaurant)
            }
        )

        // evening 1, tick 1: seats are published by the restaurants, then the groups decide
        simulation.runSimulation()

        val decisions = output.toString().lines()
            .filter { it.contains("Restaurant Decision") || it.contains("No Decision") }
        assertEquals(
            listOf(
                "[DEBUG] Restaurant Decision: Group 1 decided on restaurant 1.",
                "[DEBUG] Restaurant Decision: Group 2 decided on restaurant 2.",
                "[DEBUG] Restaurant No Decision: Group 3 could not decide for a restaurant."
            ),
            decisions
        )
        assertEquals(listOf(bigGroup), best.restaurant.eventCustomers)
        assertEquals(listOf(fitsOnlyInOther), other.restaurant.eventCustomers)
        assertEquals(2, best.stats.availableEventSeats[TableType.COMMON])
        assertEquals(0, other.stats.availableEventSeats[TableType.COMMON])

        // evening 4: the restaurants turn the decisions into table reservations
        Time.evening = 4
        Time.tick = 1
        best.restaurant.prepareForEvening(emptyList())
        other.restaurant.prepareForEvening(emptyList())

        assertEquals(listOf(TableStatus.RESERVED, TableStatus.RESERVED), best.tables.map { it.status })
        assertEquals(listOf(TableStatus.RESERVED), other.tables.map { it.status })
        assertEquals(emptyList(), best.restaurant.eventCustomers)
        assertFalse(output.toString().contains("Group 4"))
    }
}
