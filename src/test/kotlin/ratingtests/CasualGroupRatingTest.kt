package ratingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.RatingProcessor
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

class CasualGroupRatingTest {
    private fun newDummyCasualGroup(ratingLikelihood: RatingLikelihood) = CasualGroup(
        1,
        5,
        TableType.COMMON,
        1,
        emptyList(),
        emptyList(),
        emptyList(),
        0,
        ratingLikelihood
    )

    private fun newEmptyRatingProcessor(): RatingProcessor {
        return RatingProcessor(
            mutableListOf(),
            emptyList(),
            mutableListOf(),
            { emptyList() },
            { 0 },
            { }
        )
    }

    @BeforeEach
    fun setup() {
        Time.tick = 1
        Logger.restaurantID = 1
        Logger.setup(LogLevel.DEBUG)
    }

    @Test
    fun `Always Rate - Success`() {
        val ratingProcessor = newEmptyRatingProcessor()
        val casualGroup: CasualGroup = newDummyCasualGroup(RatingLikelihood.ALWAYS)
        val positiveRatings = 0
        val negativeRatings = 0

        casualGroup.experience = ExperienceType.POSITIVE
        assertEquals(RatingType.POSITIVE, casualGroup.determineRating())
        val ratingsPositive: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings
        )
        assertEquals(1, ratingsPositive.first)
        assertEquals(0, ratingsPositive.second)

