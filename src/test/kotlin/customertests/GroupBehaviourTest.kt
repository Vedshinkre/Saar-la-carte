package customertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Recipe
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Behaviour that every group type shares: the rating of an experience, and when a delivery group orders. */
class GroupBehaviourTest {
    private fun regular() = RegularGroup(1, 2, TableType.COMMON, 4, emptyList(), 1, 1, 1)

    private fun event() = EventGroup(
        id = 1,
        size = 4,
        tableType = TableType.COMMON,
        visitingAt = 4,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = 4,
        eventDishes = emptyMap()
    )

    private fun casual(
        likelihood: RatingLikelihood = RatingLikelihood.ALWAYS,
        distance: Int = 0,
        visitingAt: Int = 20
    ) = CasualGroup(
        id = 1,
        size = 2,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = distance,
        ratingLikelihood = likelihood
    )

    // ---- rating of an experience

    @Test
    fun `a REGULAR group rates negative after a negative experience`() {
        val group = regular().also { it.experience = ExperienceType.NEGATIVE }

        assertEquals(RatingType.NEGATIVE, group.determineRating())
    }

    @Test
    fun `a REGULAR group rates positive after a neutral experience`() {
        val group = regular().also { it.experience = ExperienceType.NEUTRAL }

        assertEquals(RatingType.POSITIVE, group.determineRating())
    }

    @Test
    fun `a REGULAR group rates positive after a positive experience`() {
        val group = regular().also { it.experience = ExperienceType.POSITIVE }

        assertEquals(RatingType.POSITIVE, group.determineRating())
    }

    @Test
    fun `an EVENT group always rates and a neutral experience is positive`() {
        val group = event()

        group.experience = ExperienceType.NEUTRAL
        assertEquals(RatingType.POSITIVE, group.determineRating())
        group.experience = ExperienceType.NEGATIVE
        assertEquals(RatingType.NEGATIVE, group.determineRating())
    }

    @Test
    fun `a CASUAL group rates according to its rating likelihood`() {
        val expected = mapOf(
            RatingLikelihood.NEVER to listOf(RatingType.NO_RATING, RatingType.NO_RATING, RatingType.NO_RATING),
            RatingLikelihood.SOME to listOf(RatingType.NEGATIVE, RatingType.NO_RATING, RatingType.POSITIVE),
            RatingLikelihood.ALWAYS to listOf(RatingType.NEGATIVE, RatingType.POSITIVE, RatingType.POSITIVE)
        )
        val experiences = listOf(ExperienceType.NEGATIVE, ExperienceType.NEUTRAL, ExperienceType.POSITIVE)

        for ((likelihood, ratings) in expected) {
            val actual = experiences.map { experience ->
                casual(likelihood).also { it.experience = experience }.determineRating()
            }
            assertEquals(ratings, actual, "$likelihood")
        }
    }

    // ---- when a delivery group orders

    private fun orderTick(distance: Int, visitingAt: Int): Int? {
        val group = casual(distance = distance, visitingAt = visitingAt)
        return (1..24).firstOrNull { tick ->
            Time.tick = tick
            group.isVisitingThisTick()
        }
    }

    @Test
    fun `a delivery group orders the driving ticks plus three cooking ticks before its visiting tick`() {
        assertEquals(20 - 1 - 3, orderTick(distance = 5, visitingAt = 20))
        assertEquals(20 - 2 - 3, orderTick(distance = 10, visitingAt = 20))
    }

    @Test
    fun `the driving ticks of a delivery are rounded up`() {
        assertEquals(20 - 1 - 3, orderTick(distance = 1, visitingAt = 20))
        assertEquals(20 - 2 - 3, orderTick(distance = 6, visitingAt = 20))
        assertEquals(20 - 3 - 3, orderTick(distance = 11, visitingAt = 20))
    }

    @Test
    fun `a group without delivery distance arrives at its visiting tick`() {
        val group = casual(distance = 0, visitingAt = 7)

        Time.tick = 6
        assertFalse(group.isVisitingThisTick())
        Time.tick = 7
        assertTrue(group.isVisitingThisTick())
    }

    // ---- event favourite dish

    @Test
    fun `an event favourite of No Dish is never treated as a dish name`() {
        val recipe = Recipe(1, "No Dish", 10, listOf(CookType.EXEC), mutableMapOf(), null)
        val preference = FoodPreference(emptyList(), emptyList(), emptyList())

        val dish = preference.decideDish(listOf(recipe), "No Dish", RestaurantType.EUROPEAN)

        assertEquals("No Dish", dish!!.recipe.name, "chosen as the highest id, not as the event favourite")
    }
}
