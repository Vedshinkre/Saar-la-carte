package deliveryhandoverandreturntests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ordering (F18) and delivery (F20) together: a delivery group orders through the real
 * [FrontOfHouse] and countertop without any waiter, the "kitchen" cooks the queued order, and the
 * order then travels waiter -> driver -> customer -> eaten, with the logs in the specified order.
 */
class DeliveryOrderingIntegrationTest {
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
        Time.tick = 4
        orderQueue.clear()
        drivers.clear()
        foh = FrontOfHouse(
            tables = emptyList(),
            waiters = listOf(EventOrderingFixtures.waiter(id = 1)),
            drivers = drivers,
            countertop = EventOrderingFixtures.countertop(orderQueue)
        )
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun deliveryGroup(
        id: Int,
        preferences: List<FoodPreference>,
        distance: Int = 5,
        visitingAt: Int = 10,
    ) = CasualGroup(
        id = id,
        size = preferences.size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = preferences,
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = distance,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    private fun order(group: CasualGroup) {
        foh.processArrival(group, EventOrderingFixtures.menu)
    }

    /** stands in for the kitchen: everything in the queue is cooked */
    private fun cookQueuedOrders() = orderQueue.forEach { o -> o.dishes.forEach { it.status = DishStatus.COOKED } }

    /** the FOH steps of one tick that concern deliveries, in the specified order */
    private fun tick() {
        foh.clearActionLoads()
        foh.processServing()
        foh.processDelivering()
        foh.processEating()
        Time.tick += 1
    }

    @Test
    fun `a delivery group orders without a waiter and its order reaches the kitchen queue`() {
        drivers.add(Driver())
        val group = deliveryGroup(1, List(2) { EventOrderingFixtures.noPreference() })

        order(group)

        val placed = group.currentOrder!!
        assertEquals(2, placed.dishes.size)
        assertTrue(placed.dishes.all { it.recipe.name == "Rice" && it.status == DishStatus.UNCOOKED })
        assertEquals(listOf(placed), orderQueue.toList())
        val ordering = lines().single { it.contains("FOH Ordering (R 1)") }
        val expectedStart = "[IMPORTANT] FOH Ordering (R 1): Group 1 placed order ${placed.id} of "
        assertTrue(ordering.startsWith(expectedStart), ordering)
        assertTrue(ordering.endsWith("."), "the log ends after the dishes, without waitstaff: $ordering")
        assertFalse(ordering.contains("waitstaff"), ordering)
        assertTrue(lines().none { it.contains("FOH Seating (R 1)") }, "deliveries are not seated")
    }

    @Test
    fun `a group that can order nothing is turned away and does not tie up a driver`() {
        drivers.add(Driver())
        val group = deliveryGroup(1, listOf(EventOrderingFixtures.excludesEverything()))

        order(group)

        assertNull(group.currentOrder)
        assertTrue(orderQueue.isEmpty())
        assertEquals(1, foh.getAvailableDrivers())
    }

    @Test
    fun `an ordered delivery does not tie up a driver before the meals are cooked`() {
        drivers.add(Driver())
        drivers.add(Driver())

        order(deliveryGroup(1, listOf(EventOrderingFixtures.noPreference())))

        assertEquals(2, foh.getAvailableDrivers(), "meals queue until the whole order is ready and a driver is free")
    }

    @Test
    fun `an order that is not cooked yet is not handed to a driver`() {
        val driver = Driver()
        drivers.add(driver)
        order(deliveryGroup(1, listOf(EventOrderingFixtures.noPreference())))

        tick()

        assertEquals(DriverState.IDLE, driver.state)
        assertTrue(lines().none { it.contains("FOH Delivery") })
    }

    @Test
    fun `an ordered and cooked delivery is handed over, driven, delivered, eaten and counted`() {
        val driver = Driver()
        drivers.add(driver)
        val group = deliveryGroup(1, List(2) { EventOrderingFixtures.noPreference() }, distance = 5)
        order(group)
        val placed = group.currentOrder!!
        cookQueuedOrders()

        repeat(2) { tick() } // tick 4: hand-over and preparation, tick 5: driving, arrival, delivery

        assertEquals(5, placed.deliveredAt)
        assertEquals(2, foh.numberOfCustomersDelivered)
        repeat(2) { tick() } // tick 6 back at the restaurant, tick 7 last eating tick

        val log = lines()
        val expectedInOrder = listOf(
            "FOH Delivery (R 1): Waitstaff 1 serves",
            "Delivery Preparation (R 1): Driver 1 prepares driving order ${placed.id} to group 1, " +
                "which will take 1 ticks.",
            "Delivery Driving (R 1): Driver 1 drove 5 km and needs 0 more ticks.",
            "Delivery Arrival (R 1): Driver 1 arrived at group 1 with order ${placed.id}.",
            "Delivery Finished (R 1): Driver 1 gave delivery of order ${placed.id} to group 1.",
            "Delivery Returned (R 1): Driver 1 has returned.",
            "Delivery Finished Eating (R 1): Group 1 has finished eating."
        )
        var from = 0
        for (expected in expectedInOrder) {
            val at = log.drop(from).indexOfFirst { it.contains(expected) }
            assertTrue(at >= 0, "missing or out of order: $expected\n${log.joinToString("\n")}")
            from += at + 1
        }
        assertEquals(DriverState.IDLE, driver.state)
    }

    @Test
    fun `with one driver the second delivery waits until the first driver is back`() {
        val driver = Driver()
        drivers.add(driver)
        val first = deliveryGroup(1, listOf(EventOrderingFixtures.noPreference()), distance = 5)
        val second = deliveryGroup(2, listOf(EventOrderingFixtures.noPreference()), distance = 5)
        order(first)
        order(second)
        cookQueuedOrders()

        tick() // tick 4: the only driver takes the first order
        assertTrue(driver.targetGroup === first)
        assertTrue(second.currentOrder!!.dishes.all { it.status == DishStatus.COOKED })

        tick() // tick 5: delivered, driver turns around
        tick() // tick 6: driver is back and idle again
        assertEquals(DriverState.IDLE, driver.state)
        assertTrue(second.currentOrder!!.dishes.all { it.status == DishStatus.COOKED }, "hand-over is next tick")

        tick() // tick 7: the second order is handed to the same driver, who keeps id 1
        assertTrue(driver.targetGroup === second)
        assertEquals(1, driver.id)
        assertTrue(second.currentOrder!!.dishes.all { it.status == DishStatus.SERVED })
    }

    @Test
    fun `a delivery nobody can bring in time is given up and never delivered`() {
        drivers.add(Driver())
        val group = deliveryGroup(1, listOf(EventOrderingFixtures.noPreference()), distance = 5, visitingAt = 5)
        order(group)
        // the kitchen never cooks the meals

        repeat(6) { tick() } // 4..9, the group gives up in tick 8 (visitingAt 5 + 3)

        val log = lines()
        assertEquals(1, log.count { it.contains("Delivery Given Up (R 1): Group 1 gave up on waiting") })
        assertFalse(log.any { it.contains("Delivery Finished (R 1)") })
        assertEquals(0, foh.numberOfCustomersDelivered)
    }
}
