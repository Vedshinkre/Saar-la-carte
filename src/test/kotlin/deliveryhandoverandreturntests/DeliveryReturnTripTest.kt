package deliveryhandoverandreturntests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.restaurant.helpers.DeliveryProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * F20 return trip and log order: the driver drives back the same number of ticks, whatever the
 * outcome, logs the return once, and every log type is written for all drivers before the next type.
 * Outbound driving itself is in `deliveryoutboundtests`.
 */
class DeliveryReturnTripTest {
    private lateinit var fx: DeliveryFixtures

    @BeforeEach
    fun setup() {
        fx = DeliveryFixtures()
    }

    /** a driver that holds the fully served order of [group] and is about to prepare, as after the hand-over */
    private fun waitingDriver(id: Int, group: CasualGroup): Driver {
        group.currentOrder!!.dishes.forEach { it.status = DishStatus.SERVED }
        return Driver().apply {
            this.id = id
            targetGroup = group
            currentOrder = group.currentOrder
            state = DriverState.WAITING
        }
    }

    private fun tick(processor: DeliveryProcessor, count: Int = 1) = repeat(count) {
        processor.processDelivering()
        Time.tick += 1
    }

    @Test
    fun `after the hand-over the driver returns in as many ticks as the way out took`() {
        val group = fx.deliveryGroup(1, distance = 10) // 2 ticks each way
        val driver = waitingDriver(1, group)
        val processor = DeliveryProcessor(listOf(driver), listOf(group))

        tick(processor, 3) // prepare, drive, drive + arrive + hand over
        assertEquals(DriverState.RETURNING, driver.state)
        assertTrue(fx.logLinesContaining("Delivery Returned").isEmpty())

        tick(processor) // first tick back
        assertEquals(DriverState.RETURNING, driver.state)
        assertTrue(fx.logLinesContaining("Delivery Returned").isEmpty(), "one tick back is still missing")

        tick(processor) // second tick back
        assertEquals(DriverState.IDLE, driver.state)
        assertEquals(
            listOf("[INFO] Delivery Returned (R 1): Driver 1 has returned."),
            fx.logLinesContaining("Delivery Returned")
        )
    }

    @Test
    fun `a five kilometre delivery returns one tick after the hand-over`() {
        val group = fx.deliveryGroup(1, distance = 5)
        val driver = waitingDriver(1, group)
        val processor = DeliveryProcessor(listOf(driver), listOf(group))

        tick(processor, 2) // prepare, drive + arrive + hand over
        assertEquals(DriverState.RETURNING, driver.state)

        tick(processor)
        assertEquals(DriverState.IDLE, driver.state)
        assertEquals(1, fx.logLinesContaining("Delivery Returned").size)
    }

    @Test
    fun `the way back is not logged tick by tick`() {
        val group = fx.deliveryGroup(1, distance = 15) // 3 ticks each way
        val driver = waitingDriver(1, group)
        val processor = DeliveryProcessor(listOf(driver), listOf(group))

        tick(processor, 4) // prepare, 3 driving ticks, arrival and hand over in the last one
        val drivingLogsAfterArrival = fx.logLinesContaining("Delivery Driving").size
        tick(processor, 3)

        assertEquals(3, drivingLogsAfterArrival)
        assertEquals(3, fx.logLinesContaining("Delivery Driving").size)
    }

    @Test
    fun `a failed delivery is driven back over the same distance`() {
        val group = fx.deliveryGroup(1, distance = 10)
        val driver = waitingDriver(1, group)
        val processor = DeliveryProcessor(listOf(driver), listOf(group))
        tick(processor, 2) // prepare and one driving tick
        group.currentOrder!!.dishes.forEach { it.status = DishStatus.ABORTED }

        tick(processor) // arrival, the delivery fails
        assertEquals(1, fx.logLinesContaining("Delivery Failed").size)
        assertEquals(DriverState.RETURNING, driver.state)

        tick(processor, 2)
        assertEquals(DriverState.IDLE, driver.state)
        assertEquals(1, fx.logLinesContaining("Delivery Returned").size)
    }

