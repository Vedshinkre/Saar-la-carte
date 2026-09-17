package casualcustomerdeliverytests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CasualCustomerDelivery {

    /**
     * Helper function to quickly instantiate a CasualGroup for testing
     */
    private fun createGroup(
        visitingAt: Int,
        deliveryDistance: Int,
        ratingLikelihood: RatingLikelihood
    ): CasualGroup {
        return CasualGroup(
            id = 1,
            size = 2,
            tableType = TableType.COMMON,
            visitingAt = visitingAt,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = deliveryDistance,
            ratingLikelihood = ratingLikelihood
        )
    }

    // determineRating()

    @Test
    fun `determineRating-NEVER rating likelihood -always returns NO_RATING`() {
        //  visitingAt = 10. deliveryDistance = 10 (2 ticks travel).
        // Ordering Tick = 10 - 3 (cooking) - 2 (travel) = Tick 5.
        val group = createGroup(10, 10, RatingLikelihood.NEVER)

        //  driver arriving at Tick 9 (Early)
        group.experience = ExperienceType.POSITIVE
        assertEquals(RatingType.NO_RATING, group.determineRating())

        //  driver arriving at Tick 10 (On Time)
        group.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.NO_RATING, group.determineRating())

        //  driver arriving at Tick 11 (Late)
        group.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NO_RATING, group.determineRating())
    }

    @Test
    fun `determineRating-SOME rating likelihood-returns correct ratings`() {
        // Math: visitingAt = 15. deliveryDistance = 15 (3 ticks travel).
        // Ordering Tick = 15 - 3 (cooking) - 3 (travel) = Tick 9.
        val group = createGroup(15, 15, RatingLikelihood.SOME)

        // driver arriving at Tick 14 (Early)
        group.experience = ExperienceType.POSITIVE
        assertEquals(RatingType.POSITIVE, group.determineRating())

        // driver arriving at Tick 15 (On Time)
        group.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.NO_RATING, group.determineRating())

        // driver arriving at Tick 16 (Late)
        group.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NEGATIVE, group.determineRating())
    }

    @Test
    fun `determineRating-ALWAYS rating likelihood-returns correct ratings`() {
        // Math: visitingAt = 12. deliveryDistance = 7 (ceil(7/5) = 2 ticks travel).
        // Ordering Tick = 12 - 3 (cooking) - 2 (travel) = Tick 7.
        val group = createGroup(12, 7, RatingLikelihood.ALWAYS)

        // driver arriving at Tick 11 (Early)
        group.experience = ExperienceType.POSITIVE
        assertEquals(RatingType.POSITIVE, group.determineRating())

        // driver arriving at Tick 12 (On Time)
        group.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.POSITIVE, group.determineRating())

        // driver arriving at Tick 13 (Late)
        group.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NEGATIVE, group.determineRating())
    }

    // --- Tests for isVisitingThisTick() and getDeliveryOrderTick() ---

    @Test
    fun `isVisitingThisTick-no delivery uses-visitingAt directly`() {
        val group = createGroup(visitingAt = 10, deliveryDistance = 0, RatingLikelihood.NEVER)

        assertFalse(group.wantsDelivery)

        Time.tick = 9
        assertFalse(group.isVisitingThisTick())

        Time.tick = 10
        assertTrue(group.isVisitingThisTick())
    }

    @Test
    fun `isVisitingThisTick- delivery distance 5 uses-correct ordering tick`() {
        // visitingAt = 12
        // delivery = 5. ceil(5/5) = 1.
        // orderingTick = 12 - 3 (cooking) - 1 (travel) = 8
        val group = createGroup(visitingAt = 12, deliveryDistance = 5, RatingLikelihood.NEVER)

        assertTrue(group.wantsDelivery)

        Time.tick = 7
        assertFalse(group.isVisitingThisTick())

        Time.tick = 8
        assertTrue(group.isVisitingThisTick())
    }

    @Test
    fun `isVisitingThisTick- delivery distance 7-test private ceil function`() {
        // visitingAt = 15
        // delivery = 7. ceil(7/5) = 2.
        // orderingTick = 15 - 3 (cooking) - 2 (travel) = 10
        val group = createGroup(visitingAt = 15, deliveryDistance = 7, RatingLikelihood.NEVER)

        Time.tick = 9
        assertFalse(group.isVisitingThisTick())

        Time.tick = 10
        assertTrue(group.isVisitingThisTick())
    }
}