        casualGroup.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.POSITIVE, casualGroup.determineRating())
        val ratingsNeutral: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings
        )
        assertEquals(1, ratingsNeutral.first)
        assertEquals(0, ratingsNeutral.second)

        casualGroup.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NEGATIVE, casualGroup.determineRating())
        val ratingsNegative: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings
        )
        assertEquals(0, ratingsNegative.first)
        assertEquals(1, ratingsNegative.second)
    }

    @Test
    fun `Sometimes Rate - Success`() {
        val ratingProcessor = newEmptyRatingProcessor()
        val casualGroup: CasualGroup = newDummyCasualGroup(RatingLikelihood.SOME)
        val positiveRatings = 0
        val negativeRatings = 0

        casualGroup.experience = ExperienceType.POSITIVE
        assertEquals(RatingType.POSITIVE, casualGroup.determineRating())
        val ratingsPositive: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings
        )
        assertEquals(1, ratingsPositive.first)
        assertEquals(0, ratingsPositive.second)

        casualGroup.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.NO_RATING, casualGroup.determineRating())
        val ratingsNeutral: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings
        )
        assertEquals(0, ratingsNeutral.first)
        assertEquals(0, ratingsNeutral.second)

        casualGroup.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NEGATIVE, casualGroup.determineRating())
        val ratingsNegative: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings
        )
        assertEquals(0, ratingsNegative.first)
        assertEquals(1, ratingsNegative.second)
    }

    @Test
    fun `Never Rate - Success`() {
        val ratingProcessor = newEmptyRatingProcessor()
        val casualGroup: CasualGroup = newDummyCasualGroup(RatingLikelihood.NEVER)
        val positiveRatings = 0
        val negativeRatings = 0

        casualGroup.experience = ExperienceType.POSITIVE
        assertEquals(RatingType.NO_RATING, casualGroup.determineRating())
        val ratingsPositive: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings
        )
        assertEquals(0, ratingsPositive.first)
        assertEquals(0, ratingsPositive.second)

        casualGroup.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.NO_RATING, casualGroup.determineRating())
        val ratingsNeutral: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings
        )
        assertEquals(0, ratingsNeutral.first)
        assertEquals(0, ratingsNeutral.second)

        casualGroup.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NO_RATING, casualGroup.determineRating())
        val ratingsNegative: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings
        )
        assertEquals(0, ratingsNegative.first)
        assertEquals(0, ratingsNegative.second)
    }

    @Test
    fun `Always Rate - Closing`() {
        val ratingProcessor = newEmptyRatingProcessor()
        val casualGroup: CasualGroup = newDummyCasualGroup(RatingLikelihood.ALWAYS)
        val positiveRatings = 0
        val negativeRatings = 0

        casualGroup.experience = ExperienceType.POSITIVE
        val ratingsPositive: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings,
            true
        )
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
        assertEquals(0, ratingsPositive.first)
        assertEquals(1, ratingsPositive.second)

        casualGroup.experience = ExperienceType.NEUTRAL
        val ratingsNeutral: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings,
            true
        )
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
        assertEquals(0, ratingsNeutral.first)
        assertEquals(1, ratingsNeutral.second)

        casualGroup.experience = ExperienceType.NEGATIVE
        val ratingsNegative: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings,
            true
        )
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
        assertEquals(0, ratingsNegative.first)
        assertEquals(1, ratingsNegative.second)
    }

    @Test
    fun `Sometimes Rate - Closing`() {
        val ratingProcessor = newEmptyRatingProcessor()
        val casualGroup: CasualGroup = newDummyCasualGroup(RatingLikelihood.SOME)
        val positiveRatings = 0
        val negativeRatings = 0

        casualGroup.experience = ExperienceType.POSITIVE
        val ratingsPositive: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings,
            true
        )
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
        assertEquals(0, ratingsPositive.first)
        assertEquals(1, ratingsPositive.second)

        casualGroup.experience = ExperienceType.NEUTRAL
        val ratingsNeutral: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings,
            true
        )
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
        assertEquals(0, ratingsNeutral.first)
        assertEquals(1, ratingsNeutral.second)

        casualGroup.experience = ExperienceType.NEGATIVE
        val ratingsNegative: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings,
            true
        )
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
        assertEquals(0, ratingsNegative.first)
        assertEquals(1, ratingsNegative.second)
    }

    @Test
    fun `Never Rate - Closing`() {
        val ratingProcessor = newEmptyRatingProcessor()
        val casualGroup: CasualGroup = newDummyCasualGroup(RatingLikelihood.NEVER)
        val positiveRatings = 0
        val negativeRatings = 0

        casualGroup.experience = ExperienceType.POSITIVE
        val ratingsPositive: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings,
            true
        )
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
        assertEquals(0, ratingsPositive.first)
        assertEquals(0, ratingsPositive.second)

        casualGroup.experience = ExperienceType.NEUTRAL
        val ratingsNeutral: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings,
            true
        )
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
        assertEquals(0, ratingsNeutral.first)
        assertEquals(0, ratingsNeutral.second)

        casualGroup.experience = ExperienceType.NEGATIVE
        val ratingsNegative: Pair<Int, Int> = ratingProcessor.rate(
            casualGroup,
            positiveRatings,
            negativeRatings,
            true
        )
        assertEquals(ExperienceType.NEGATIVE, casualGroup.experience)
        assertEquals(0, ratingsNegative.first)
        assertEquals(0, ratingsNegative.second)
    }

    @Test
    fun `Sometimes Rate - Negative Rating - No Waitstaff, Single Customer`() {
        val frontOfHouse = FrontOfHouse(
            listOf(Table(1, 5, TableType.COMMON)),
            emptyList(),
            emptyList(),
            mock()
        )
        val casualGroup = newDummyCasualGroup(RatingLikelihood.SOME)

        assertFalse(frontOfHouse.processArrival(casualGroup, emptyList()))
        Time.incrementTick()
        assertTrue(frontOfHouse.processArrival(casualGroup, emptyList()))
        assertEquals(RatingType.NEGATIVE, casualGroup.determineRating())
        assertEquals(Pair(0, 1), frontOfHouse.processRatings(0, 0))
    }

    @Test
    fun `Sometimes Rate - Negative Rating - No Table, Single Customer`() {
        val frontOfHouse = FrontOfHouse(
            listOf(),
            listOf(Waiter()),
            emptyList(),
            mock()
        )
        val casualGroup = newDummyCasualGroup(RatingLikelihood.SOME)

        assertTrue(frontOfHouse.processArrival(casualGroup, emptyList()))
        assertEquals(RatingType.NEGATIVE, casualGroup.determineRating())
        assertEquals(Pair(0, 1), frontOfHouse.processRatings(0, 0))
    }

    @Test
    fun `Sometimes Rate - Negative Rating - No Table Type, Single Customer`() {
        val frontOfHouse = FrontOfHouse(
            listOf(Table(1, 9, TableType.BAR)),
            listOf(Waiter()),
            emptyList(),
            mock()
        )
        val casualGroup = newDummyCasualGroup(RatingLikelihood.SOME)

        assertTrue(frontOfHouse.processArrival(casualGroup, emptyList()))
        assertEquals(RatingType.NEGATIVE, casualGroup.determineRating())
        assertEquals(Pair(0, 1), frontOfHouse.processRatings(0, 0))
    }

    @Test
    fun `Sometimes Rate - Negative Rating - Table Small, Single Customer`() {
        val frontOfHouse = FrontOfHouse(
            listOf(Table(1, 4, TableType.COMMON)),
            listOf(Waiter()),
            emptyList(),
            mock()
        )
        val casualGroup = newDummyCasualGroup(RatingLikelihood.SOME)

        assertTrue(frontOfHouse.processArrival(casualGroup, emptyList()))
        assertEquals(RatingType.NEGATIVE, casualGroup.determineRating())
        assertEquals(Pair(0, 1), frontOfHouse.processRatings(0, 0))
    }

    @Test
    fun `Sometimes Rate - Negative Rating - Not Three Quarters, Single Customer`() {
        val frontOfHouse = FrontOfHouse(
            listOf(Table(1, 9, TableType.COMMON)),
            listOf(Waiter()),
            emptyList(),
            mock()
        )
        val casualGroup = newDummyCasualGroup(RatingLikelihood.SOME)

        assertTrue(frontOfHouse.processArrival(casualGroup, emptyList()))
        assertEquals(RatingType.NEGATIVE, casualGroup.determineRating())
        assertEquals(Pair(0, 1), frontOfHouse.processRatings(0, 0))
    }

    @Test
    fun `Sometimes Rate - Negative Rating - Table Merge Small, Single Customer`() {
        val frontOfHouse = FrontOfHouse(
            listOf(
                Table(1, 2, TableType.COMMON),
                Table(2, 2, TableType.COMMON)
            ),
            listOf(Waiter()),
            emptyList(),
            mock()
        )
        val casualGroup = newDummyCasualGroup(RatingLikelihood.SOME)

        assertTrue(frontOfHouse.processArrival(casualGroup, emptyList()))
        assertEquals(RatingType.NEGATIVE, casualGroup.determineRating())
        assertEquals(Pair(0, 1), frontOfHouse.processRatings(0, 0))
    }

    @Test
    fun `Sometimes Rate - Negative Rating - No Three Quarters Table Merge, Single Customer`() {
        val frontOfHouse = FrontOfHouse(
            listOf(
                Table(1, 2, TableType.COMMON),
                Table(2, 7, TableType.COMMON)
            ),
            listOf(Waiter()),
            emptyList(),
            mock()
        )
        val casualGroup = newDummyCasualGroup(RatingLikelihood.SOME)

        assertTrue(frontOfHouse.processArrival(casualGroup, emptyList()))
        assertEquals(RatingType.NEGATIVE, casualGroup.determineRating())
        assertEquals(Pair(0, 1), frontOfHouse.processRatings(0, 0))
    }

    @Test
    fun `Sometimes Rate - Negative Rating - No Waitstaff, Multiple Customers`() {
        val frontOfHouse = FrontOfHouse(
            listOf(Table(1, 5, TableType.COMMON)),
            emptyList(),
            emptyList(),
            mock()
        )
        val casualGroupOne = newDummyCasualGroup(RatingLikelihood.SOME)
        val casualGroupTwo = newDummyCasualGroup(RatingLikelihood.SOME)

        assertFalse(frontOfHouse.processArrival(casualGroupOne, emptyList()))
        assertFalse(frontOfHouse.processArrival(casualGroupTwo, emptyList()))
        Time.incrementTick()
        assertTrue(frontOfHouse.processArrival(casualGroupOne, emptyList()))
        assertTrue(frontOfHouse.processArrival(casualGroupTwo, emptyList()))
        assertEquals(Pair(0, 2), frontOfHouse.processRatings(0, 0))
    }
}
