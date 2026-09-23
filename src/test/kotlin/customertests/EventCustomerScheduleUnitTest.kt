package customertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * unit tests for P04 (Customer - Events) for when an EVENT group reserves and arrives,
 * and which dish it recommends once a restaurant has been chosen
 * ordering tested by Deniz
 */
class EventCustomerScheduleUnitTest {

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
    }

    private fun event(
        eventEvening: Int = 7,
        visitingAt: Int = 5,
        dishes: Map<RestaurantType, String> = mapOf(RestaurantType.EUROPEAN to "soup")
    ) = EventGroup(
        id = 1,
        size = 4,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = List(4) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = dishes.keys.toList(),
        eventEvening = eventEvening,
        eventDishes = dishes
    )

    // booking three evenings ahead

    /** the reservation is made three evenings before the event, so the restaurant can prepare */
    @Test
    fun `an event group books exactly three evenings before its event`() {
        val group = event(eventEvening = 7)

        val bookingEvenings = (1..10).filter { evening ->
            Time.evening = evening
            group.visitingInThreeEvenings()
        }

        assertEquals(listOf(4), bookingEvenings)
    }

    @Test
    fun `an event group does not book on the evening of the event itself`() {
        val group = event(eventEvening = 7)

        Time.evening = 7
        assertFalse(group.visitingInThreeEvenings())
    }

    // turning up

    @Test
    fun `an event group only visits on its event evening`() {
        val group = event(eventEvening = 7)

        val visitingEvenings = (1..10).filter { evening ->
            Time.evening = evening
            group.isVisitingTonight()
        }

        assertEquals(listOf(7), visitingEvenings)
    }

    @Test
    fun `an event group arrives on exactly one tick of that evening`() {
        val group = event(visitingAt = 5)

        val arrivalTicks = (1..24).filter { tick ->
            Time.tick = tick
            group.isVisitingThisTick()
        }

        assertEquals(listOf(5), arrivalTicks)
    }

    /** booking and arriving are independent: the booking evening does not make the group arrive */
    @Test
    fun `the booking evening is not a visiting evening`() {
        val group = event(eventEvening = 7)

        Time.evening = 4
        assertTrue(group.visitingInThreeEvenings())
        assertFalse(group.isVisitingTonight())
    }

    // the dish the event recommends

    @Test
    fun `there is no favourite dish before a restaurant has been chosen`() {
        val group = event()

        assertNull(group.getCurrentEventDish())
    }

    @Test
    fun `the favourite dish follows the chosen restaurant type`() {
        val group = event(
            dishes = mapOf(RestaurantType.EUROPEAN to "soup", RestaurantType.ASIAN to "noodles")
        )

        group.currentRestaurantType = RestaurantType.ASIAN
        assertEquals("noodles", group.getCurrentEventDish())

        group.currentRestaurantType = RestaurantType.EUROPEAN
        assertEquals("soup", group.getCurrentEventDish())
    }

    /** a type the event never named has no favourite, even once it has been set */
    @Test
    fun `a restaurant type the event has no dish for has no favourite`() {
        val group = event(dishes = mapOf(RestaurantType.EUROPEAN to "soup"))

        group.currentRestaurantType = RestaurantType.AFRICAN

        assertNull(group.getCurrentEventDish())
    }

    /** the event evening and the dish per restaurant type are what the configuration gave it */
    @Test
    fun `an event group keeps the booking details it was configured with`() {
        val dishes = mapOf(RestaurantType.EUROPEAN to "soup", RestaurantType.ASIAN to "noodles")
        val group = event(eventEvening = 9, dishes = dishes)

        assertEquals(9, group.eventEvening)
        assertEquals(dishes, group.eventDishes)
        assertEquals(listOf(RestaurantType.EUROPEAN, RestaurantType.ASIAN), group.restaurantTypes)
        assertNull(group.currentRestaurantType, "no restaurant has been chosen yet")
    }

    // a fresh start for the evening

    /** an event group that was turned down once starts the next attempt with all customers */
    @Test
    fun `resetting for a new evening restores the whole group`() {
        val group = event()
        group.customersRemainingInRestaurant = 0

        group.resetForNewEvening()

        assertEquals(4, group.customersRemainingInRestaurant)
        assertNull(group.currentOrder)
    }
}
