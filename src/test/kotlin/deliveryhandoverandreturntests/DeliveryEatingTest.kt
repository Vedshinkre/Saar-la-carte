package deliveryhandoverandreturntests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.restaurant.helpers.EatingProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** F20 eating: a delivered group eats for 2 ticks after the hand-over and logs "Delivery Finished Eating" once. */
class DeliveryEatingTest {
    private lateinit var fx: DeliveryFixtures

    @BeforeEach
    fun setup() {
        fx = DeliveryFixtures()
    }

    private val delivered = mutableListOf<Int>()

    private fun eating(vararg groups: CasualGroup) = EatingProcessor(
        deliveryGroups = groups.toList(),
        getInHouseGroups = { emptyList() },
        getServingPriority = { 2 },
        getAssignedTableId = { null },
        addCustomersDelivered = { delivered += it },
    )

    /** a group whose served order the driver hands over in the current tick */
    private fun handedOver(id: Int, dishCount: Int = 2): CasualGroup {
        val group = fx.deliveryGroup(id, dishCount, status = DishStatus.SERVED)
        group.currentOrder!!.deliveredAt = Time.tick
        return group
    }

    private fun finishedEatingLines() = fx.logLinesContaining("Delivery Finished Eating")

    @Test
    fun `a served order that was not delivered yet is not eaten`() {
        val group = fx.deliveryGroup(1, status = DishStatus.SERVED) // deliveredAt stays null
        val processor = eating(group)

        repeat(5) {
            processor.processEating()
            Time.tick += 1
        }

        assertTrue(group.currentOrder!!.dishes.all { it.status == DishStatus.SERVED })
        assertTrue(finishedEatingLines().isEmpty())
    }

    @Test
    fun `the group finishes eating two ticks after the hand-over and logs it once`() {
        val group = handedOver(1)
        val processor = eating(group)

        processor.processEating() // hand-over tick
        Time.tick += 1
        processor.processEating()
        assertTrue(finishedEatingLines().isEmpty(), "still eating one tick after the hand-over")
        assertTrue(group.currentOrder!!.dishes.none { it.status == DishStatus.EATEN })

        Time.tick += 1
        processor.processEating()
        assertEquals(
            listOf("[INFO] Delivery Finished Eating (R 1): Group 1 has finished eating."),
            finishedEatingLines()
        )
        assertTrue(group.currentOrder!!.dishes.all { it.status == DishStatus.EATEN })

        repeat(3) {
            Time.tick += 1
            processor.processEating()
        }
        assertEquals(1, finishedEatingLines().size, "logged once per order")
    }

    @Test
    fun `groups finishing in the same tick are logged in ascending group id`() {
        val groupFour = handedOver(4)
        val groupTwo = handedOver(2)
        val processor = eating(groupFour, groupTwo)

        repeat(3) {
            processor.processEating()
            Time.tick += 1
        }

        assertEquals(
            listOf(
                "[INFO] Delivery Finished Eating (R 1): Group 2 has finished eating.",
                "[INFO] Delivery Finished Eating (R 1): Group 4 has finished eating."
            ),
            finishedEatingLines()
        )
    }

    @Test
    fun `delivered customers are counted once in the tick of the hand-over`() {
        val processor = eating(handedOver(1, dishCount = 3))

        repeat(6) {
            processor.processEating()
            Time.tick += 1
        }

        assertEquals(listOf(3), delivered)
    }

    @Test
    fun `groups handed over in different ticks are counted separately`() {
        val first = handedOver(1, dishCount = 2)
        val processor = eating(first)
        processor.processEating()
        Time.tick += 1
        val second = handedOver(2, dishCount = 1)
        val both = eating(first, second)

        repeat(6) {
            both.processEating()
            Time.tick += 1
        }

        assertEquals(listOf(2, 1), delivered)
    }
}
