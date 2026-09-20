package eventseatingtests

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * P02 (FOH - Event Seating)
 *
 * build waiter state up through prior seatings (or failures) rather than presetting it (like in the unit test)
 * check how event seating interacts with REGULAR/CASUAL groups sharing the same waitstaff and tick
 */
class EventSeatingIntegrationTest {
    private lateinit var output: StringWriter

    private val broth = Ingredient("broth", MeasurementUnit.ML, 1000, 1000)
    private val soup = Recipe(1, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(broth to 1), null)
    private val menu = listOf(soup)

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

    private fun countertop(stockAmount: Int = 1000): Countertop {
        val supplier = Supplier(Stock(listOf(broth)))
        val pantry = Pantry(
            mutableListOf(IngredientPackage(broth, stockAmount, Time.evening + 1000, false)),
            supplier
        )
        return Countertop(pantry, ArrayDeque(), listOf(Cook(CookType.TOURNANT)), RestaurantType.EUROPEAN)
    }

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>, stockAmount: Int = 1000): FrontOfHouse =
        FrontOfHouse(tables = tables, waiters = waiters, drivers = emptyList(), countertop = countertop(stockAmount))

    private fun prefs(size: Int) = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) }

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

    private fun eventGroup(id: Int, size: Int): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = prefs(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = Time.evening,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "Soup")
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    /**
     * two REGULAR groups get assigned the same waiter (keeps giving work to the busiest still-eligible waiter, to
     * preserve capacity elsewhere), and the event that follows must still prioritize
     * busier waiter first, (as it would if the load had been preset directly)
     */
    @Test
    fun `waiters busied by earlier regular groups are still recruited in the right order for an event`() {
        val w1 = Waiter()
        val w2 = Waiter()
        val eventTable = Table(3, 12, TableType.COMMON)
        val foh = frontOfHouse(listOf(eventTable), listOf(w1, w2))

        // two REGULAR groups are seated without a prior reservation: seating is a no-op for the
        // table but the waiter bookkeeping under test still runs (see ArrivalProcessor.
        // seatRegularOrCasualGroup), which is all this test needs
        assertTrue(foh.processArrival(regularGroup(1, 3), menu))
        assertTrue(foh.processArrival(regularGroup(2, 4), menu))

        val busier = if (w1.currentLoad > w2.currentLoad) w1 else w2
        val idler = if (busier === w1) w2 else w1
        assertEquals(7, busier.currentLoad, "rule 2 keeps piling work onto the same waiter")
        assertEquals(0, idler.currentLoad)

        val event = eventGroup(3, 12)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))

        // busier waiter had 3 seats of remaining SEATING capacity, the more idle one had 10
        assertEquals(10, busier.getTickLoad(ActionType.SEAT))
        assertEquals(9, idler.getTickLoad(ActionType.SEAT))
        assertEquals(12, event.currentOrder?.dishes?.size)
        val seatingLine = lines().first { it.contains("FOH Seating (R 1): Group 3 seated") }
        assertTrue(
            seatingLine.endsWith("by waitstaff ${listOfNotNull(busier.id, idler.id).sorted().joinToString(",")}."),
            "both waiters that contributed are named, sorted ascending"
        )
    }

    /**
     * an event group is served/escorted through the event-recruitment, never
     * through the assigned-waiter map used for REGULAR/CASUAL groups. If the group were accidentally
     * added to that map too, it would show up twice in the escorting pass and be escorted twice
     * in the same tick producing two `FOH Escorting` lines
     */
    @Test
    fun `waiters used for an event keep no assigned-waiter entry for that group`() {
        val waiter = Waiter()
        val table = Table(1, 4, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(waiter))
        val event = eventGroup(1, 4)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        requireNotNull(event.currentOrder).dishes.forEach { it.status = DishStatus.EATEN }

        foh.processEscorting()

        val escortLines = lines().filter { it.contains("FOH Escorting (R 1): Waitstaff") && it.contains("group 1") }
        assertEquals(1, escortLines.size, "the event group must not be escorted twice in the same tick")
        assertEquals(0, event.customersRemainingInRestaurant)
    }

    /**
     * a failed EVENT seating still consumes the waiters' SEATING tick load for the
     * whole tick (unit test 7), which means a REGULAR or CASUAL group arriving in the very same
     * tick can find no free waitstaff left, even though nobody was actually seated.
     */
    @Test
    fun `a failed event seating leaves no SEATING capacity for a casual group in the same tick`() {
        val waiter = Waiter()
        val eventTable = Table(1, 11, TableType.COMMON)
        val foh = frontOfHouse(listOf(eventTable), listOf(waiter))
        val event = eventGroup(1, 11) // one more than the lone waiter's 10-seat capacity
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        assertEquals(
            Constants.ACTION_LIMIT,
            waiter.getTickLoad(ActionType.SEAT),
            "the failed attempt still charges the waiter"
        )

        val casual = casualGroup(2, 1)
        assertFalse(foh.processArrival(casual, menu), "with no free waiter left this tick, the group stays queued")
        assertTrue(
            lines().any { it.contains("FOH No Seating (R 1): No free waitstaff available for group 2.") }
        )
    }

    /** the tick load reset between ticks frees the waiter up again for the very next tick */
    @Test
    fun `a tick-load reset lets the same waiter seat a casual group the very next tick`() {
        val waiter = Waiter()
        val eventTable = Table(1, 11, TableType.COMMON)
        val casualTable = Table(2, 1, TableType.COMMON)
        val foh = frontOfHouse(listOf(eventTable, casualTable), listOf(waiter))
        val event = eventGroup(1, 11)
        assertTrue(foh.reserveTables(event))
        assertTrue(foh.processArrival(event, menu))
        assertFalse(foh.processArrival(casualGroup(2, 1), menu))

        Time.tick += 1
        foh.clearActionLoads()
        val casual = casualGroup(3, 1)
        assertTrue(foh.processArrival(casual, menu), "a fresh tick clears the waiter's SEATING tick load")
        assertTrue(lines().any { it.contains("FOH Seating (R 1): Group 3 seated") })
    }

    /**
     * SEATING and TAKE_ORDER must go through walk the same waiter list in the same order (`consumedWaiters`)
     * so the waiter recruited first for SEATING is also the first one handed customers for TAKE_ORDER
     * TAKE_ORDER has an independent 10-action limit though, so its split does not have to mirror the (10/4/1) SEATING
     * split the first waiter still takes 10 orders, but the leftover 5 all fit within
     * the second waiter's own limit, so the third waiter, despite having seated one customer, never takes an order
     */
    @Test
    fun `the seating order determines which waiter is offered orders first, independent of the seating split`() {
        // took example from the specification, so SEATING is split 10/4/1 as in the unit test
        val first = Waiter().also { it.currentLoad = 15 }
        val second = Waiter().also {
            it.currentLoad = 11
            it.tickLoads[ActionType.SEAT] = 6
        }
        val third = Waiter()
        val table = Table(1, 15, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(first, second, third))
        val event = eventGroup(1, 15)
        assertTrue(foh.reserveTables(event))

        assertTrue(foh.processArrival(event, menu))

        assertEquals(10, first.getTickLoad(ActionType.SEAT))
        assertEquals(10, second.getTickLoad(ActionType.SEAT), "6 preexisting + 4 newly assigned")
        assertEquals(1, third.getTickLoad(ActionType.SEAT))

        // TAKE_ORDER is a separate, independently counted action: the first waiter still fills up
        // to 10, but the remaining 5 all fit under the second waiter's own limit
        assertEquals(10, first.getTickLoad(ActionType.TAKE_ORDER), "waiter 1 takes the first 10 orders")
        assertEquals(5, second.getTickLoad(ActionType.TAKE_ORDER), "waiter 2 takes all 5 remaining orders")
        assertEquals(
            0,
            third.getTickLoad(ActionType.TAKE_ORDER),
            "waiter 3 seated one customer but never takes an order"
        )

        // the FOH Ordering line still names every waiter that took part in seating, not just the ones who took orders
        val orderingLine = lines().first { it.contains("FOH Ordering (R 1): Group 1 placed") }
        val ids = listOfNotNull(first.id, second.id, third.id).sorted().joinToString(",")
        assertTrue(orderingLine.endsWith("with waitstaff $ids."))
    }

    /**
     * a successful SEATING can still be followed by a total ordering failure (nobody in the
     * group finds a dish, e.g. because the pantry is empty)
     * the group is then turned away with a negative experience and never joins `eventGroups`
     * though waiters were already counted for seating them
     */
    @Test
    fun `a seated event that cannot order anything is turned away with a negative experience`() {
        val waiter = Waiter()
        val table = Table(1, 3, TableType.COMMON)
        val foh = frontOfHouse(listOf(table), listOf(waiter), stockAmount = 0)
        val event = eventGroup(1, 3)
        assertTrue(foh.reserveTables(event))

        assertTrue(foh.processArrival(event, menu))

        assertEquals(ExperienceType.NEGATIVE, event.experience)
        assertEquals(0, event.customersRemainingInRestaurant)
        assertEquals(3, waiter.getTickLoad(ActionType.SEAT), "seating already happened before ordering failed")
        assertTrue(lines().none { it.contains("FOH Ordering (R 1): Group 1 placed") })
        assertTrue(
            lines().any {
                it.contains("FOH No Ordering (R 1): Group 1 could not place an order for 3 customers")
            }
        )

        // never having joined eventGroups, the group is skipped by later escorting
        foh.processEscorting()
        assertTrue(lines().none { it.contains("FOH Escorting (") })
    }
}
