package eventactiontests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
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
 * half of P03 tests, covered integration tests for escorting and serving selection for event groups
 */
class EventEscortingAndServingIntegrationTest {
    private lateinit var output: StringWriter

    private val broth = Ingredient("broth", MeasurementUnit.ML, 1000, 1000)
    private val orderableSoup = Recipe(1, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(broth to 1), null)
    private val menu = listOf(orderableSoup)

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
        Time.evening = 1
        Time.ticksElapsed = 0
        Order.resetIds()
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.ticksElapsed = 0
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun prefs(size: Int) = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) }

    private fun countertop(stockAmount: Int = 1000): Countertop {
        val pantry = Pantry(
            mutableListOf(IngredientPackage(broth, stockAmount, Time.evening + 1000, false)),
            Supplier(Stock(listOf(broth)))
        )
        return Countertop(pantry, ArrayDeque(), listOf(Cook(CookType.TOURNANT)), RestaurantType.EUROPEAN)
    }

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>, stockAmount: Int = 1000): FrontOfHouse =
        FrontOfHouse(tables = tables, waiters = waiters, drivers = emptyList(), countertop = countertop(stockAmount))

    private fun eventGroup(id: Int, size: Int): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = prefs(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = Time.evening,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "Soup"),
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    private fun regularGroup(id: Int, size: Int): RegularGroup = RegularGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = prefs(size),
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1,
    )

    private fun casualGroup(id: Int, size: Int): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = prefs(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(Time.evening),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.ALWAYS,
    )

    /**
     * 1. a 25-customer event group escorted over three ticks by three waiters
     *
     * between calls, only one waiter's ESCORT tick load is left at zero - standing in for that
     * waiter having done nothing else yet this tick, other two are set as fully busy
     * to isolates the event-escort recruitment and distribution logic across ticks without needing
     * three other groups to consume the other waiters' capacity
     */
    @Test
    fun `a 25-customer event is escorted over three ticks by three different waiters`() {
        val w1 = Waiter().also { it.id = 1 }
        val w2 = Waiter().also { it.id = 2 }
        val w3 = Waiter().also { it.id = 3 }
        val table = Table(1, 25, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(w1, w2, w3))
        val event = eventGroup(1, 25)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        requireNotNull(event.currentOrder).dishes.forEach { it.status = DishStatus.EATEN }

        // tick 1: only w1 is free
        w2.tickLoads[ActionType.ESCORT] = Constants.ACTION_LIMIT
        w3.tickLoads[ActionType.ESCORT] = Constants.ACTION_LIMIT
        foh.processEscorting()
        assertEquals(15, event.customersRemainingInRestaurant)
        assertTrue(lines().any { it.contains("Waitstaff 1 escorts 10 customers of group 1") })

        // tick 2: only w2 is free
        w1.tickLoads[ActionType.ESCORT] = Constants.ACTION_LIMIT
        w2.tickLoads[ActionType.ESCORT] = 0
        foh.processEscorting()
        assertEquals(5, event.customersRemainingInRestaurant)
        assertTrue(lines().any { it.contains("Waitstaff 2 escorts 10 customers of group 1") })

        // tick 3: only w3 is free
        w2.tickLoads[ActionType.ESCORT] = Constants.ACTION_LIMIT
        w3.tickLoads[ActionType.ESCORT] = 0
        foh.processEscorting()
        assertEquals(0, event.customersRemainingInRestaurant)
        assertTrue(lines().any { it.contains("Waitstaff 3 escorts 5 customers of group 1") })
    }

    /**
     * 2. group order in one tick: REGULAR, EVENT, CASUAL share escort capacity
     *
     * the three groups are seated on separate ticks (cleared in between)
     * so that only the escorting step, is tested
     * all three then need escorting from the same one waiter in a tick
     */
    @Test
    fun `regular, event and casual groups share one waiter's escort capacity in priority order`() {
        val waiter = Waiter()
        val regTable = Table(1, 4, TableType.COMMON)
        val eventTable = Table(2, 4, TableType.COMMON)
        val casualTable = Table(3, 4, TableType.COMMON)
        val foh = frontOfHouse(listOf(regTable, eventTable, casualTable), listOf(waiter))

        val regular = regularGroup(1, 4)
        assertTrue(foh.reserveTables(regular))
        assertTrue(foh.processArrival(regular, menu))
        foh.clearActionLoads()

        val event = eventGroup(2, 4)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        foh.clearActionLoads()

        val casual = casualGroup(3, 4)
        assertTrue(foh.processArrival(casual, menu))
        foh.clearActionLoads()

        listOf(regular.currentOrder, event.currentOrder, casual.currentOrder).forEach { order ->
            requireNotNull(order).dishes.forEach { it.status = DishStatus.EATEN }
        }

        foh.processEscorting()

        val escortedGroupIds = lines().filter { it.contains("FOH Escorting (") }
            .map { it.substringAfter("of group ").substringBefore(" from").toInt() }
        assertEquals(listOf(1, 2, 3), escortedGroupIds, "REGULAR, then EVENT, then CASUAL")
        assertEquals(0, regular.customersRemainingInRestaurant)
        assertEquals(0, event.customersRemainingInRestaurant)
        assertEquals(2, casual.customersRemainingInRestaurant, "only 2 of the waiter's capacity remained for casual")
        assertEquals(Constants.ACTION_LIMIT, waiter.getTickLoad(ActionType.ESCORT))
    }

    /** 3. the event's table stays reserved after escorting (only CASUAL tables are dismantled) */
    @Test
    fun `the event's table stays reserved after the group has been fully escorted`() {
        val waiter = Waiter()
        val table = Table(1, 4, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(waiter))
        val event = eventGroup(1, 4)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        requireNotNull(event.currentOrder).dishes.forEach { it.status = DishStatus.EATEN }

        foh.processEscorting()

        assertEquals(0, event.customersRemainingInRestaurant)
        assertEquals(TableStatus.RESERVED, table.status, "an event's table is reserved for the whole evening")
    }

    /**
     * 4. an event that lost customers at ordering is escorted with the smaller remaining customers number
     *
     * only 3 units of the single ingredient are in stock, so only 3 of the 5 event customers
     * can actually order escorting must then move exactly those 3
     */
    @Test
    fun `an event that lost customers while ordering is escorted with the reduced headcount`() {
        val waiter = Waiter()
        val table = Table(1, 5, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(waiter), stockAmount = 3)
        val event = eventGroup(1, 5)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))

        assertEquals(3, event.customersRemainingInRestaurant, "only 3 units of the ingredient were available")
        assertEquals(ExperienceType.NEGATIVE, event.experience)
        val order = requireNotNull(event.currentOrder)
        assertEquals(3, order.dishes.size)
        order.dishes.forEach { it.status = DishStatus.EATEN }

        foh.processEscorting()

        val waiterId = requireNotNull(waiter.id)
        assertTrue(lines().any { it.contains("Waitstaff $waiterId escorts 3 customers of group 1") })
        assertEquals(0, event.customersRemainingInRestaurant)
    }

    /**
     * 5a. FOH no serving names the first waiter that would have served
     * when the order is still incomplete and inside its waiting window
     */
    @Test
    fun `FOH No Serving names the first waiter that would have served an incomplete order`() {
        val waiter = Waiter()
        val table = Table(1, 2, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(waiter))
        val event = eventGroup(1, 2)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        val order = requireNotNull(event.currentOrder)
        order.dishes[0].status = DishStatus.COOKED
        order.firstDishCookedAt = Time.tick
        // order.dishes[1] stays UNCOOKED: the order is incomplete and still within its wait window

        foh.clearActionLoads()
        foh.processServing()

        val waiterId = requireNotNull(waiter.id)
        assertTrue(
            lines().any { it.contains("FOH No Serving (R 1): Waitstaff $waiterId did not serve 1 meals to table 1.") }
        )
        assertEquals(DishStatus.COOKED, order.dishes[0].status, "nothing was actually served yet")
    }

    /**
     * 5b. FOH no serving prints the last waiter that served
     * once serving starts and recruited waiters' combined capacity still falls short of the complete order
     */
    @Test
    fun `FOH No Serving names the last waiter that served a partially-servable complete order`() {
        val w1 = Waiter().also {
            it.id = 1
            it.addToTickLoad(ActionType.SERVE, Constants.ACTION_LIMIT - 2)
        }
        val w2 = Waiter().also {
            it.id = 2
            it.addToTickLoad(ActionType.SERVE, Constants.ACTION_LIMIT - 1)
        }
        val table = Table(1, 5, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(w1, w2))
        val event = eventGroup(1, 5)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        val order = requireNotNull(event.currentOrder)
        order.dishes.forEach { it.status = DishStatus.COOKED }
        order.firstDishCookedAt = Time.tick
        order.startServing() // serving already began on an earlier tick

        foh.processServing()

        assertEquals(3, order.dishes.count { it.status == DishStatus.SERVED }, "w1's 2 + w2's 1 remaining capacity")
        assertTrue(
            lines().any { it.contains("FOH No Serving (R 1): Waitstaff 2 did not serve 2 meals to table 1.") },
            "the last (lowest-priority) recruited waiter is named for the unserved remainder"
        )
    }

    /** 6: an event whose kitchen hasn't finished a single dish yet is skipped, with no logs */
    @Test
    fun `an event with nothing cooked yet is not served and produces no serving log`() {
        val waiter = Waiter()
        val table = Table(1, 2, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(waiter))
        val event = eventGroup(1, 2)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        // every dish stays UNCOOKED: order.getServableDishes() is empty

        foh.clearActionLoads()
        foh.processServing()

        assertTrue(lines().none { it.contains("FOH Serving (") || it.contains("FOH No Serving (") })
    }

    /**
     * 7: (after 5a part)
     * once the partial-serving wait window is gone, incomplete event order stops waiting, starts serving partially
     */
    @Test
    fun `once the wait window expires an incomplete event order starts being served one by one`() {
        val waiter = Waiter()
        val table = Table(1, 2, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(waiter))
        val event = eventGroup(1, 2)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        val order = requireNotNull(event.currentOrder)
        order.dishes[0].status = DishStatus.COOKED
        order.firstDishCookedAt = Time.tick - (Constants.PARTIAL_SERVING_WAIT_TICKS + 1)
        // order.dishes[1] stays UNCOOKED, but the window for waiting on it is now over

        foh.clearActionLoads()
        foh.processServing()

        assertEquals(DishStatus.SERVED, order.dishes[0].status, "the one cooked dish is served without waiting further")
        val waiterId = requireNotNull(waiter.id)
        assertTrue(lines().any { it.contains("FOH Serving (R 1): Waitstaff $waiterId serves") })
    }
}
