package seatingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * tests to cover the table seating rules
 */
class SeatingAndReservationOrderTest {
    private lateinit var output: StringWriter

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
        Time.evening = 1
        Time.ticksElapsed = 0
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.ticksElapsed = 0
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter> = emptyList()): FrontOfHouse {
        val countertop = Countertop(
            pantry = Pantry(Stock(emptyList<Ingredient>())),
            orderQueue = ArrayDeque(),
            cooks = emptyList(),
            restaurantType = RestaurantType.EUROPEAN
        )
        return FrontOfHouse(tables = tables, waiters = waiters, drivers = emptyList(), countertop = countertop)
    }

    private val eventDish = Recipe(
        id = 1,
        name = "pasta",
        duration = 1,
        cookType = listOf(CookType.EXEC),
        ingredients = mutableMapOf(),
        basicDishFor = RestaurantType.EUROPEAN
    )

    private fun restaurant(
        tables: List<Table>,
        waiters: List<Waiter> = emptyList(),
        menu: List<Recipe> = emptyList(),
        cooks: List<Cook> = emptyList()
    ): Restaurant = Restaurant(
        restaurantStats = RestaurantStats(
            restaurantId = 1,
            restaurantType = RestaurantType.EUROPEAN,
            openingTickStart = 1,
            openingTickEnd = 20,
            event = false,
            positiveRatings = 0,
            negativeRatings = 0,
            menu = menu
        ),
        name = "Testaurant",
        staff = RestaurantStaff(cooks.toMutableList(), waiters.toMutableList(), mutableListOf()),
        tables = tables,
        stock = Stock(emptyList())
    )

    private fun regularGroup(id: Int, size: Int, tableType: TableType = TableType.COMMON) = RegularGroup(
        id = id,
        size = size,
        tableType = tableType,
        visitingAt = 5,
        foodPreferences = emptyList(),
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1
    )

    private fun eventGroup(id: Int, size: Int, tableType: TableType = TableType.COMMON) = EventGroup(
        id = id,
        size = size,
        tableType = tableType,
        visitingAt = 5,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = 1,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "pasta")
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    private fun preferencesPerCustomer(size: Int) =
        List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) }

    private fun casualGroup(id: Int, size: Int, visitingAt: Int = 5) = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = preferencesPerCustomer(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.ALWAYS
    )

    // --- EVENT groups are reserved for first ---

    @Test
    fun `the only table goes to the event group, not to the regular group`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val restaurant = restaurant(listOf(table), menu = listOf(eventDish))
        val event = eventGroup(id = 7, size = 4)
        restaurant.eventCustomers.add(event)

        restaurant.prepareForEvening(listOf(regularGroup(id = 2, size = 4)))

        val noReservation = lines().filter { it.contains("FOH No Reserving") }
        assertEquals(1, noReservation.size, "expected exactly one failed reservation, got ${lines()}")
        assertTrue(
            noReservation.single().contains("group 2."),
            "the regular group should have lost the table, got ${noReservation.single()}"
        )
    }

    @Test
    fun `a regular group still gets a table when the event group has one of its own`() {
        val tables = listOf(
            Table(id = 1, size = 4, tableType = TableType.COMMON),
            Table(id = 2, size = 4, tableType = TableType.COMMON)
        )
        val restaurant = restaurant(tables, menu = listOf(eventDish))
        restaurant.eventCustomers.add(eventGroup(id = 7, size = 4))

        restaurant.prepareForEvening(listOf(regularGroup(id = 2, size = 4)))

        assertTrue(lines().none { it.contains("FOH No Reserving") }, "unexpected failure in ${lines()}")
        // the event group reserves first, so it takes the lowest-id table
        assertEquals(listOf(TableStatus.RESERVED, TableStatus.RESERVED), tables.map { it.status })
    }

    // --- BAR tables cannot be merged ---

    @Test
    fun `two bar tables are never merged for a group that is too large for either`() {
        val foh = frontOfHouse(
            listOf(
                Table(id = 1, size = 2, tableType = TableType.BAR),
                Table(id = 2, size = 2, tableType = TableType.BAR)
            )
        )

        assertFalse(foh.reserveTables(regularGroup(id = 1, size = 4, tableType = TableType.BAR)))
    }

    @Test
    fun `common tables of the same size are still merged`() {
        val foh = frontOfHouse(
            listOf(
                Table(id = 1, size = 2, tableType = TableType.COMMON),
                Table(id = 2, size = 2, tableType = TableType.COMMON)
            )
        )

        assertTrue(foh.reserveTables(regularGroup(id = 1, size = 4, tableType = TableType.COMMON)))
    }

    @Test
    fun `a single bar table that fits is still reserved`() {
        val barTable = Table(id = 1, size = 4, tableType = TableType.BAR)
        val foh = frontOfHouse(listOf(barTable))

        assertTrue(foh.reserveTables(regularGroup(id = 1, size = 4, tableType = TableType.BAR)))
        assertEquals(TableStatus.RESERVED, barTable.status)
    }

    // --- a merged table counts once ---

    @Test
    fun `a group seated on a merge of three tables counts as one table in the seating status`() {
        val tables = listOf(
            Table(id = 1, size = 2, tableType = TableType.COMMON),
            Table(id = 2, size = 2, tableType = TableType.COMMON),
            Table(id = 3, size = 2, tableType = TableType.COMMON)
        )
        val restaurant = restaurant(tables, waiters = listOf(Waiter()))
        restaurant.addToCustomerQueue(casualGroup(id = 1, size = 6))

        restaurant.simulateTick()

        val status = lines().single { it.contains("FOH Seating Status") }
        assertTrue(status.contains("on 1 tables."), "merged tables must count once, got $status")
    }

    @Test
    fun `two separately seated groups still count as two tables`() {
        val tables = listOf(
            Table(id = 1, size = 2, tableType = TableType.COMMON),
            Table(id = 2, size = 2, tableType = TableType.COMMON)
        )
        // both groups have to find something to order: a group whose customers cannot order all
        // leave again in the same step (item 104) and hand their table straight back
        val restaurant = restaurant(
            tables,
            waiters = listOf(Waiter(), Waiter()),
            menu = listOf(eventDish),
            cooks = listOf(Cook(CookType.EXEC))
        )
        restaurant.addToCustomerQueue(casualGroup(id = 1, size = 2))
        restaurant.addToCustomerQueue(casualGroup(id = 2, size = 2))

        restaurant.simulateTick()

        val status = lines().single { it.contains("FOH Seating Status") }
        assertTrue(status.contains("on 2 tables."), "expected two tables, got $status")
    }

    // --- arrivals are processed by type and then ascending id -------------------------

    @Test
    fun `a casual group left over from an earlier tick does not overtake a lower id newcomer`() {
        // only one table, so the first group processed wins it
        val restaurant = restaurant(
            listOf(Table(id = 1, size = 2, tableType = TableType.COMMON)),
            waiters = listOf(Waiter())
        )
        // group 5 arrives first but cannot be seated yet: there is no waiter free for it
        val leftOver = casualGroup(id = 5, size = 2, visitingAt = 5)
        restaurant.addToCustomerQueue(leftOver)
        restaurant.addToCustomerQueue(casualGroup(id = 3, size = 2, visitingAt = 6))

        Time.tick = 6
        restaurant.simulateTick()

        val arrivals = lines().filter { it.contains("Restaurant Arrival") }
        val seatings = lines().filter { it.contains("FOH Seating (") }
        assertTrue(arrivals.isNotEmpty(), "expected an arrival log, got ${lines()}")
        assertTrue(
            seatings.first().contains("Group 3 "),
            "the lower id must be served first, got $seatings"
        )
    }
}
