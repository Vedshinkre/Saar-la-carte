package customertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the visiting schedule of CasualGroups in a running Simulation.
 */
class CasualVisitScheduleIntegrationTest {
    private lateinit var output: StringWriter

    private val water = Ingredient("water", MeasurementUnit.ML, 9, 1000)
    private val soup = Recipe(
        1,
        "soup",
        10,
        listOf(CookType.TOURNANT),
        mutableMapOf(water to 1),
        RestaurantType.EUROPEAN
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

    private val tickHeader = Regex("""Tick (\d+) \((\d+)\) started""")

    private fun whenLogged(marker: String): Pair<Int, Int>? {
        var evening = 0
        var tickInEvening = 0
        for (line in output.toString().lines()) {
            val header = tickHeader.find(line)
            if (header != null) {
                tickInEvening = header.groupValues[1].toInt()
                evening = header.groupValues[2].toInt()
            }
            if (line.contains(marker)) return Pair(evening, tickInEvening)
        }
        return null
    }

    private fun restaurant(drivers: Int = 0): Pair<RestaurantStats, Restaurant> {
        val restaurantStats = RestaurantStats(
            1,
            RestaurantType.EUROPEAN,
            1,
            24,
            false,
            0,
            0,
            listOf(soup)
        )
        val restaurantStaff = RestaurantStaff(
            mutableListOf(Cook(CookType.TOURNANT)),
            mutableListOf(Waiter()),
            MutableList(drivers) { Driver() }
        )
        val tables = listOf(Table(1, 4, TableType.COMMON))
        return Pair(
            restaurantStats,
            Restaurant(restaurantStats, "Soup Kitchen", restaurantStaff, tables, Stock(listOf(water)))
        )
    }

    private fun casualGroup(
        id: Int,
        visitingEvenings: List<Int>,
        visitingAt: Int,
        distance: Int = 0
    ) = CasualGroup(
        id,
        2,
        TableType.COMMON,
        visitingAt,
        List(2) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        listOf(RestaurantType.EUROPEAN),
        visitingEvenings,
        distance,
        RatingLikelihood.NEVER
    )

    private fun runSimulation(casualGroups: List<CasualGroup>, evenings: Int, drivers: Int = 0) {
        val (restaurantStats, restaurant) = restaurant(drivers)
        Time.maxTicks = evenings * 24
        Simulation(
            SimulationConfig().apply {
                customers = casualGroups
                this.restaurantStats = listOf(restaurantStats)
                restaurants = listOf(restaurant)
                stock = Stock(listOf(water))
            }
        ).runSimulation()
    }

    @Test
    fun `Casual Group Walks In On Listed Evening At Visiting Tick`() {
        runSimulation(listOf(casualGroup(1, visitingEvenings = listOf(2), visitingAt = 5)), evenings = 2)

        assertEquals(Pair(2, 5), whenLogged("Group 1 arrived at restaurant 1"))
    }

    @Test
    fun `Casual Group Never Appears On Unlisted Evening`() {
        runSimulation(listOf(casualGroup(1, visitingEvenings = listOf(4), visitingAt = 5)), evenings = 2)

        assertNull(whenLogged("Group 1 arrived"))
        assertTrue(output.toString().lines().none { it.contains("Group 1 decided on restaurant") })
    }

    @Test
    fun `Casual Group Visits Each Listed Evening`() {
        runSimulation(listOf(casualGroup(1, visitingEvenings = listOf(1, 3), visitingAt = 4)), evenings = 3)

        val arrivals = output.toString().lines().count { it.contains("Group 1 arrived at restaurant 1") }
        assertEquals(2, arrivals)
        assertEquals(Pair(1, 4), whenLogged("Group 1 arrived at restaurant 1"))
    }

    @Test
    fun `Delivery Group Orders Before Visiting Tick`() {
        runSimulation(
            listOf(casualGroup(1, visitingEvenings = listOf(1), visitingAt = 20, distance = 10)),
            evenings = 1,
            drivers = 1
        )

        assertEquals(Pair(1, 15), whenLogged("Group 1 placed order"))
    }

    @Test
    fun `Delivery Group Further Away Orders Earlier`() {
        runSimulation(
            listOf(casualGroup(1, visitingEvenings = listOf(1), visitingAt = 20, distance = 25)),
            evenings = 1,
            drivers = 1
        )

        assertEquals(Pair(1, 12), whenLogged("Group 1 placed order"))
    }
}
