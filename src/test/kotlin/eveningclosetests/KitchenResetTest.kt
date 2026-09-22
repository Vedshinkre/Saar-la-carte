package eveningclosetests

import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.restaurant.Kitchen
import de.unisaarland.cs.se.selab.restaurant.Pantry
import eveningclosetests.EveningCloseFixtures.cook
import eveningclosetests.EveningCloseFixtures.slowRecipe
import eveningclosetests.EveningCloseFixtures.stock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * F30 unit tests for Kitchen.resetKitchen
 */
class KitchenResetTest {

    @Test
    fun `resetKitchen aborts every dish still in the order queue and clears the cooks`() {
        val order = Order(listOf(Dish(slowRecipe)))
        val cook = cook().apply {
            id = 5
            orderId = order.id
            currentRecipe = slowRecipe
            isCooking = true
        }
        val kitchen = Kitchen(listOf(cook), Pantry(stock()), mutableListOf(order), RestaurantType.EUROPEAN)

        kitchen.resetKitchen()

        assertEquals(DishStatus.ABORTED, order.dishes.single().status)
        assertNull(cook.id)
        assertNull(cook.orderId)
        assertNull(cook.currentRecipe)
        assertFalse(cook.isCooking)
    }

    @Test
    fun `cook id counter restarts at 1 after a reset`() {
        val kitchen = Kitchen(listOf(cook()), Pantry(stock()), mutableListOf(), RestaurantType.EUROPEAN)
        kitchen.getNextCookId()
        kitchen.getNextCookId()

        kitchen.resetKitchen()

        assertEquals(1, kitchen.getNextCookId())
    }
}
