package loggertests

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.DeliveryLogger
import de.unisaarland.cs.se.selab.loggers.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

class DeliveryLoggerTest {
    private lateinit var output: StringWriter
    private val restaurantId: Id = 1
    private val driverId: Id = 2
    private val orderId: Id = 3
    private val groupId: Id = 4
    private val ticksRequiredToDeliver: Tick = 5
    private val distanceCovered: Int = 6

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = restaurantId
    }

    private val loggedLine: String get() = output.toString().lines().single { it.isNotBlank() }

    @Test
    fun `INFO - Delivery Preparation(driverId, orderId, groupId, ticksRequiredToDeliver)`() {
        DeliveryLogger.logDeliveryPreparation(driverId, orderId, groupId, ticksRequiredToDeliver)
        assertEquals(
            "[INFO] Delivery Preparation (R $restaurantId): Driver $driverId " +
                "prepares driving order $orderId to group $groupId, which will take $ticksRequiredToDeliver ticks.",
            loggedLine
        )
    }

    @Test
    fun `DEBUG - Delivery Driving(driverId, distanceCovered, ticksRequiredToDeliver)`() {
        DeliveryLogger.logDeliveryDriving(driverId, distanceCovered, ticksRequiredToDeliver)
        assertEquals(
            "[DEBUG] Delivery Driving (R $restaurantId): Driver $driverId drove " +
                "$distanceCovered km and needs $ticksRequiredToDeliver more ticks.",
            loggedLine
        )
    }

    @Test
    fun `INFO - Delivery Arrival(driveId, groupId, orderId)`() {
        DeliveryLogger.logDeliveryArrival(driverId, groupId, orderId)
        assertEquals(
            "[INFO] Delivery Arrival (R $restaurantId): Driver $driverId arrived " +
                "at group $groupId with order $orderId.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - Delivery Finished(driverId, orderId, groupId)`() {
        DeliveryLogger.logDeliveryFinished(driverId, orderId, groupId)
        assertEquals(
            "[IMPORTANT] Delivery Finished (R $restaurantId): Driver $driverId " +
                "gave delivery of order $orderId to group $groupId.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - Delivery Failed - driverId, orderId, groupId`() {
        DeliveryLogger.logDeliveryFailed(driverId, orderId, groupId)
        assertEquals(
            "[IMPORTANT] Delivery Failed (R $restaurantId): Driver $driverId " +
                "failed to deliver order $orderId to group $groupId.",
            loggedLine
        )
    }

    @Test
    fun `INFO - Delivery Given Up(groupId, orderId)`() {
        DeliveryLogger.logDeliveryGivenUp(groupId, orderId)
        assertEquals(
            "[INFO] Delivery Given Up (R $restaurantId): Group $groupId gave up on " +
                "waiting for delivery of order $orderId.",
            loggedLine
        )
    }

    @Test
    fun `INFO - Delivery Returned(driverId)`() {
        DeliveryLogger.logDeliveryReturned(driverId)
        assertEquals(
            "[INFO] Delivery Returned (R $restaurantId): Driver $driverId has returned.",
            loggedLine
        )
    }

    @Test
    fun `INFO - Delivery Finished Eating(groupId)`() {
        DeliveryLogger.logDeliveryFinishedEating(groupId)
        assertEquals(
            "[INFO] Delivery Finished Eating (R $restaurantId): Group $groupId has finished eating.",
            loggedLine
        )
    }
}
