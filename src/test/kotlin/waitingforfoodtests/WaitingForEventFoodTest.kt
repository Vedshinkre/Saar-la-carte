package waitingforfoodtests

import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * F27 unit tests for [de.unisaarland.cs.se.selab.restaurant.helpers.EatingProcessor], covering EVENT
 * groups - [WaitingForFoodTest] and [WaitingForFoodIntegrationTest] never run one through the eating
 * phase, even though the same wait/eating rules apply to them too.
 */
class WaitingForEventFoodTest {
    private lateinit var fx: WaitingFixtures

    @BeforeEach
    fun setup() {
        fx = WaitingFixtures()
    }

    // ---- the wait limits apply to EVENT groups too

    @Test
    fun `an EVENT group that receives no food at all leaves four ticks after ordering`() {
        val order = fx.order(DishStatus.UNCOOKED, DishStatus.COOKING)
        val group = fx.event(1, order)
        val processor = fx.eating(listOf(group))

        for (ticks in 0..3) {
            fx.atTicksSinceOrder(ticks)
            processor.processEating()
        }
        assertTrue(fx.noEatingLines().isEmpty(), "must not leave before four ticks after ordering")

        fx.atTicksSinceOrder(4)
        processor.processEating()

        assertEquals(
            listOf("[INFO] Restaurant No Eating (R 1): 2 customers of group 1 leave table 1 due to not being served."),
            fx.noEatingLines()
        )
        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(2, group.getCustomersWhoLeft())
        assertEquals(ExperienceType.NEGATIVE, group.experience)
    }

    @Test
    fun `an EVENT group where somebody was already served waits until six ticks after ordering`() {
        val order = fx.order(DishStatus.SERVED, DishStatus.UNCOOKED, DishStatus.COOKING)
        val group = fx.event(1, order)
        val processor = fx.eating(listOf(group))

        for (ticks in 4..5) {
            fx.atTicksSinceOrder(ticks)
            processor.processEating()
            assertTrue(fx.noEatingLines().isEmpty(), "still waiting $ticks ticks after ordering")
        }

        fx.atTicksSinceOrder(6)
        processor.processEating()

        assertEquals(
            listOf("[INFO] Restaurant No Eating (R 1): 2 customers of group 1 leave table 1 due to not being served."),
            fx.noEatingLines()
        )
        assertEquals(1, group.customersRemainingInRestaurant, "the already-served customer stays")
        assertEquals(ExperienceType.NEGATIVE, group.experience)
    }

    // the wait counts until the meal is SERVED, not until it is cooked (specification page 21)
    @Test
    fun `a cooked but not yet served dish for an EVENT group still counts as unserved`() {
        val order = fx.order(DishStatus.COOKED)
        val group = fx.event(1, order)
        val processor = fx.eating(listOf(group))

        fx.atTicksSinceOrder(4)
        processor.processEating()

        assertEquals(1, fx.noEatingLines().size, "cooked but never served food must not keep customers waiting")
        assertEquals(DishStatus.COOKED, order.dishes.single().status, "the kitchen carries on cooking it")
        assertTrue(order.dishes.single().abandoned)
    }

    @Test
    fun `an EVENT group partially served - the unserved customers leave while the served ones keep eating`() {
        val order = fx.order(DishStatus.SERVED, DishStatus.UNCOOKED)
        val group = fx.event(1, order, size = 2)
        val processor = fx.eating(listOf(group))

        fx.atTicksSinceOrder(7)
        processor.processEating()

        assertEquals(
            listOf("[INFO] Restaurant No Eating (R 1): 1 customers of group 1 leave table 1 due to not being served."),
            fx.noEatingLines()
        )
        assertEquals(1, group.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertEquals(DishStatus.SERVED, order.dishes[0].status, "the customer who was served is unaffected")
    }

    // ---- experience once everything is served

    @Test
    fun `an EVENT group served within the four tick expectation window gets a positive experience`() {
        val group = fx.event(1, fx.order(DishStatus.SERVED, DishStatus.SERVED))
        val processor = fx.eating(listOf(group))

        fx.atTicksSinceOrder(3)
        processor.processEating()

        assertEquals(ExperienceType.POSITIVE, group.experience)
    }

    // "before the 4 tick expectation window is over": exactly 4 ticks after ordering is only neutral
    @Test
    fun `an EVENT group served exactly four ticks after ordering is only a neutral experience`() {
        val group = fx.event(1, fx.order(DishStatus.SERVED, DishStatus.SERVED))
        val processor = fx.eating(listOf(group))

        fx.atTicksSinceOrder(4)
        processor.processEating()

        assertEquals(ExperienceType.NEUTRAL, group.experience)
    }

    // ---- eating

    @Test
    fun `an EVENT group's served customers take two full ticks to eat and finish in the third`() {
        val order = fx.order(DishStatus.SERVED, DishStatus.SERVED)
        val group = fx.event(1, order)
        val processor = fx.eating(listOf(group))

        processor.processEating()
        fx.advance()
        processor.processEating()
        assertTrue(fx.finishedEatingLines().isEmpty(), "still eating after two ticks")
        assertTrue(order.dishes.none { it.status == DishStatus.EATEN })

        fx.advance()
        processor.processEating()

        assertEquals(
            listOf("[INFO] FOH Finished Eating (R 1): 2 customers of group 1 have finished eating at table 1."),
            fx.finishedEatingLines()
        )
        assertTrue(order.dishes.all { it.status == DishStatus.EATEN })
    }

    // ---- ordering of the logs: REGULAR, then EVENT, then CASUAL, then id (spec p.36-37)

    @Test
    fun `groups leaving in the same tick are logged REGULAR before EVENT before CASUAL, then by id`() {
        val casual = fx.casual(1, fx.order(DishStatus.UNCOOKED))
        val event = fx.event(5, fx.order(DishStatus.UNCOOKED, DishStatus.UNCOOKED))
        val regular = fx.regular(9, fx.order(DishStatus.UNCOOKED))
        // shuffled construction order too, so a pass here cannot be an accident of input order
        val processor = fx.eating(listOf(casual, regular, event))

        fx.atTicksSinceOrder(5)
        processor.processEating()

        assertEquals(
            listOf(
                "[INFO] Restaurant No Eating (R 1): 1 customers of group 9 leave table 9 due to not being served.",
                "[INFO] Restaurant No Eating (R 1): 2 customers of group 5 leave table 5 due to not being served.",
                "[INFO] Restaurant No Eating (R 1): 1 customers of group 1 leave table 1 due to not being served."
            ),
            fx.noEatingLines()
        )
    }

    @Test
    fun `finished eating in the same tick is logged REGULAR before EVENT before CASUAL, then by id`() {
        val casual = fx.casual(1, fx.order(DishStatus.SERVED))
        val event = fx.event(5, fx.order(DishStatus.SERVED))
        val regular = fx.regular(9, fx.order(DishStatus.SERVED))
        val processor = fx.eating(listOf(event, casual, regular))

        repeat(3) {
            processor.processEating()
            fx.advance()
        }

        val lines = fx.finishedEatingLines()
        assertEquals(3, lines.size, lines.toString())
        assertTrue(lines[0].contains("group 9"), lines.toString())
        assertTrue(lines[1].contains("group 5"), lines.toString())
        assertTrue(lines[2].contains("group 1"), lines.toString())
    }
}
