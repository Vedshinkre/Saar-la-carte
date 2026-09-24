package customertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the visiting schedule of CasualGroups.
 */
class CasualVisitScheduleTest {

    @AfterTest
    fun resetTime() {
        Time.tick = 1
        Time.evening = 1
    }

    private fun casualGroup(
        distance: Int = 0,
        visitingAt: Int = 10,
        visitingEvenings: List<Int> = listOf(1)
    ) = CasualGroup(
        1,
        2,
        TableType.COMMON,
        visitingAt,
        emptyList(),
        listOf(RestaurantType.EUROPEAN),
        visitingEvenings,
        distance,
        RatingLikelihood.ALWAYS
    )

    private fun actingTicks(casualGroup: CasualGroup): List<Int> = (1..24).filter { tick ->
        Time.tick = tick
        casualGroup.isVisitingThisTick()
    }

    @Test
    fun `Started Tick Of Travel Counts As Whole Tick`() {
        val casualGroup = casualGroup()

        with(casualGroup) {
            assertEquals(1, 1.ceilDiv(5))
            assertEquals(2, 6.ceilDiv(5))
            assertEquals(3, 11.ceilDiv(5))
        }
    }

    @Test
    fun `Exact Multiple Is Not Rounded Up`() {
        val casualGroup = casualGroup()

        with(casualGroup) {
            assertEquals(0, 0.ceilDiv(5))
            assertEquals(2, 10.ceilDiv(5))
            assertEquals(4, 20.ceilDiv(5))
        }
    }

    @Test
    fun `Negative Values Are Rounded Towards Zero`() {
        val casualGroup = casualGroup()

        with(casualGroup) {
            assertEquals(-1, (-7).ceilDiv(5))
            assertEquals(-2, (-10).ceilDiv(5))
            assertEquals(0, (-3).ceilDiv(5))
        }
    }

    @Test
    fun `Group Without Delivery Distance Walks In`() {
        assertFalse(casualGroup(distance = 0).wantsDelivery)
    }

    @Test
    fun `Any Delivery Distance Makes Group A Delivery`() {
        assertTrue(casualGroup(distance = 1).wantsDelivery)
        assertTrue(casualGroup(distance = 30).wantsDelivery)
    }

    @Test
    fun `Clearing Delivery Flag Moves Group To Visiting Tick`() {
        val casualGroup = casualGroup(distance = 10, visitingAt = 20)
        assertEquals(listOf(20 - 2 - 3), actingTicks(casualGroup))

        casualGroup.wantsDelivery = false

        assertEquals(listOf(20), actingTicks(casualGroup))
    }

    @Test
    fun `Walk In Group Arrives On Visiting Tick`() {
        assertEquals(listOf(7), actingTicks(casualGroup(distance = 0, visitingAt = 7)))
    }

    @Test
    fun `Delivery Group Orders Cooking And Driving Time Ahead`() {
        assertEquals(listOf(20 - 1 - 3), actingTicks(casualGroup(distance = 5, visitingAt = 20)))
        assertEquals(listOf(20 - 4 - 3), actingTicks(casualGroup(distance = 20, visitingAt = 20)))
    }

    @Test
    fun `Group Further Away Orders Earlier`() {
        val nearTick = actingTicks(casualGroup(distance = 5, visitingAt = 20)).single()
        val farTick = actingTicks(casualGroup(distance = 25, visitingAt = 20)).single()

        assertTrue(farTick < nearTick)
    }

    @Test
    fun `Casual Group Only Visits On Listed Evenings`() {
        val casualGroup = casualGroup(visitingEvenings = listOf(2, 5))

        val visitedEvenings = (1..6).filter { evening ->
            Time.evening = evening
            casualGroup.isVisitingTonight()
        }

        assertEquals(listOf(2, 5), visitedEvenings)
    }

    @Test
    fun `Casual Group Without Visiting Evenings Never Visits`() {
        val casualGroup = casualGroup(visitingEvenings = emptyList())

        Time.evening = 1
        assertFalse(casualGroup.isVisitingTonight())
    }

    @Test
    fun `Casual Group Keeps Configured Schedule And Rating Likelihood`() {
        val casualGroup = casualGroup(distance = 7, visitingAt = 12, visitingEvenings = listOf(2, 5))

        assertEquals(listOf(2, 5), casualGroup.visitingEvenings)
        assertEquals(RatingLikelihood.ALWAYS, casualGroup.ratingLikelihood)
        assertEquals(7, casualGroup.deliveryDistance)
        assertEquals(listOf(RestaurantType.EUROPEAN), casualGroup.restaurantTypes)
    }
}
