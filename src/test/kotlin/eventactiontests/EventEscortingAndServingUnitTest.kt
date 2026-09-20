package eventactiontests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
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
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.EscortingProcessor
import de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P03 (FOH - Event Other Actions)
 * tested escorting and serving waiter recruitment for EventGroups
 * ordering part tested by Vlad
 *
 * tests 1-7 construct EscortingProcessor with a stub recruit, they check distribution/logs given a decided waiter list
 * tests 2-3, 8-9 (serving) go through FOH, because the "lowest current load"/"lowest id" recruitment order is private
 */
class EventEscortingAndServingUnitTest {
    private lateinit var output: StringWriter

    private val water = Ingredient("water", MeasurementUnit.ML, 10, 1)
    private val soupRecipe = Recipe(1, "soup", 1, emptyList(), mutableMapOf(water to 1), null)

    private val broth = Ingredient("broth", MeasurementUnit.ML, 1000, 1000)
    private val orderableSoup = Recipe(2, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(broth to 1), null)
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

    // HELPERS

    private fun orderOfStatuses(statuses: List<DishStatus>): Order =
        Order(statuses.map { Dish(soupRecipe, false, 0, it) })

    private fun eatenOrder(dishes: Int): Order = orderOfStatuses(List(dishes) { DishStatus.EATEN })

    private fun casual(id: Int, size: Int) = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = prefs(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(Time.evening),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.ALWAYS,
    ).also { it.currentOrder = eatenOrder(size) }

