package escortingtests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * integration tests for F21 (FOH - Escorting) for event escorting through FOH
 * assigned waiter escorting part done by Deniz
 */
class EscortingEventWaitersIntegrationTest {
    private lateinit var output: StringWriter

    private val water = Ingredient("water", MeasurementUnit.ML, 5, 1000)
    private val soup = Recipe(1, "soup", 10, listOf(CookType.TOURNANT), mutableMapOf(water to 1), null)
    private val menu = listOf(soup)

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
        Time.evening = 1
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun escortingLines(): List<String> = lines().filter { it.contains("FOH Escorting (") }

    /** waiter id and the number of customers they escorted, in the order the logs appeared */
    private fun escortsInOrder(): List<Pair<String, Int>> = escortingLines().map { line ->
        line.substringAfter("Waitstaff ").substringBefore(" escorts") to
            line.substringAfter("escorts ").substringBefore(" customers").toInt()
    }

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse {
        val pantry = Pantry(
            inventory = mutableListOf(IngredientPackage(water)),
            supplier = Supplier(Stock(listOf(water)))
        )
        val countertop = Countertop(
            pantry,
            ArrayDeque<Order>(),
            listOf(Cook(CookType.TOURNANT)),
            RestaurantType.EUROPEAN
        )
        return FrontOfHouse(tables, waiters, emptyList(), countertop)
    }

    private fun waiter(id: Int, currentLoad: Int = 0) = Waiter().apply {
        this.id = id
        addToCurrentLoad(currentLoad)
    }

    private fun event(id: Int, size: Int) = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = 1,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "soup"),
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    private fun seatAndFeed(foh: FrontOfHouse, group: EventGroup) {
        assertTrue(foh.reserveTables(group), "the event group needs a reserved table")
        foh.processArrival(group, menu)
        val order = checkNotNull(group.currentOrder) { "the event group should have ordered, got ${lines()}" }
        order.dishes.forEach { it.status = DishStatus.EATEN }
        foh.clearActionLoads()
    }

    /**
     * the manager escorts an event group with whoever is free
     * starting with the waiter that has the fewest customers of their own
     */
    @Test
    fun `the waiter with the lowest current load escorts the event group first`() {
        val size = Constants.ACTION_LIMIT + 5
        val foh = frontOfHouse(
            listOf(Table(1, size, TableType.COMMON)),
            listOf(waiter(1, currentLoad = 5), waiter(2, currentLoad = 0), waiter(3, currentLoad = 9))
        )
        val group = event(1, size)
        seatAndFeed(foh, group)

        foh.processEscorting()

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(listOf("2" to Constants.ACTION_LIMIT, "1" to 5), escortsInOrder())
    }

    /** equal current loads fall back on the lowest waiter id */
    @Test
    fun `waiters with the same current load are recruited by ascending id`() {
        val size = Constants.ACTION_LIMIT + 3
        val foh = frontOfHouse(
            listOf(Table(1, size, TableType.COMMON)),
            listOf(waiter(2), waiter(1))
        )
        val group = event(1, size)
        seatAndFeed(foh, group)

        foh.processEscorting()

        assertEquals(listOf("1" to Constants.ACTION_LIMIT, "2" to 3), escortsInOrder())
    }

    /** escorting an event group does not change the load the waiters carry for their own tables */
    @Test
    fun `escorting an event group leaves the waiters' current loads untouched`() {
        val waiters = listOf(waiter(1, currentLoad = 4), waiter(2, currentLoad = 7))
        val foh = frontOfHouse(listOf(Table(1, 6, TableType.COMMON)), waiters)
        val group = event(1, 6)
        seatAndFeed(foh, group)

        foh.processEscorting()

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(listOf(4, 7), waiters.map { it.currentLoad })
    }

    /** the event table is reserved for the whole evening and is not handed to anybody else */
    @Test
    fun `an event group's reserved table is not released when they leave`() {
        val table = Table(1, 4, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(waiter(1)))
        val group = event(1, 4)
        seatAndFeed(foh, group)

        foh.processEscorting()

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(TableStatus.RESERVED, table.status)
        assertEquals(0, foh.getAvailableSeats()[TableType.COMMON])
    }

    /**
     * when the recruited waiters have already spent most of their escort actions elsewhere this
     * tick, the event group only leaves partially and the rest follows in the next tick
     */
    @Test
    fun `an event group leaves over two ticks when the waiters are nearly out of actions`() {
        val size = Constants.ACTION_LIMIT + 4
        val waiters = listOf(waiter(1), waiter(2))
        val foh = frontOfHouse(listOf(Table(1, size, TableType.COMMON)), waiters)
        val group = event(1, size)
        seatAndFeed(foh, group)
        // both waiters already escorted 5 of their own customers this tick
        waiters.forEach { it.addToTickLoad(ActionType.ESCORT, 5) }

        foh.processEscorting()
        assertEquals(4, group.customersRemainingInRestaurant)

        foh.clearActionLoads()
        foh.processEscorting()

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(listOf("1" to 5, "2" to 5, "1" to 4), escortsInOrder())
    }

    /** once the group has gone there is nothing left to escort, so no second log appears */
    @Test
    fun `an event group that already left is not escorted again`() {
        val foh = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val group = event(1, 4)
        seatAndFeed(foh, group)
        foh.processEscorting()
        foh.processEscorting()

        val secondTickLines = escortingLines()

        assertEquals(1, secondTickLines.size, "the group left in the first tick and is not escorted twice")
    }

    /** an event group that is still eating is left at its table */
    @Test
    fun `an event group that has not finished eating is not escorted`() {
        val foh = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val group = event(1, 4)
        assertTrue(foh.reserveTables(group))
        foh.processArrival(group, menu)

        foh.processEscorting()

        assertEquals(4, group.customersRemainingInRestaurant)
        assertTrue(escortingLines().isEmpty())
    }

    /** at closing the event guests are put outside too, without any waiter action */
    @Test
    fun `closing time empties an event table without an escorting action`() {
        val foh = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val group = event(1, 4)
        assertTrue(foh.reserveTables(group))
        foh.processArrival(group, menu)

        foh.startFohClosing()

        assertEquals(0, group.customersRemainingInRestaurant)
        assertTrue(escortingLines().isEmpty())
    }

    /** a waiter that is at the escort limit is not recruited */
    @Test
    fun `a waiter already at the escort limit is skipped by the recruitment`() {
        val busy = waiter(1).apply { addToTickLoad(ActionType.ESCORT, Constants.ACTION_LIMIT) }
        val free = waiter(2)
        val foh = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(busy, free))
        val group = event(1, 4)
        assertTrue(foh.reserveTables(group))
        foh.processArrival(group, menu)
        val order = checkNotNull(group.currentOrder)
        order.dishes.forEach { it.status = DishStatus.EATEN }

        foh.processEscorting()

        assertEquals(listOf("2" to 4), escortsInOrder())
    }
}
