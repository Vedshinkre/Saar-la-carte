package deliveryoutboundtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.helpers.DeliveryProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the outbound half of F29: [DeliveryProcessor.processDelivering] preparing a
 * delivery (WAITING -> DELIVERING), driving to the customer and handing over there. The return
 * trip and end-of-evening handling are out of scope (see the F29 split).
 */
class DeliveryOutboundTripTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 4
    }

    private fun logLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun deliveryGroup(id: Int = 1, distance: Int = 5, visitingAt: Int = 10): CasualGroup = CasualGroup(
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

    private fun servedOrder(): Order {
        val recipe = Recipe(1, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(), null)
        return Order(List(2) { Dish(recipe).apply { status = DishStatus.SERVED } })
    }

    /** One delivery phase of a tick; the group's order is registered like FrontOfHouse would. */
    private fun tick(driver: Driver, group: CasualGroup) {
        Time.tick += 1
        DeliveryProcessor(listOf(driver), listOf(group)).processDelivering()
    }

    private fun waitingDriver(group: CasualGroup, order: Order = servedOrder(), id: Int = 1): Driver {
        group.currentOrder = order
        return Driver().apply {
            this.id = id
            targetGroup = group
            currentOrder = order
            state = DriverState.WAITING
        }
    }

    /** Puts a driver mid-trip so driving can be tested without going through preparation. */
    private fun drivingDriver(group: CasualGroup, order: Order, oneWayTicks: Int): Driver {
        group.currentOrder = order
        return Driver().apply {
            id = 1
            targetGroup = group
            currentOrder = order
            state = DriverState.DELIVERING
            ticksToDest = oneWayTicks
            totalTripTicks = oneWayTicks * 2
            tripDistance = group.deliveryDistance
        }
    }

    // ---- Preparation ----

    @Test
    fun `preparing a delivery takes ceil of distance over 5 ticks and starts the driver`() {
        val expectedTicksByDistance = mapOf(1 to 1, 5 to 1, 6 to 2, 13 to 3)
        for ((distance, expectedTicks) in expectedTicksByDistance) {
            val group = deliveryGroup(distance = distance)
            val driver = waitingDriver(group)

            tick(driver, group)

            assertEquals(expectedTicks * 2, driver.totalTripTicks, "round trip for $distance km")
            assertEquals(expectedTicks, driver.ticksToDest, "one way for $distance km, no drive yet")
            assertEquals(DriverState.DELIVERING, driver.state)
        }
    }

    @Test
    fun `preparation logs the driver, order, group and one-way tick count`() {
        val order = servedOrder()
        val group = deliveryGroup(id = 7, distance = 13)
        val driver = waitingDriver(group, order, id = 3)

        tick(driver, group)

        val expected = "Delivery Preparation (R 1): Driver 3 prepares driving order ${order.id} to group 7, " +
            "which will take 3 ticks."
        assertTrue(logLines().any { it.contains(expected) })
    }

    @Test
    fun `an idle driver is not started`() {
        val group = deliveryGroup()
        val driver = Driver().apply { id = 1 }

        tick(driver, group)

        assertEquals(DriverState.IDLE, driver.state)
        assertTrue(logLines().isEmpty())
    }

    @Test
    fun `a driver prepared this tick only starts driving next tick`() {
        val group = deliveryGroup(distance = 13)
        val driver = waitingDriver(group)

        tick(driver, group)

        assertEquals(3, driver.ticksToDest, "no tick of driving consumed yet")
        assertFalse(logLines().any { it.contains("Delivery Driving") })
    }

    // ---- Driving ----

    @Test
    fun `each driving tick logs the remaining ticks counting down to zero`() {
        val group = deliveryGroup(distance = 13)
        val driver = drivingDriver(group, servedOrder(), oneWayTicks = 3)

        repeat(3) { tick(driver, group) }

        val remaining = logLines().filter { it.contains("Delivery Driving") }
            .map { it.substringAfter("needs ").substringBefore(" more") }
        assertEquals(listOf("2", "1", "0"), remaining)
    }

    @Test
    fun `the driving log reports cumulative distance capped at the total`() {
        val group = deliveryGroup(distance = 13)
        val driver = drivingDriver(group, servedOrder(), oneWayTicks = 3)

        repeat(3) { tick(driver, group) }

        val distances = logLines().filter { it.contains("Delivery Driving") }
            .map { it.substringAfter("drove ").substringBefore(" km") }
        assertEquals(listOf("5", "10", "13"), distances)
    }

    @Test
    fun `a nine kilometre trip logs five then nine kilometres`() {
        val group = deliveryGroup(distance = 9)
        val driver = drivingDriver(group, servedOrder(), oneWayTicks = 2)

        repeat(2) { tick(driver, group) }

        val distances = logLines().filter { it.contains("Delivery Driving") }
            .map { it.substringAfter("drove ").substringBefore(" km") }
        assertEquals(listOf("5", "9"), distances)
    }

    @Test
    fun `a one tick trip arrives immediately after a single drive`() {
        val group = deliveryGroup(distance = 3)
        val driver = drivingDriver(group, servedOrder(), oneWayTicks = 1)

        tick(driver, group)

        assertTrue(logLines().any { it.contains("Delivery Arrival (R 1): Driver 1 arrived at group 1") })
        assertEquals(DriverState.RETURNING, driver.state)
    }

    @Test
    fun `the driver does not arrive before the last driving tick`() {
        val group = deliveryGroup(distance = 13)
        val driver = drivingDriver(group, servedOrder(), oneWayTicks = 3)

        repeat(2) { tick(driver, group) }

        assertFalse(logLines().any { it.contains("Delivery Arrival") })
        assertEquals(DriverState.DELIVERING, driver.state)
    }

    // ---- Hand-over and the customer's experience ----

    @Test
    fun `a successful hand-over logs finished, records the delivery tick and clears no dishes`() {
        val order = servedOrder()
        val group = deliveryGroup(visitingAt = 10)
        val driver = drivingDriver(group, order, oneWayTicks = 1)
        Time.tick = 8 // tick() advances to 9

        tick(driver, group)

        val finished = "Delivery Finished (R 1): Driver 1 gave delivery of order ${order.id}"
        assertTrue(logLines().any { it.contains(finished) })
        assertEquals(9, order.deliveredAt)
        assertTrue(order.dishes.none { it.status == DishStatus.ABORTED })
    }

    @Test
    fun `delivery before, at and after the visiting tick is a positive, neutral and negative experience`() {
        val expectations = mapOf(
            9 to ExperienceType.POSITIVE,
            10 to ExperienceType.NEUTRAL,
            11 to ExperienceType.NEGATIVE
        )
        for ((arrivalTick, expected) in expectations) {
            val group = deliveryGroup(visitingAt = 10)
            val driver = drivingDriver(group, servedOrder(), oneWayTicks = 1)
            Time.tick = arrivalTick - 1

            tick(driver, group)

            assertEquals(expected, group.experience, "arriving at tick $arrivalTick")
        }
    }

    @Test
    fun `arriving with an already aborted order logs failed and records no delivery`() {
        val order = servedOrder().apply { dishes.first().status = DishStatus.ABORTED }
        val group = deliveryGroup()
        val driver = drivingDriver(group, order, oneWayTicks = 1)

        tick(driver, group)

        val failed = "Delivery Failed (R 1): Driver 1 failed to deliver order ${order.id}"
        assertTrue(logLines().any { it.contains(failed) })
        assertFalse(logLines().any { it.contains("Delivery Finished") })
        assertNull(order.deliveredAt)
    }

    // ---- Customer gives up waiting ----

    @Test
    fun `a driver keeps driving after the customer gave up and fails on arrival`() {
        val order = servedOrder()
        val group = deliveryGroup(distance = 13, visitingAt = 7)
        val driver = drivingDriver(group, order, oneWayTicks = 3)
        Time.tick = 9 // tick() advances to 10 = visitingTick + 3, the tick the group gives up

        tick(driver, group)

        assertEquals(DriverState.DELIVERING, driver.state, "still on the way to the customer")
        assertTrue(logLines().any { it.contains("Delivery Given Up (R 1): Group 1 gave up on waiting") })

        repeat(2) { tick(driver, group) }

        assertTrue(logLines().any { it.contains("Delivery Failed (R 1): Driver 1 failed to deliver") })
        assertEquals(DriverState.RETURNING, driver.state)
    }

    /**
     * Giving up does not abort the meals: the order is only flagged, so a driver already carrying it
     * still drives it out and fails at the door (GivenUpDeliveryIsStillDrivenOutAndFailsSystemTest,
     * confirmed against the reference implementation).
     */
    @Test
    fun `a customer who gave up gets a negative experience and their order is flagged as given up`() {
        val order = servedOrder()
        val group = deliveryGroup(distance = 13, visitingAt = 7)
        val driver = drivingDriver(group, order, oneWayTicks = 3)
        Time.tick = 9

        tick(driver, group)

        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(order.deliveryGivenUp)
        assertTrue(order.dishes.all { it.status == DishStatus.SERVED }, "the meals stay with the driver")
    }

    @Test
    fun `giving up is logged only once`() {
        val group = deliveryGroup(distance = 13, visitingAt = 7)
        val driver = drivingDriver(group, servedOrder(), oneWayTicks = 3)
        Time.tick = 9

        repeat(3) { tick(driver, group) }

        assertEquals(1, logLines().count { it.contains("Delivery Given Up") })
    }
}
