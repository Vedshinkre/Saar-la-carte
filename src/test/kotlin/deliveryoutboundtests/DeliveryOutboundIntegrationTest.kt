package deliveryoutboundtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.helpers.DeliveryProcessor
import de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for F29's outbound half: the real [ServingProcessor] handing a cooked
 * delivery order to a driver (F20) and the real [DeliveryProcessor] taking over from there.
 */
class DeliveryOutboundIntegrationTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 4
    }

    private val recipe = Recipe(1, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(), null)

    private fun logLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun deliveryGroup(id: Int, distance: Int = 5, visitingAt: Int = 10): CasualGroup {
        val group = CasualGroup(
            id = id,
            size = 2,
            tableType = TableType.COMMON,
            visitingAt = visitingAt,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = distance,
            ratingLikelihood = RatingLikelihood.NEVER
        )
        group.currentOrder = Order(List(2) { Dish(recipe).apply { status = DishStatus.COOKED } })
        return group
    }

    private class Pipeline(val serving: ServingProcessor, val delivering: DeliveryProcessor) {
        fun tick() {
            serving.processServing()
            delivering.processDelivering()
        }
    }

    private fun pipeline(groups: List<CustomerGroup>, drivers: List<Driver>): Pipeline {
        val waiter = Waiter().apply { id = 1 }
        val serving = ServingProcessor(
            waiters = listOf(waiter),
            drivers = drivers,
            deliveryGroups = groups,
            getInHouseGroups = { emptyList() },
            waiterFor = { null },
            getServingPriority = { 2 },
            getAssignedTableId = { null },
            recruitWaitersForEventGroup = { _, _ -> emptyList() },
            getNextWaiterId = { 1 }
        )
        return Pipeline(serving, DeliveryProcessor(drivers, groups))
    }

    @Test
    fun `a handed over order is prepared, driven and delivered in that order`() {
        val group = deliveryGroup(id = 1, distance = 10)
        val driver = Driver()
        val pipeline = pipeline(listOf(group), listOf(driver))

        repeat(4) {
            pipeline.tick()
            Time.tick += 1
        }

        val lines = logLines()
        val prep = lines.indexOfFirst { it.contains("Delivery Preparation") }
        val arrival = lines.indexOfFirst { it.contains("Delivery Arrival") }
        val finished = lines.indexOfFirst { it.contains("Delivery Finished") }
        assertTrue(prep in 0 until arrival, "preparation comes before arrival")
        assertTrue(arrival < finished, "hand-over follows arrival")
        assertTrue(lines.any { it.contains("Delivery Preparation (R 1): Driver 1 prepares driving") })
    }

    @Test
    fun `a one tick delivery prepared in tick 4 arrives in tick 5`() {
        val group = deliveryGroup(id = 1, distance = 5)
        val driver = Driver()
        val pipeline = pipeline(listOf(group), listOf(driver))

        pipeline.tick() // tick 4: hand-over + preparation

        assertFalse(logLines().any { it.contains("Delivery Arrival") }, "must not arrive in the preparation tick")
        assertEquals(DriverState.DELIVERING, driver.state)

        Time.tick += 1
        pipeline.tick() // tick 5: the one driving tick, arrival at its end

        assertTrue(logLines().any { it.contains("Delivery Arrival (R 1): Driver 1 arrived at group 1") })
    }

    @Test
    fun `simultaneously prepared drivers are prepared in ascending group id order`() {
        val groupFive = deliveryGroup(id = 5)
        val groupTwo = deliveryGroup(id = 2)
        listOf(groupFive, groupTwo).forEach { g ->
            g.currentOrder!!.dishes.forEach { it.status = DishStatus.SERVED }
        }
        val driverA = Driver().apply {
            id = 1
            targetGroup = groupFive
            currentOrder = groupFive.currentOrder
            state = DriverState.WAITING
        }
        val driverB = Driver().apply {
            id = 2
            targetGroup = groupTwo
            currentOrder = groupTwo.currentOrder
            state = DriverState.WAITING
        }

        DeliveryProcessor(listOf(driverA, driverB), listOf(groupFive, groupTwo)).processDelivering()

        val prepLines = logLines().filter { it.contains("Delivery Preparation") }
        assertTrue(prepLines[0].contains("to group 2"))
        assertTrue(prepLines[1].contains("to group 5"))
    }

    @Test
    fun `with a single driver only the first group's order is handed over`() {
        val first = deliveryGroup(id = 1)
        val second = deliveryGroup(id = 2)
        val driver = Driver()
        val pipeline = pipeline(listOf(first, second), listOf(driver))

        pipeline.tick()

        assertTrue(first.currentOrder!!.dishes.all { it.status == DishStatus.SERVED })
        assertTrue(second.currentOrder!!.dishes.all { it.status == DishStatus.COOKED }, "no free driver for group 2")
        assertEquals(1, driver.id)
    }

    @Test
    fun `a driver is only available while idle`() {
        val idle = Driver()
        val busy = Driver().apply { state = DriverState.DELIVERING }

        assertTrue(DeliveryProcessor(listOf(idle), emptyList()).isDriverAvailable())
        assertFalse(DeliveryProcessor(listOf(busy), emptyList()).isDriverAvailable())
        assertTrue(DeliveryProcessor(listOf(busy, idle), emptyList()).isDriverAvailable())
    }
}
