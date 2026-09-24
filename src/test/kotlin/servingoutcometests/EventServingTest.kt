package servingoutcometests
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Kitchen
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Serving of EVENT tables (F19) with several recruited waiters, and the serving logs.
 */
class EventServingTest {
    private lateinit var output: StringWriter

    private val broth = Ingredient("broth", MeasurementUnit.ML, 10, 1000)
    private val soup = Recipe(1, "Soup", 20, listOf(CookType.TOURNANT), mutableMapOf(broth to 1), null)
    private val menu = listOf(soup)

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
        Time.evening = 1
        Order.resetIds()
    }

    private fun lines() = output.toString().lines().filter { it.isNotBlank() }.map { it.substringAfter("] ") }

    private fun servingLines() = lines().filter { it.startsWith("FOH Serving (") || it.startsWith("FOH No Serving") }

    private fun waiter(id: Int, serveLoad: Int = 0) = Waiter().also {
        it.id = id
        it.addToTickLoad(ActionType.SERVE, serveLoad)
    }

    private fun prefs(size: Int) = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) }

    private fun eventGroup(id: Int, size: Int = 2) = EventGroup(
        id,
        size,
        TableType.COMMON,
        Time.tick,
        prefs(size),
        listOf(RestaurantType.EUROPEAN),
        Time.evening,
        mapOf(RestaurantType.EUROPEAN to "Soup"),
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    private fun eventWithOrder(vararg statuses: DishStatus, firstCookedAt: Int? = Time.tick) = eventGroup(1).also {
        it.currentOrder = Order(statuses.map { status -> Dish(soup, false, 2, status) })
        it.currentOrder?.firstDishCookedAt = firstCookedAt
    }

    private fun servingProcessor(
        groups: List<CustomerGroup>,
        recruitedWaiters: List<Waiter>,
        tableId: Int? = 1,
    ) = ServingProcessor(
        waiters = recruitedWaiters,
        drivers = emptyList(),
        deliveryGroups = emptyList(),
        getInHouseGroups = { groups },
        waiterFor = { null },
        getServingPriority = { 1 },
        getAssignedTableId = { tableId },
        recruitWaitersForEventGroup = { _, _ -> recruitedWaiters },
        getNextWaiterId = { 99 },
    )

    @Test
    fun `Incomplete Order Within Window Waits And Names First Candidate`() {
        val eventGroup = eventWithOrder(DishStatus.COOKED, DishStatus.UNCOOKED)
        val nothingCooked = eventGroup(2).also { it.currentOrder = Order(listOf(Dish(soup))) }
        val noOrder = eventGroup(3)

        servingProcessor(listOf(eventGroup, nothingCooked, noOrder), listOf(waiter(3), waiter(4))).processServing()

        assertEquals(listOf("FOH No Serving (R 1): Waitstaff 3 did not serve 1 meals to table 1."), servingLines())
        assertFalse(eventGroup.currentOrder!!.hasServingStarted())
        assertEquals(DishStatus.COOKED, eventGroup.currentOrder!!.dishes[0].status)

        servingProcessor(listOf(eventGroup), emptyList()).processServing()
        servingProcessor(listOf(eventGroup), listOf(waiter(3)), tableId = null).processServing()
        assertEquals(1, servingLines().size)
    }

    @Test
    fun `Complete Order With Too Little Pooled Capacity Serves Nothing Then Serves Next Tick`() {
        val eventGroup = eventWithOrder(DishStatus.COOKED, DishStatus.COOKED, DishStatus.COOKED, DishStatus.COOKED)
        val firstWaiter = waiter(3, serveLoad = 8)
        val secondWaiter = waiter(4, serveLoad = 9)
        val servingProcessor = servingProcessor(listOf(eventGroup), listOf(firstWaiter, secondWaiter))

        servingProcessor.processServing()

        assertEquals(listOf("FOH No Serving (R 1): Waitstaff 3 did not serve 4 meals to table 1."), servingLines())
        assertTrue(eventGroup.currentOrder!!.hasServingStarted())

        firstWaiter.resetActionLoads()
        secondWaiter.resetActionLoads()
        servingProcessor.processServing()

        assertTrue(eventGroup.currentOrder!!.areAllDishesServed())
        assertFalse(eventGroup.currentOrder!!.hasServingStarted())
    }

    @Test
    fun `Enough Pooled Capacity Spreads Dishes Over Several Waiters`() {
        val eventGroup = eventWithOrder(*Array(5) { DishStatus.COOKED })
        val unneededWaiter = waiter(5)

        servingProcessor(listOf(eventGroup), listOf(waiter(3, serveLoad = 7), waiter(4), unneededWaiter))
            .processServing()

        assertEquals(
            listOf(
                "FOH Serving (R 1): Waitstaff 3 serves Soup:3 to table 1 0 ticks after ordering.",
                "FOH Serving (R 1): Waitstaff 4 serves Soup:2 to table 1 0 ticks after ordering.",
            ),
            servingLines()
        )
        assertEquals(0, unneededWaiter.getTickLoad(ActionType.SERVE))
        assertEquals("FOH Serving Status (R 1): 2 waitstaff served 12 meals.", lines().last())
    }

    @Test
    fun `Started Or Overdue Tables Are Served Without Waiting For The Rest`() {
        val startedGroup = eventWithOrder(DishStatus.COOKED, DishStatus.COOKED, DishStatus.UNCOOKED)
        startedGroup.currentOrder!!.startServing()
        val overdueGroup = eventWithOrder(DishStatus.COOKED, DishStatus.UNCOOKED, firstCookedAt = Time.tick - 2)

        servingProcessor(listOf(startedGroup, overdueGroup), listOf(waiter(3, serveLoad = 7))).processServing()

        assertEquals(
            listOf(
                "FOH Serving (R 1): Waitstaff 3 serves Soup:2 to table 1 0 ticks after ordering.",
                "FOH Serving (R 1): Waitstaff 3 serves Soup:1 to table 1 0 ticks after ordering.",
            ),
            servingLines()
        )
        assertTrue(startedGroup.currentOrder!!.hasServingStarted())
        assertEquals(DishStatus.UNCOOKED, overdueGroup.currentOrder!!.dishes[1].status)
    }

    @Test
    fun `Shortfall Mid Serve Names The Last Recruited Waiter`() {
        val eventGroup = eventWithOrder(*Array(5) { DishStatus.COOKED })
        eventGroup.currentOrder!!.startServing()
        val recruitedWaiters = listOf(waiter(2, serveLoad = 10), waiter(3, serveLoad = 9), waiter(4, serveLoad = 8))

        servingProcessor(listOf(eventGroup), recruitedWaiters).processServing()

        assertEquals(
            listOf(
                "FOH Serving (R 1): Waitstaff 3 serves Soup:1 to table 1 0 ticks after ordering.",
                "FOH Serving (R 1): Waitstaff 4 serves Soup:2 to table 1 0 ticks after ordering.",
                "FOH No Serving (R 1): Waitstaff 4 did not serve 2 meals to table 1.",
            ),
            servingLines()
        )
    }

    private fun countertop(orderQueue: ArrayDeque<Order>) = Countertop(
        Pantry(mutableListOf(IngredientPackage(broth)), Supplier(Stock(listOf(broth)))),
        orderQueue,
        listOf(Cook(CookType.TOURNANT)),
        RestaurantType.EUROPEAN
    )

    @Test
    fun `Event Is Served After Regular But Before Casual`() {
        val sharedWaiter = Waiter()
        val tables = listOf(Table(1, 1, TableType.COMMON), Table(2, 2, TableType.COMMON), Table(3, 3, TableType.COMMON))
        val frontOfHouse = FrontOfHouse(tables, listOf(sharedWaiter), emptyList(), countertop(ArrayDeque()))
        val regularGroup = RegularGroup(9, 3, TableType.COMMON, Time.tick, prefs(3), 1, 1, 1)
        val eventGroup = eventGroup(5)
        val casualGroup = CasualGroup(
            1, 1, TableType.COMMON, Time.tick, prefs(1), listOf(RestaurantType.EUROPEAN), listOf(1), 0,
            RatingLikelihood.NEVER
        )
        assertTrue(frontOfHouse.reserveTables(regularGroup))
        assertTrue(frontOfHouse.reserveTables(eventGroup))
        listOf(regularGroup, eventGroup, casualGroup).forEach {
            frontOfHouse.processArrival(it, menu)
            it.currentOrder!!.dishes.forEach { dish -> dish.status = DishStatus.COOKED }
        }
        frontOfHouse.clearActionLoads()
        sharedWaiter.addToTickLoad(ActionType.SERVE, 5)

        frontOfHouse.processServing()

        assertEquals(
            listOf(
                "FOH Serving (R 1): Waitstaff 1 serves Soup:3 to table 3 0 ticks after ordering.",
                "FOH Serving (R 1): Waitstaff 1 serves Soup:2 to table 2 0 ticks after ordering.",
                "FOH No Serving (R 1): Waitstaff 1 did not serve 1 meals to table 1.",
            ),
            servingLines()
        )
    }

    @Test
    fun `Event Order Goes From Ordering Through Cooking To Serving`() {
        val orderQueue = ArrayDeque<Order>()
        val cook = Cook(CookType.TOURNANT)
        val table = Table(1, 2, TableType.COMMON)
        val frontOfHouse = FrontOfHouse(listOf(table), listOf(Waiter()), emptyList(), countertop(orderQueue))
        val kitchen = Kitchen(listOf(cook), Pantry(Stock(listOf(broth))), orderQueue, RestaurantType.EUROPEAN)
        val eventGroup = eventGroup(1)
        assertTrue(frontOfHouse.reserveTables(eventGroup))
        assertTrue(frontOfHouse.processArrival(eventGroup, menu))

        kitchen.processCooking()
        frontOfHouse.processServing()
        assertTrue(servingLines().isEmpty())

        Time.tick = 6
        frontOfHouse.clearActionLoads()
        kitchen.processCooking()
        frontOfHouse.processServing()

        assertEquals(
            listOf("FOH Serving (R 1): Waitstaff 1 serves Soup:2 to table 1 1 ticks after ordering."),
            servingLines()
        )
        assertTrue(eventGroup.currentOrder!!.areAllDishesServed())
        assertEquals(2, frontOfHouse.numberOfCustomersServed)
    }
}
