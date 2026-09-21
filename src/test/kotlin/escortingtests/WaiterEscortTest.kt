package escortingtests

import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** A waiter escorts at most 10 customers per tick, so big groups leave over several ticks (spec: "partially"). */
class WaiterEscortTest {
    private fun group(size: Int) = CasualGroup(
        id = 1,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    @Test
    fun `a waiter escorts a whole small group at once and frees their load`() {
        val waiter = Waiter().also { it.addToCurrentLoad(6) }
        val group = group(6)

        waiter.escort(group)

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(0, waiter.currentLoad)
        assertEquals(6, waiter.getTickLoad(ActionType.ESCORT))
    }

    @Test
    fun `a waiter escorts at most ten customers per tick and the rest follows in the next tick`() {
        val waiter = Waiter().also { it.addToCurrentLoad(12) }
        val group = group(12)

        waiter.escort(group)
        assertEquals(2, group.customersRemainingInRestaurant)
        assertEquals(2, waiter.currentLoad)

        waiter.resetActionLoads()
        waiter.escort(group)
        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(0, waiter.currentLoad)
    }

    @Test
    fun `a waiter at the escort limit escorts nobody`() {
        val waiter = Waiter().also { it.addToCurrentLoad(12) }
        waiter.addToTickLoad(ActionType.ESCORT, 10)
        val group = group(12)

        waiter.escort(group)

        assertEquals(12, group.customersRemainingInRestaurant)
        assertEquals(12, waiter.currentLoad)
        assertEquals(10, waiter.getTickLoad(ActionType.ESCORT))
    }

    @Test
    fun `a group without customers left is not escorted`() {
        val waiter = Waiter()
        val group = group(4).also { it.customersRemainingInRestaurant = 0 }

        waiter.escort(group)

        assertEquals(0, waiter.getTickLoad(ActionType.ESCORT))
    }
}