    @Test
    fun `a returned driver keeps its id and can take the next order`() {
        val group = fx.deliveryGroup(1, distance = 5)
        val driver = waitingDriver(4, group)
        val processor = DeliveryProcessor(listOf(driver), listOf(group))

        tick(processor, 3)

        assertEquals(DriverState.IDLE, driver.state)
        assertEquals(4, driver.id)
        assertNull(driver.currentOrder)
        assertNull(driver.targetGroup)
        assertTrue(processor.isDriverAvailable())
    }

    @Test
    fun `a returning driver is not idle and takes no new order until it is back`() {
        val group = fx.deliveryGroup(1, distance = 10)
        val driver = waitingDriver(1, group)
        val processor = DeliveryProcessor(listOf(driver), listOf(group))

        tick(processor, 3)

        assertEquals(DriverState.RETURNING, driver.state)
        assertTrue(!processor.isDriverAvailable())
    }

    @Test
    fun `drivers that return in the same tick are logged in ascending group id`() {
        val groupFive = fx.deliveryGroup(5, distance = 5)
        val groupTwo = fx.deliveryGroup(2, distance = 5)
        val driverA = waitingDriver(1, groupFive)
        val driverB = waitingDriver(2, groupTwo)
        val processor = DeliveryProcessor(listOf(driverA, driverB), listOf(groupFive, groupTwo))

        tick(processor, 3)

        assertEquals(
            listOf(
                "[INFO] Delivery Returned (R 1): Driver 2 has returned.",
                "[INFO] Delivery Returned (R 1): Driver 1 has returned."
            ),
            fx.logLinesContaining("Delivery Returned")
        )
    }

    @Test
    fun `each delivery log type is written for all drivers before the next type`() {
        val groupFive = fx.deliveryGroup(5, distance = 10)
        val groupTwo = fx.deliveryGroup(2, distance = 10)
        val driverA = waitingDriver(1, groupFive)
        val driverB = waitingDriver(2, groupTwo)
        val processor = DeliveryProcessor(listOf(driverA, driverB), listOf(groupFive, groupTwo))

        tick(processor, 3) // prepare, drive, drive + arrive + hand over

        val types = listOf("Preparation", "Driving", "Arrival", "Finished")
        val logged = fx.logLines()
            .mapNotNull { line -> types.firstOrNull { line.contains("Delivery $it (R 1)") }?.let { it to line } }
        assertEquals(
            listOf(
                "Preparation", "Preparation", "Driving", "Driving", "Driving", "Driving", "Arrival", "Arrival",
                "Finished", "Finished"
            ),
            logged.map { it.first }
        )
        // within a type, ascending group id: group 2 is driven by driver 2
        assertTrue(logged[0].second.contains("Driver 2"))
        assertTrue(logged[1].second.contains("Driver 1"))
        assertTrue(logged[6].second.contains("group 2"))
        assertTrue(logged[7].second.contains("group 5"))
        assertTrue(logged[8].second.contains("group 2"))
        assertTrue(logged[9].second.contains("group 5"))
    }

    @Test
    fun `a return is logged after the preparations of the same tick`() {
        val arrivingGroup = fx.deliveryGroup(3, distance = 10)
        val returningGroup = fx.deliveryGroup(1, distance = 5)
        val returning = waitingDriver(1, returningGroup)
        val processor = DeliveryProcessor(listOf(returning), listOf(returningGroup, arrivingGroup))
        tick(processor, 2) // returning driver: prepared, then arrived and handed over
        val arriving = waitingDriver(2, arrivingGroup)
        val both = DeliveryProcessor(listOf(returning, arriving), listOf(returningGroup, arrivingGroup))
        tick(both) // arriving driver prepares, returning driver drives back

        assertEquals(DriverState.IDLE, returning.state)
        val lines = fx.logLines()
        val preparation = lines.indexOfLast { it.contains("Delivery Preparation") }
        val returned = lines.indexOfLast { it.contains("Delivery Returned") }
        assertTrue(preparation in 0 until returned, "returns are the last delivery step of a tick")
    }
}
