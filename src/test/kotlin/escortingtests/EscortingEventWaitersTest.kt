package escortingtests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.EscortingProcessor
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * unit tests for F21 (FOH - Escorting) for event escorting (with recruited waiters)
 * assigned waiter escorting tested by Deniz
 */
class EscortingEventWaitersTest {
    private lateinit var output: StringWriter

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

    private val water = Ingredient("water", MeasurementUnit.ML, 10, 1)
    private val soup = Recipe(1, "soup", 1, emptyList(), mutableMapOf(water to 1), null)

    private fun order(dishes: Int, status: DishStatus = DishStatus.EATEN): Order =
        Order(List(dishes) { Dish(soup, false, 0, status) })

    private fun event(id: Int, size: Int, order: Order? = order(size)) = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = 1,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "soup"),
    ).also { it.currentOrder = order }

    private fun processor(
        group: EventGroup,
        tables: MutableMap<CustomerGroup, List<Table>>,
        eventWaiters: List<Waiter>,
    ): EscortingProcessor {
        var nextId: Id = 1
        return EscortingProcessor(
            customerToTable = tables,
            eventGroups = mutableListOf(group),
            inHouseGroupsToWaiter = emptyMap(),
            getInHouseGroups = { emptyList() },
            getServingPriority = { 1 },
            recruitWaitersForEventGroup = { _, _ -> eventWaiters },
            getNextWaiterId = { nextId++ },
        )
    }

    /** the event manager only acts once every meal of the event table has been eaten */
    @Test
    fun `an event group that is still eating is not escorted`() {
        val group = event(1, 3, order(3, DishStatus.SERVED))
        val tables = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(Table(1, 3, TableType.COMMON)))

        processor(group, tables, listOf(Waiter())).processEscorting()

        assertEquals(3, group.customersRemainingInRestaurant)
        assertTrue(escortingLines().isEmpty())
    }

    /** an event group that never placed an order has nothing to finish, so it is skipped */
    @Test
    fun `an event group that never placed an order is skipped`() {
        val group = event(1, 3, order = null)
        val tables = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(Table(1, 3, TableType.COMMON)))

        processor(group, tables, listOf(Waiter())).processEscorting()

        assertEquals(3, group.customersRemainingInRestaurant)
        assertTrue(escortingLines().isEmpty())
    }

    /** with nobody recruited, the event group stays seated and no escorting is logged */
    @Test
    fun `an event group without a recruited waiter is not escorted`() {
        val group = event(1, 4)
        val tables = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(Table(1, 4, TableType.COMMON)))

        processor(group, tables, emptyList()).processEscorting()

        assertEquals(4, group.customersRemainingInRestaurant)
        assertTrue(escortingLines().isEmpty())
        assertEquals(
            "[DEBUG] FOH Escorting Status (R 1): 0 waitstaff escorted 0 customers this tick.",
            lines().last { it.contains("FOH Escorting Status") }
        )
    }

    /** the event table is reserved for the whole evening, so it is never dismantled */
    @Test
    fun `an event group keeps its reserved table after everybody has left`() {
        val group = event(1, 4)
        val table = Table(6, 4, TableType.COMMON).also { it.status = TableStatus.RESERVED }
        val tables = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(table))

        processor(group, tables, listOf(Waiter())).processEscorting()

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(TableStatus.RESERVED, table.status, "an event table is not freed for other groups")
        assertEquals(1, tables.size)
    }

    /** more customers than one waiter can take: the recruited waiters split the group between them */
    @Test
    fun `recruited waiters split a group that exceeds one action limit`() {
        val size = Constants.ACTION_LIMIT + 3
        val group = event(2, size)
        val waiters = listOf(Waiter(), Waiter())
        val tables = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(Table(1, size, TableType.COMMON)))

        processor(group, tables, waiters).processEscorting()

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(
            listOf(Constants.ACTION_LIMIT, 3),
            escortingLines().map { it.substringAfter("escorts ").substringBefore(" customers").toInt() }
        )
        assertEquals(
            "[DEBUG] FOH Escorting Status (R 1): 2 waitstaff escorted $size customers this tick.",
            lines().last { it.contains("FOH Escorting Status") }
        )
    }

    /** not enough capacity in a tick: the rest of the event group leaves in the next one */
    @Test
    fun `an event group too big for one tick leaves over two ticks`() {
        val size = Constants.ACTION_LIMIT + 2
        val group = event(1, size)
        val waiter = Waiter()
        val tables = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(Table(1, size, TableType.COMMON)))
        val processor = processor(group, tables, listOf(waiter))

        processor.processEscorting()
        assertEquals(2, group.customersRemainingInRestaurant)

        waiter.resetActionLoads()
        processor.processEscorting()

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(
            listOf(Constants.ACTION_LIMIT, 2),
            escortingLines().map { it.substringAfter("escorts ").substringBefore(" customers").toInt() }
        )
    }

    /** EVENT customers never count towards a waiter's current load, so escorting them cannot reduce it */
    @Test
    fun `escorting an event group leaves the waiter's current load untouched`() {
        val waiter = Waiter().also { it.addToCurrentLoad(5) }
        val group = event(1, 3)

        waiter.escortEventGroups(group)

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(5, waiter.currentLoad, "the load belongs to the waiter's own seated groups")
        assertEquals(3, waiter.getTickLoad(ActionType.ESCORT))
    }

    /** a waiter who already used their ESCORT actions this tick takes nobody from the event */
    @Test
    fun `a waiter at the escort limit takes nobody from an event group`() {
        val waiter = Waiter().also { it.addToTickLoad(ActionType.ESCORT, Constants.ACTION_LIMIT) }
        val group = event(1, 4)

        waiter.escortEventGroups(group)

        assertEquals(4, group.customersRemainingInRestaurant)
        assertEquals(Constants.ACTION_LIMIT, waiter.getTickLoad(ActionType.ESCORT))
    }

    /** an event group that has already left costs no action, so the waiter stays free */
    @Test
    fun `an event group that already left is not escorted again`() {
        val waiter = Waiter()
        val group = event(1, 4).also { it.customersRemainingInRestaurant = 0 }

        waiter.escortEventGroups(group)

        assertEquals(0, waiter.getTickLoad(ActionType.ESCORT))
    }
}
