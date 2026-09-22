package simulationstatisticsdeliverytests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import eventseatingoutcometests.EventSeatingFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import simulationstatisticsdeliverytests.StatisticsFixtures.deliveryGroup
import simulationstatisticsdeliverytests.StatisticsFixtures.order
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val OPENING_TICK_END = 24
private const val LARGE_ORDER = 12
private const val EVENT_ORDER = 15
private const val DINE_IN_GROUP_SIZE = 2

/**
 * F07: tests for the served-customers for delivered customers and ratings.
 */
class StatisticsServedTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    @AfterEach
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    private fun log(): String = output.toString()

    private fun servingProcessor(
        waiters: List<Waiter>,
        inHouseGroups: List<CustomerGroup> = emptyList(),
        waiterFor: (CustomerGroup) -> Waiter? = { null },
        getAssignedTableId: (CustomerGroup) -> Id? = { 1 },
        recruitForEvent: (ActionType, EventGroup) -> List<Waiter> = { _, _ -> emptyList() }
    ): ServingProcessor {
        var nextWaiterId = 100
        return ServingProcessor(
            waiters = waiters,
            drivers = emptyList(),
            deliveryGroups = emptyList(),
            getInHouseGroups = { inHouseGroups },
            waiterFor = waiterFor,
            getServingPriority = { 0 },
            getAssignedTableId = getAssignedTableId,
            recruitWaitersForEventGroup = recruitForEvent,
            getNextWaiterId = { nextWaiterId++ }
        )
    }

    @Test
    fun `serving a dine-in group's cooked dishes adds the whole batch to the served counter`() {
        val group = deliveryGroup(id = 1, size = DINE_IN_GROUP_SIZE)
        group.currentOrder = order(DishStatus.COOKED, dishes = DINE_IN_GROUP_SIZE)
        val waiter = Waiter()
        val processor = servingProcessor(listOf(waiter), listOf(group), waiterFor = { waiter })

        processor.processServing()

        assertEquals(DINE_IN_GROUP_SIZE, processor.numberOfCustomersServed)
        assertTrue(requireNotNull(group.currentOrder).dishes.all { it.status == DishStatus.SERVED })
    }

    @Test
    fun `once serving has started, only the dishes that fit the waiter's capacity this tick are counted`() {
        val group = deliveryGroup(id = 1, size = LARGE_ORDER)
        val theOrder = order(DishStatus.COOKED, dishes = LARGE_ORDER).apply { startServing() }
        group.currentOrder = theOrder
        val waiter = Waiter()
        val processor = servingProcessor(listOf(waiter), listOf(group), waiterFor = { waiter })

        processor.processServing()

        assertEquals(
            Constants.ACTION_LIMIT,
            processor.numberOfCustomersServed,
            "only a fresh waiter's ${Constants.ACTION_LIMIT}-dish capacity fits, the rest waits"
        )
        assertEquals(Constants.ACTION_LIMIT, theOrder.dishes.count { it.status == DishStatus.SERVED })
        assertTrue(log().contains("FOH No Serving"), log())
    }

    @Test
    fun `a complete order too big to start with one waiter is not served yet, so the counter stays at zero`() {
        val group = deliveryGroup(id = 1, size = LARGE_ORDER)
        group.currentOrder = order(DishStatus.COOKED, dishes = LARGE_ORDER)
        val waiter = Waiter()
        val processor = servingProcessor(listOf(waiter), listOf(group), waiterFor = { waiter })

        processor.processServing()

        assertEquals(
            0,
            processor.numberOfCustomersServed,
            "a not-yet-started complete order needs one waiter able to take all of it at once"
        )
        assertTrue(requireNotNull(group.currentOrder).dishes.all { it.status == DishStatus.COOKED })
    }

    @Test
    fun `an order with nothing cooked yet does not touch the served counter`() {
        val group = deliveryGroup(id = 1, size = DINE_IN_GROUP_SIZE)
        group.currentOrder = order(DishStatus.UNCOOKED, dishes = DINE_IN_GROUP_SIZE)
        val waiter = Waiter()
        val processor = servingProcessor(listOf(waiter), listOf(group), waiterFor = { waiter })

        processor.processServing()

        assertEquals(0, processor.numberOfCustomersServed)
    }

    @Test
    fun `a group with no assigned waiter this tick is skipped and the counter is untouched`() {
        val group = deliveryGroup(id = 1, size = DINE_IN_GROUP_SIZE)
        group.currentOrder = order(DishStatus.COOKED, dishes = DINE_IN_GROUP_SIZE)
        val processor = servingProcessor(listOf(Waiter()), listOf(group), waiterFor = { null })

        processor.processServing()

        assertEquals(0, processor.numberOfCustomersServed)
    }

    @Test
    fun `a seated group whose table cannot be identified this tick is skipped and the counter is untouched`() {
        val group = deliveryGroup(id = 1, size = DINE_IN_GROUP_SIZE)
        group.currentOrder = order(DishStatus.COOKED, dishes = DINE_IN_GROUP_SIZE)
        val waiter = Waiter()
        val processor =
            servingProcessor(listOf(waiter), listOf(group), waiterFor = { waiter }, getAssignedTableId = { null })

        processor.processServing()

        assertEquals(0, processor.numberOfCustomersServed)
    }

    @Test
    fun `serving an event group recruits several waiters and the counter sums every waiter's batch`() {
        val event = EventSeatingFixtures.eventGroup(id = 1, size = EVENT_ORDER)
        event.currentOrder = order(DishStatus.COOKED, dishes = EVENT_ORDER)
        val w1 = Waiter().apply { id = 1 }
        val w2 = Waiter().apply { id = 2 }
        val processor = servingProcessor(
            waiters = listOf(w1, w2),
            inHouseGroups = listOf(event),
            recruitForEvent = { _, _ -> listOf(w1, w2) }
        )

        processor.processServing()

        assertEquals(
            EVENT_ORDER,
            processor.numberOfCustomersServed,
            "w1's ${Constants.ACTION_LIMIT} + w2's ${EVENT_ORDER - Constants.ACTION_LIMIT}"
        )
        assertTrue(requireNotNull(event.currentOrder).dishes.all { it.status == DishStatus.SERVED })
    }

    @Test
    fun `an event order whose recruited waiters can't cover it yet is not served, so the counter stays at zero`() {
        val event = EventSeatingFixtures.eventGroup(id = 1, size = LARGE_ORDER)
        event.currentOrder = order(DishStatus.COOKED, dishes = LARGE_ORDER)
        val waiter = Waiter().apply { id = 1 }
        val processor = servingProcessor(
            waiters = listOf(waiter),
            inHouseGroups = listOf(event),
            recruitForEvent = { _, _ -> listOf(waiter) }
        )

        processor.processServing()

        assertEquals(0, processor.numberOfCustomersServed)
    }

    // ---- real FrontOfHouse integration tests (no mocks) ----

    @Test
    fun `serving a real regular group through FrontOfHouse increases numberOfCustomersServed by its dish count`() {
        val table = EventSeatingFixtures.table(id = 1, size = DINE_IN_GROUP_SIZE)
        val waiter = EventSeatingFixtures.waiter()
        val foh = EventSeatingFixtures.frontOfHouse(listOf(table), listOf(waiter))
        val regular = EventSeatingFixtures.regularGroup(id = 1, size = DINE_IN_GROUP_SIZE)

        assertTrue(foh.reserveTables(regular))
        assertTrue(foh.processArrival(regular, EventSeatingFixtures.menu))
        requireNotNull(regular.currentOrder).dishes.forEach { it.status = DishStatus.COOKED }

        foh.clearActionLoads()
        foh.processServing()

        assertEquals(DINE_IN_GROUP_SIZE, foh.numberOfCustomersServed)
    }

    @Test
    fun `serving a real event group across two real waiters sums both waiters' batches into the counter`() {
        val table = EventSeatingFixtures.table(id = 1, size = EVENT_ORDER)
        val w1 = EventSeatingFixtures.waiter(id = 1)
        val w2 = EventSeatingFixtures.waiter(id = 2)
        val foh = EventSeatingFixtures.frontOfHouse(listOf(table), listOf(w1, w2))
        val event = EventSeatingFixtures.eventGroup(id = 1, size = EVENT_ORDER)

        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, EventSeatingFixtures.menu))
        requireNotNull(event.currentOrder).dishes.forEach { it.status = DishStatus.COOKED }

        foh.clearActionLoads()
        foh.processServing()

        assertEquals(EVENT_ORDER, foh.numberOfCustomersServed)
    }

    // ---- real Restaurant delegation, through to the final Simulation log ----

    /**
     * A recipe with no ingredients, so order placement never depends on
     * `Restaurant.prepareForEvening`'s free-seat ingredient-stocking estimate (a rough heuristic that
     * under-provisions for small groups - see the kitchen-planning note in project memory). What is
     * under test here is the served-counter delegation, not ordering or pantry stocking.
     */
    private val zeroIngredientRecipe = Recipe(
        id = 1,
        name = "Simple",
        duration = 1,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(),
        basicDishFor = null
    )

    private fun realRestaurant(table: Table, waiter: Waiter): Restaurant {
        val stats = RestaurantStats(
            restaurantId = 1,
            restaurantType = RestaurantType.EUROPEAN,
            openingTickStart = 1,
            openingTickEnd = OPENING_TICK_END,
            event = false,
            positiveRatings = 0,
            negativeRatings = 0,
            menu = listOf(zeroIngredientRecipe)
        )
        val busyCook = Cook(CookType.TOURNANT).apply { isCooking = true }
        val staff = RestaurantStaff(mutableListOf(busyCook), mutableListOf(waiter), mutableListOf())
        return Restaurant(stats, "Test Kitchen", staff, listOf(table), Stock(emptyList()))
    }

    private fun realServedRestaurant(): Restaurant {
        val table = Table(id = 1, size = DINE_IN_GROUP_SIZE, tableType = TableType.COMMON)
        val waiter = Waiter()
        val restaurant = realRestaurant(table, waiter)
        val regular = EventSeatingFixtures.regularGroup(id = 1, size = DINE_IN_GROUP_SIZE)

        restaurant.prepareForEvening(listOf(regular))
        Time.tick = 1
        restaurant.simulateTick()
        val realOrder = requireNotNull(regular.currentOrder) { "the zero-ingredient recipe is always orderable" }
        realOrder.dishes.forEach { it.status = DishStatus.COOKED }

        Time.tick = 2
        restaurant.simulateTick()
        return restaurant
    }

    @Test
    fun `Restaurant getNumberOfCustomersServed reflects dishes really served through the real FrontOfHouse`() {
        val restaurant = realServedRestaurant()

        assertEquals(DINE_IN_GROUP_SIZE, restaurant.getNumberOfCustomersServed())
    }

    @Test
    fun `a real, unmocked restaurant's served count feeds the final simulation statistics log`() {
        val restaurant = realServedRestaurant()

        Simulation(SimulationConfig()).apply { restaurants = listOf(restaurant) }.runSimulation()

        assertTrue(
            log().contains("Simulation Statistics: Restaurant 1 served $DINE_IN_GROUP_SIZE customers."),
            log()
        )
    }
}