    private fun event(id: Int, size: Int) = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = prefs(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = Time.evening,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "soup"),
    )

    private fun directProcessor(
        inHouse: Map<CustomerGroup, Waiter> = emptyMap(),
        eventGroups: MutableList<EventGroup> = mutableListOf(),
        tables: MutableMap<CustomerGroup, List<Table>> = mutableMapOf(),
        eventWaiters: List<Waiter> = emptyList(),
    ): EscortingProcessor {
        var nextId: Id = 1
        return EscortingProcessor(
            customerToTable = tables,
            eventGroups = eventGroups,
            inHouseGroupsToWaiter = inHouse,
            getInHouseGroups = { inHouse.keys.toList() },
            getServingPriority = { group ->
                when (group) {
                    is RegularGroup -> 0
                    is EventGroup -> 1
                    else -> 2
                }
            },
            recruitWaitersForEventGroup = { _, _ -> eventWaiters },
            getNextWaiterId = { nextId++ },
        )
    }

    private fun countertop(cooks: List<Cook> = listOf(Cook(CookType.TOURNANT))): Countertop {
        val supplier = Supplier(Stock(listOf(broth)))
        val pantry = Pantry(mutableListOf(IngredientPackage(broth)), supplier)
        return Countertop(pantry, ArrayDeque(), cooks, RestaurantType.EUROPEAN)
    }

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse =
        FrontOfHouse(tables = tables, waiters = waiters, drivers = emptyList(), countertop = countertop())

    private fun realEventGroup(id: Int, size: Int): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = prefs(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = Time.evening,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "Soup"),
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    /** seats and orders an event group of [size] on a table sized to fit it exactly */
    private fun seatedEvent(foh: FrontOfHouse, id: Int, size: Int): EventGroup {
        val event = realEventGroup(id, size)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        return event
    }

    // 1. escorted only when all dishes are eaten
    @Test
    fun `an event is escorted only once every dish has been eaten`() {
        val group = event(1, 2)
        group.currentOrder = orderOfStatuses(listOf(DishStatus.EATEN, DishStatus.SERVED))
        val waiter = Waiter()
        val tables = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(Table(1, 2, TableType.COMMON)))
        val proc = directProcessor(eventGroups = mutableListOf(group), tables = tables, eventWaiters = listOf(waiter))

        proc.processEscorting()
        assertEquals(2, group.customersRemainingInRestaurant, "one dish is not yet eaten, so nobody is escorted")
        assertTrue(lines().none { it.contains("FOH Escorting (") })

        group.currentOrder = orderOfStatuses(listOf(DishStatus.EATEN, DishStatus.EATEN))
        proc.processEscorting()
        assertEquals(0, group.customersRemainingInRestaurant, "once every dish is eaten, the whole group leaves")
        assertTrue(lines().any { it.contains("FOH Escorting (") })
    }

    // 2. lowest current load first
    @Test
    fun `the recruited waiter is the one with the lowest current load`() {
        val busy = Waiter().also { it.currentLoad = 8 }
        val idle = Waiter().also { it.currentLoad = 2 }
        val table = Table(1, 5, TableType.COMMON)
        // list order deliberately puts the busier waiter first, so only the sort can explain the result
        val foh = frontOfHouse(listOf(table), listOf(busy, idle))
        val event = seatedEvent(foh, 1, 5)
        requireNotNull(event.currentOrder).dishes.forEach { it.status = DishStatus.EATEN }

        foh.processEscorting()

        val idleId = requireNotNull(idle.id)
        val busyId = requireNotNull(busy.id)
        assertTrue(
            lines().any { it.contains("Waitstaff $idleId escorts 5 customers of group 1") },
            "the idler, not the busy waiter, does the escorting"
        )
        assertTrue(lines().none { it.contains("Waitstaff $busyId escorts") })
        assertEquals(0, event.customersRemainingInRestaurant)
    }

    // 3. equal load goes to lowest id (tiebrek)
    @Test
    fun `equal current load is broken by the lowest id`() {
        val highId = Waiter().also {
            it.currentLoad = 4
            it.id = 9
        }
        val lowId = Waiter().also {
            it.currentLoad = 4
            it.id = 2
        }
        val table = Table(1, 3, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(highId, lowId))
        val event = seatedEvent(foh, 1, 3)
        requireNotNull(event.currentOrder).dishes.forEach { it.status = DishStatus.EATEN }

        foh.processEscorting()

        assertTrue(lines().any { it.contains("Waitstaff 2 escorts 3 customers of group 1") })
        assertTrue(lines().none { it.contains("Waitstaff 9 escorts") })
    }

    // 4. partial escorting with a 10 limit, remainder next tick
    @Test
    fun `a lone waiter escorts at most 10 per tick and finishes the rest the next tick`() {
        val group = event(1, 15)
        group.currentOrder = eatenOrder(15)
        val waiter = Waiter()
        val tables = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(Table(1, 15, TableType.COMMON)))
        val proc = directProcessor(eventGroups = mutableListOf(group), tables = tables, eventWaiters = listOf(waiter))

        proc.processEscorting()
        assertEquals(5, group.customersRemainingInRestaurant, "only 10 of the 15 fit under the action limit")
        val waiterId = requireNotNull(waiter.id)
        assertTrue(lines().any { it.contains("Waitstaff $waiterId escorts 10 customers of group 1") })

        // simulate the next tick: the waiter's ESCORT tick load resets, the group's does not
        waiter.resetActionLoads()
        proc.processEscorting()
        assertEquals(0, group.customersRemainingInRestaurant, "the remaining 5 are escorted the following tick")
        assertTrue(lines().any { it.contains("Waitstaff $waiterId escorts 5 customers of group 1") })
    }

    // 5. one FOH Escorting line per waiter with a positive count
    @Test
    fun `each waiter with a positive share gets exactly one escorting line, waiters with none get none`() {
        val group = event(1, 13)
        group.currentOrder = eatenOrder(13)
        val first = Waiter()
        val second = Waiter()
        val third = Waiter() // recruited but never reached: the group is fully covered before it
        val tables = mutableMapOf<CustomerGroup, List<Table>>(group to listOf(Table(1, 13, TableType.COMMON)))
        val proc = directProcessor(
            eventGroups = mutableListOf(group),
            tables = tables,
            eventWaiters = listOf(first, second, third)
        )

        proc.processEscorting()

        val escortLines = lines().filter { it.contains("FOH Escorting (") }
        assertEquals(2, escortLines.size, "only the two waiters with a positive share are logged")
        assertTrue(escortLines.any { it.contains("Waitstaff ${requireNotNull(first.id)} escorts 10 customers") })
        assertTrue(escortLines.any { it.contains("Waitstaff ${requireNotNull(second.id)} escorts 3 customers") })
        assertNull(third.id, "the third waiter is never reached, so it never receives an id")
    }

    // 6. merged tables keep the lowest id (some overlap with F15 testing here)
    @Test
    fun `escorting a group seated on merged tables reports the lowest table id`() {
        val group = event(1, 4)
        group.currentOrder = eatenOrder(4)
        val waiter = Waiter()
        val tables = mutableMapOf<CustomerGroup, List<Table>>(
            group to listOf(Table(5, 2, TableType.COMMON), Table(2, 1, TableType.COMMON), Table(8, 1, TableType.COMMON))
        )
        val proc = directProcessor(eventGroups = mutableListOf(group), tables = tables, eventWaiters = listOf(waiter))

        proc.processEscorting()

        assertTrue(lines().any { it.contains("from table 2 outside.") })
        assertTrue(lines().none { it.contains("from table 5 outside.") || it.contains("from table 8 outside.") })
    }

    // 7. status counts distinct waiters and customers
    @Test
    fun `the escorting status counts a shared waiter once and sums both groups' customers`() {
        val shared = Waiter()
        val groupOne = casual(1, 2)
        val groupTwo = casual(2, 3)
        val inHouse = linkedMapOf<CustomerGroup, Waiter>(groupOne to shared, groupTwo to shared)
        val tables = mutableMapOf<CustomerGroup, List<Table>>(
            groupOne to listOf(Table(1, 2, TableType.COMMON)),
            groupTwo to listOf(Table(2, 3, TableType.COMMON)),
        )
        val proc = directProcessor(inHouse = inHouse, tables = tables)

        proc.processEscorting()

        assertEquals(
            "[DEBUG] FOH Escorting Status (R 1): 1 waitstaff escorted 5 customers this tick.",
            lines().last { it.contains("FOH Escorting Status") }
        )
    }

    /**
     * seats [group] so that only [waiter] can possibly be assigned (maxed out everyone else temporarily)
     * allows unit testing a specific CASUAL group with a specific waiter without relying on seating recruitment order
     */
    private fun seatExclusivelyWith(foh: FrontOfHouse, group: CasualGroup, waiter: Waiter, others: List<Waiter>) {
        val saved = others.associateWith { it.getTickLoad(ActionType.SEAT) }
        others.forEach { it.tickLoads[ActionType.SEAT] = Constants.ACTION_LIMIT }
        assertTrue(foh.processArrival(group, menu), "the group must actually be seated for this fixture to be valid")
        others.forEach { it.tickLoads[ActionType.SEAT] = saved.getValue(it) }
        assertTrue(waiter.getTickLoad(ActionType.SEAT) > 0, "the intended waiter must be the one that seated the group")
    }

    private fun casualWithCookedDishes(id: Int, cookedCount: Int): CasualGroup = CasualGroup(
        id = id,
        size = 2,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = prefs(2),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(Time.evening),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.ALWAYS,
    ).also { it.currentOrder = orderOfStatuses(List(cookedCount) { DishStatus.COOKED }) }

    // 8. Serving waiters in descending cooked-meal order with a lowest-id tie.
    @Test
    fun `event serving recruits the waiter with more cooked meals on their tables first`() {
        val busyWaiter = Waiter()
        val idleWaiter = Waiter()
        val eventTable = Table(1, 11, TableType.COMMON)
        val busyTable = Table(2, 2, TableType.COMMON)
        val idleTable = Table(3, 2, TableType.COMMON)
        val foh = frontOfHouse(listOf(eventTable, busyTable, idleTable), listOf(idleWaiter, busyWaiter))

        val busyGroup = casualWithCookedDishes(10, cookedCount = 5)
        seatExclusivelyWith(foh, busyGroup, busyWaiter, listOf(idleWaiter))
        busyGroup.currentOrder = orderOfStatuses(List(5) { DishStatus.COOKED })

        val idleGroup = casualWithCookedDishes(11, cookedCount = 2)
        seatExclusivelyWith(foh, idleGroup, idleWaiter, listOf(busyWaiter))
        idleGroup.currentOrder = orderOfStatuses(List(2) { DishStatus.COOKED })

        // 11 servable dishes need both waiters' full 10-capacity, so both must be recruited
        val event = realEventGroup(1, 11)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        val order = requireNotNull(event.currentOrder)
        order.dishes.forEach { it.status = DishStatus.COOKED }
        order.firstDishCookedAt = Time.tick

        foh.clearActionLoads()
        foh.processServing()

        val servingWaiterIds = lines()
            .filter { it.contains("FOH Serving (R 1): Waitstaff") && it.contains("table 1") }
            .map { it.substringAfter("Waitstaff ").substringBefore(" serves") }
        assertEquals(
            listOf(requireNotNull(busyWaiter.id).toString(), requireNotNull(idleWaiter.id).toString()),
            servingWaiterIds,
            "the waiter with more cooked meals on their own tables is served before the other"
        )
    }

    @Test
    fun `equal cooked-meal counts are broken by the lowest waiter id`() {
        val highId = Waiter().also { it.id = 9 }
        val lowId = Waiter().also { it.id = 2 }
        val eventTable = Table(1, 11, TableType.COMMON)
        val highIdTable = Table(2, 2, TableType.COMMON)
        val lowIdTable = Table(3, 2, TableType.COMMON)
        val foh = frontOfHouse(listOf(eventTable, highIdTable, lowIdTable), listOf(highId, lowId))

        val groupForHighId = casualWithCookedDishes(10, cookedCount = 3)
        seatExclusivelyWith(foh, groupForHighId, highId, listOf(lowId))
        groupForHighId.currentOrder = orderOfStatuses(List(3) { DishStatus.COOKED })

        val groupForLowId = casualWithCookedDishes(11, cookedCount = 3)
        seatExclusivelyWith(foh, groupForLowId, lowId, listOf(highId))
        groupForLowId.currentOrder = orderOfStatuses(List(3) { DishStatus.COOKED })

        val event = realEventGroup(1, 11)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        val order = requireNotNull(event.currentOrder)
        order.dishes.forEach { it.status = DishStatus.COOKED }
        order.firstDishCookedAt = Time.tick

        foh.clearActionLoads()
        foh.processServing()

        val firstServingWaiter = lines()
            .first { it.contains("FOH Serving (R 1): Waitstaff") && it.contains("table 1") }
            .substringAfter("Waitstaff ")
            .substringBefore(" serves")
        assertEquals("2", firstServingWaiter, "equal cooked-meal counts fall back to the lowest id")
    }

    // 9. capacity rule for a complete table
    @Test
    fun `a complete event order is served all at once or not at all this tick`() {
        val waiter = Waiter().also { it.addToTickLoad(ActionType.SERVE, Constants.ACTION_LIMIT - 1) }
        val table = Table(1, 2, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(waiter))
        val event = seatedEvent(foh, 1, 2)
        val order = requireNotNull(event.currentOrder)
        order.dishes.forEach { it.status = DishStatus.COOKED }
        order.firstDishCookedAt = Time.tick

        foh.clearActionLoads()
        waiter.addToTickLoad(ActionType.SERVE, Constants.ACTION_LIMIT - 1)
        foh.processServing()

        assertTrue(order.dishes.all { it.status == DishStatus.COOKED }, "1 free slot cannot cover 2 dishes at once")
        val waiterId = requireNotNull(waiter.id)
        assertTrue(
            lines().any { it.contains("FOH No Serving (R 1): Waitstaff $waiterId did not serve 2 meals to table 1.") }
        )

        // give the waiter full capacity back and retry the same (still complete, unserved) order
        waiter.resetActionLoads()
        foh.processServing()
        assertTrue(order.dishes.all { it.status == DishStatus.SERVED }, "full capacity now covers both dishes at once")
    }

    /**
     * in `serveEventTable` in case an event group somehow without an assigned table (invariant, cannot happen)
     * then it should be skipped and not crash; FOH never leaves a group in `eventGroups` without a table entry
     */
    @Test
    fun `an event with no assigned table is skipped without crashing`() {
        val event = event(1, 2)
        event.currentOrder = orderOfStatuses(listOf(DishStatus.COOKED, DishStatus.COOKED))
        val serving = ServingProcessor(
            waiters = emptyList(),
            drivers = emptyList(),
            deliveryGroups = emptyList(),
            getInHouseGroups = { listOf(event) },
            waiterFor = { null },
            getServingPriority = { 1 },
            getAssignedTableId = { null },
            recruitWaitersForEventGroup = { _, _ -> emptyList() },
            getNextWaiterId = { 1 },
        )

        serving.processServing()

        // DOTO: can't use assertTrue with a nullable boolean, find a better way
        assertTrue(event.currentOrder?.dishes?.all { it.status == DishStatus.COOKED } == true)
        assertTrue(lines().none { it.contains("FOH Serving (") || it.contains("FOH No Serving (") })
    }

    /**
     * when no waiter is eligible for SERVE, an incomplete event order within its wait
     * window has no candidate to log against, so `logNoServing` should do nothing
     */
    @Test
    fun `an incomplete event order with no eligible waiter logs nothing`() {
        val event = event(1, 2)
        event.currentOrder = orderOfStatuses(listOf(DishStatus.COOKED, DishStatus.UNCOOKED))
        event.currentOrder?.firstDishCookedAt = Time.tick
        val serving = ServingProcessor(
            waiters = emptyList(),
            drivers = emptyList(),
            deliveryGroups = emptyList(),
            getInHouseGroups = { listOf(event) },
            waiterFor = { null },
            getServingPriority = { 1 },
            getAssignedTableId = { 1 },
            recruitWaitersForEventGroup = { _, _ -> emptyList() },
            getNextWaiterId = { 1 },
        )

        serving.processServing()

        assertTrue(lines().none { it.contains("FOH Serving (") || it.contains("FOH No Serving (") })
    }
}
