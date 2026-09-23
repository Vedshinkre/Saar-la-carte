package waitingforfoodtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.restaurant.helpers.DeliveryProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * F27 unit tests for customers waiting for a delivery: they wait until 3 ticks after the tick they wanted the
 * food, then give up ([DeliveryProcessor]); delivered food is eaten in 2 ticks (EatingProcessor).
 */
class WaitingForDeliveryTest {
    private lateinit var fx: WaitingFixtures

    @BeforeEach
    fun setup() {
        fx = WaitingFixtures()
    }

    private fun givenUpLines() = fx.logLinesContaining("Delivery Given Up")

    private fun delivery(vararg groups: CustomerGroup) =
        DeliveryProcessor(drivers = emptyList(), deliveryGroups = groups.toList())

    @Test
    fun `the group keeps waiting until three ticks after it wanted the food`() {
        val group = fx.deliveryGroup(1, fx.order(DishStatus.COOKING), visitingAt = 10)
        val processor = delivery(group)

        for (tick in 10..12) {
            Time.tick = tick
            processor.processDelivering()
        }

        assertTrue(givenUpLines().isEmpty())
        assertEquals(ExperienceType.NEUTRAL, group.experience)
        assertTrue(group.currentOrder!!.dishes.none { it.status == DishStatus.ABORTED })
    }

    @Disabled("Changed abortion to abandoned")
    @Test
    fun `the group gives up three ticks after it wanted the food`() {
        val order = fx.order(DishStatus.COOKING, DishStatus.UNCOOKED)
        val group = fx.deliveryGroup(1, order, visitingAt = 10)
        val processor = delivery(group)

        Time.tick = 13
        processor.processDelivering()

        assertEquals(
            listOf("[INFO] Delivery Given Up (R 1): Group 1 gave up on waiting for delivery of order ${order.id}."),
            givenUpLines()
        )
        // the kitchen is not told: the meals carry on being cooked and are driven out, only to be
        // refused at the door (GivenUpDeliveryIsStillCookedSystemTest)
        assertTrue(order.deliveryGivenUp)
        assertTrue(order.dishes.none { it.status == DishStatus.ABORTED })
        assertEquals(ExperienceType.NEGATIVE, group.experience)
    }

    @Test
    fun `giving up is logged only once`() {
        val group = fx.deliveryGroup(1, fx.order(DishStatus.COOKING), visitingAt = 10)
        val processor = delivery(group)

        for (tick in 13..18) {
            Time.tick = tick
            processor.processDelivering()
        }

        assertEquals(1, givenUpLines().size)
    }

    @Test
    fun `a delivered order is never given up`() {
        val order = fx.order(DishStatus.SERVED)
        order.deliveredAt = 11
        val group = fx.deliveryGroup(1, order, visitingAt = 10)
        val processor = delivery(group)

        Time.tick = 20
        processor.processDelivering()

        assertTrue(givenUpLines().isEmpty())
        assertEquals(ExperienceType.NEUTRAL, group.experience)
    }

    @Disabled("Changed abortion to abandoned")
    @Test
    fun `giving up leaves every dish of the order as it was`() {
        val order = fx.order(DishStatus.EATEN, DishStatus.COOKING)
        val group = fx.deliveryGroup(1, order, visitingAt = 10)
        val processor = delivery(group)

        Time.tick = 13
        processor.processDelivering()

        assertEquals(DishStatus.EATEN, order.dishes[0].status)
        assertEquals(DishStatus.COOKING, order.dishes[1].status)
    }

    @Test
    fun `groups giving up in the same tick are logged in ascending group id`() {
        val first = fx.deliveryGroup(5, fx.order(DishStatus.COOKING), visitingAt = 10)
        val second = fx.deliveryGroup(2, fx.order(DishStatus.COOKING), visitingAt = 10)
        val processor = delivery(first, second)

        Time.tick = 13
        processor.processDelivering()

        val lines = givenUpLines()
        assertTrue(lines[0].contains("Group 2"), lines.toString())
        assertTrue(lines[1].contains("Group 5"), lines.toString())
    }

    @Test
    fun `each group gives up relative to its own wanted tick`() {
        val early = fx.deliveryGroup(1, fx.order(DishStatus.COOKING), visitingAt = 10)
        val late = fx.deliveryGroup(2, fx.order(DishStatus.COOKING), visitingAt = 12)
        val processor = delivery(early, late)

        Time.tick = 13
        processor.processDelivering()

        assertEquals(1, givenUpLines().size)
        assertEquals(ExperienceType.NEGATIVE, early.experience)
        assertEquals(ExperienceType.NEUTRAL, late.experience)
    }

    @Test
    fun `a group without an order does not give up`() {
        val group = fx.deliveryGroup(1, fx.order(DishStatus.COOKING), visitingAt = 10)
        group.currentOrder = null
        val processor = delivery(group)

        Time.tick = 20
        processor.processDelivering()

        assertTrue(givenUpLines().isEmpty())
    }

    @Disabled("Changed abortion to abandoned")
    @Test
    fun `a given up order is never eaten afterwards`() {
        val order = fx.order(DishStatus.COOKING, DishStatus.COOKING)
        val group = fx.deliveryGroup(1, order, visitingAt = 10)
        val delivered = mutableListOf<Int>()
        val giveUp = delivery(group)
        val eating = fx.eating(inHouse = emptyList(), deliveryGroups = listOf(group), delivered = delivered)

        Time.tick = 13
        giveUp.processDelivering()
        repeat(4) {
            eating.processEating()
            Time.tick += 1
        }

        assertTrue(delivered.isEmpty())
        assertTrue(fx.logLinesContaining("Delivery Finished Eating").isEmpty())
        // the meals are still cooking for a delivery that will be refused on arrival
        assertTrue(order.dishes.all { it.status == DishStatus.COOKING })
    }

    @Test
    fun `a delivery order is not affected by the in-restaurant waiting limit`() {
        val order = fx.order(DishStatus.COOKING)
        val group = fx.deliveryGroup(1, order, visitingAt = 30)
        val processor = fx.eating(inHouse = emptyList(), deliveryGroups = listOf(group))

        fx.atTicksSinceOrder(20)
        processor.processEating()

        assertTrue(fx.noEatingLines().isEmpty())
        assertEquals(DishStatus.COOKING, order.dishes.single().status)
    }
}
