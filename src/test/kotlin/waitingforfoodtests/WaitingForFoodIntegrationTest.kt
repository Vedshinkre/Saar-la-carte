package waitingforfoodtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import eventorderingtests.EventOrderingFixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * F27 together with ordering (F18), seating, serving (F19), escorting and delivery (F20): groups order through the
 * real [FrontOfHouse], a stand-in kitchen cooks what the test wants, and the wait / leave / eat behaviour is
 * checked through the FOH tick phases in their specified order.
 */
class WaitingForFoodIntegrationTest {
    private lateinit var output: StringWriter
    private val orderQueue = ArrayDeque<Order>()
    private val drivers = mutableListOf<Driver>()
    private lateinit var foh: FrontOfHouse

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Order.resetIds()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
        orderQueue.clear()
        drivers.clear()
        foh = frontOfHouse(tableSizes = listOf(2, 2), waiterCount = 1)
    }

    private fun frontOfHouse(tableSizes: List<Int>, waiterCount: Int) = FrontOfHouse(
        tables = tableSizes.mapIndexed { index, size -> EventOrderingFixtures.table(index + 1, size) },
        waiters = List(waiterCount) { EventOrderingFixtures.waiter() },
        drivers = drivers,
        countertop = EventOrderingFixtures.countertop(orderQueue)
    )

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun lines(text: String): List<String> = lines().filter { it.contains(text) }

    private fun casual(id: Int, size: Int, visitingAt: Int = 1, distance: Int = 0): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = List(size) { EventOrderingFixtures.noPreference() },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = distance,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    /** the group arrives and orders in the current tick */
    private fun arrive(group: CustomerGroup) {
        foh.processArrival(group, EventOrderingFixtures.menu)
    }

    private fun cook(group: CustomerGroup, vararg dishIndexes: Int) {
        val dishes = group.currentOrder!!.dishes
        val chosen = if (dishIndexes.isEmpty()) dishes else dishIndexes.map { dishes[it] }
        chosen.forEach { it.status = DishStatus.COOKED }
    }

    /** the FOH phases of one tick in the specified order */
    private fun tick() {
        foh.clearActionLoads()
        foh.processServing()
        foh.processDelivering()
        foh.processEating()
        foh.processEscorting()
        foh.processRatings(0, 0)
        Time.tick += 1
    }

    private fun tickUntil(tick: Int) {
        while (Time.tick < tick) tick()
    }

    private fun tickThrough(tick: Int) = tickUntil(tick + 1)

    // ---- nothing is ever cooked

    @Test
    fun `a group whose food never arrives leaves five ticks after ordering and not earlier`() {
        val group = casual(1, size = 2)
        arrive(group) // ordered in tick 1
        assertNotNull(group.currentOrder)

        tickThrough(5)
        assertTrue(lines("Restaurant No Eating").isEmpty(), "four ticks after ordering the group still waits")

        tick() // tick 6 = five ticks after ordering
        assertEquals(
            listOf("[INFO] Restaurant No Eating (R 1): 2 customers of group 1 leave table 1 due to not being served."),
            lines("Restaurant No Eating")
        )
        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(group.currentOrder!!.dishes.all { it.status == DishStatus.ABORTED })
    }

    @Test
    fun `after a whole casual group walked out its table is free for the next group`() {
        val first = casual(1, size = 2)
        arrive(first)
        val tableBefore = foh.getAvailableSeats().values.sum()
        tickUntil(7)

        val second = casual(2, size = 2, visitingAt = 7)
        arrive(second)

        assertTrue(foh.getAvailableSeats().values.sum() >= tableBefore, "the freed table can host the next group")
        assertNotNull(second.currentOrder)
        assertTrue(lines("FOH Seating (R 1): Group 2").isNotEmpty(), lines().joinToString("\n"))
    }

    // ---- food arrives in time

    @Test
    fun `food cooked in time is served, eaten in two ticks, and the group leaves happy`() {
        val group = casual(1, size = 2)
        arrive(group)
        cook(group)

        tick() // tick 1: order complete and cooked, waiter serves at once
        val order = group.currentOrder!!
        assertTrue(order.dishes.all { it.status == DishStatus.SERVED })
        assertEquals(1, lines("FOH Serving (R 1): Waitstaff 1 serves").size)

        tickThrough(2)
        assertTrue(lines("FOH Finished Eating").isEmpty(), "the meal takes two full ticks")
        tick()

        assertEquals(
            listOf("[INFO] FOH Finished Eating (R 1): 2 customers of group 1 have finished eating at table 1."),
            lines("FOH Finished Eating")
        )
        assertTrue(lines("Restaurant No Eating").isEmpty())
        assertEquals(ExperienceType.POSITIVE, group.experience)
        assertEquals(0, group.customersRemainingInRestaurant, "the waiter escorted the customers out")
        assertEquals(1, lines("FOH Escorting").filter { it.contains("group 1") || it.contains("Group 1") }.size)
    }

    @Test
    fun `food served four ticks after ordering is still a positive experience`() {
        val group = casual(1, size = 2)
        arrive(group)

        tickUntil(5) // ticks 1..4 pass with nothing cooked
        cook(group)
        tick() // tick 5: four ticks after ordering

        assertEquals(ExperienceType.POSITIVE, group.experience)
        assertTrue(lines("Restaurant No Eating").isEmpty())
    }

    @Test
    fun `food served exactly five ticks after ordering arrives before the group leaves and is neutral`() {
        val group = casual(1, size = 2)
        arrive(group)

        tickUntil(6)
        cook(group)
        tick() // tick 6: serving runs before eating, so the customer is served before the wait check

        assertTrue(lines("Restaurant No Eating").isEmpty())
        assertEquals(1, lines("FOH Serving (R 1)").size)
        assertEquals(ExperienceType.NEUTRAL, group.experience)
    }

    @Test
    fun `once the customers left nothing is served to their table any more`() {
        val group = casual(1, size = 2)
        arrive(group)
        tickUntil(7) // the group left in tick 6

        assertEquals(1, lines("Restaurant No Eating").size)
        assertTrue(lines("FOH Serving (R 1)").isEmpty())
        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
    }

    // ---- partial service

    @Test
    fun `partially served group - the unserved customers leave seven ticks after ordering`() {
        val group = casual(1, size = 2)
        arrive(group) // tick 1
        val order = group.currentOrder!!
        cook(group, 0)
        order.firstDishCookedAt = 1

        tickThrough(3)
        assertTrue(lines("Restaurant No Eating").isEmpty(), "still waiting")
        assertEquals(DishStatus.SERVED, order.dishes[0].status, "partial serving starts after the wait window")

        tickThrough(7) // ticks 4..7: up to six ticks after ordering
        assertTrue(lines("Restaurant No Eating").isEmpty(), "two more ticks after the first meal were served")

        tick() // tick 8 = seven ticks after ordering
        assertEquals(
            listOf("[INFO] Restaurant No Eating (R 1): 1 customers of group 1 leave table 1 due to not being served."),
            lines("Restaurant No Eating")
        )
        assertEquals(1, group.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(order.dishes.drop(1).all { it.status == DishStatus.ABORTED })
    }

    @Test
    fun `the customer served before the others left still finishes eating`() {
        val group = casual(1, size = 2)
        arrive(group)
        val order = group.currentOrder!!
        cook(group, 0)
        order.firstDishCookedAt = 1

        tickUntil(9)

        assertEquals(DishStatus.EATEN, order.dishes[0].status)
        assertEquals(DishStatus.ABORTED, order.dishes[1].status)
        assertEquals(1, lines("Restaurant No Eating").size)
        assertTrue(lines("FOH Finished Eating").single().contains("1 customers of group 1"))
    }

    // ---- several groups

    @Test
    fun `groups wait independently - one leaves while the other one eats`() {
        val hungry = casual(1, size = 2)
        val fed = casual(2, size = 2)
        arrive(hungry)
        arrive(fed)
        cook(fed)

        tickUntil(7)

        val leaving = lines("Restaurant No Eating")
        assertEquals(1, leaving.size)
        assertTrue(leaving.single().contains("group 1"), leaving.toString())
        assertEquals(ExperienceType.NEGATIVE, hungry.experience)
        assertEquals(ExperienceType.POSITIVE, fed.experience)
        assertTrue(lines("FOH Finished Eating").single().contains("group 2"))
    }

    @Test
    fun `a REGULAR group nobody served gets a failed attempt`() {
        val regular = EventOrderingFixtures.regularGroup(id = 1, size = 2)
        foh.reserveTables(regular)
        arrive(regular)

        tickUntil(7)

        assertEquals(1, regular.failedAttempts)
        assertEquals(1, lines("Restaurant No Eating").size)
    }

    @Test
    fun `a REGULAR group that ate does not get a failed attempt`() {
        val regular = EventOrderingFixtures.regularGroup(id = 1, size = 2)
        foh.reserveTables(regular)
        arrive(regular)
        cook(regular)

        tickUntil(7)

        assertEquals(0, regular.failedAttempts)
        assertTrue(lines("Restaurant No Eating").isEmpty())
    }

    // ---- delivery

    @Test
    fun `a delivery group gives up three ticks after it wanted the food`() {
        drivers.add(Driver())
        val group = casual(1, size = 2, visitingAt = 5, distance = 5)
        Time.tick = 4
        arrive(group)

        tickUntil(8)
        assertTrue(lines("Delivery Given Up").isEmpty(), "still waiting at tick 7")

        tick() // tick 8 = visitingAt + 3
        assertEquals(1, lines("Delivery Given Up (R 1): Group 1 gave up on waiting").size)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(lines("Restaurant No Eating").isEmpty(), "delivery customers never use the in-restaurant rule")
    }

    @Test
    fun `a delivery that arrives in time is eaten and never given up`() {
        drivers.add(Driver())
        val group = casual(1, size = 2, visitingAt = 10, distance = 5)
        Time.tick = 4
        arrive(group)
        cook(group)

        tickUntil(14)

        assertTrue(lines("Delivery Given Up").isEmpty())
        assertEquals(1, lines("Delivery Finished Eating (R 1): Group 1 has finished eating.").size)
        assertEquals(2, foh.numberOfCustomersDelivered)
    }
}
