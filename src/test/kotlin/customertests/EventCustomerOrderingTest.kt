package customertests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.Pantry
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the ordering of EventGroups.
 */
class EventCustomerOrderingTest {
    private val flour = Ingredient("flour", MeasurementUnit.G, 5, 1000)
    private val rice = Ingredient("rice", MeasurementUnit.G, 5, 1000)
    private val bread = Recipe(1, "Bread", 10, listOf(CookType.TOURNANT), mutableMapOf(flour to 10), null)
    private val riceDish = Recipe(2, "Rice", 10, listOf(CookType.TOURNANT), mutableMapOf(rice to 10), null)
    private val menu = listOf(bread, riceDish)

    @BeforeTest
    fun setUp() {
        Time.tick = 1
        Time.evening = 1
    }

    private fun countertop(orderQueue: ArrayDeque<Order> = ArrayDeque()): Countertop {
        val pantry = Pantry(
            mutableListOf(IngredientPackage(flour), IngredientPackage(rice)),
            Supplier(Stock(listOf(flour, rice)))
        )
        return Countertop(
            pantry,
            orderQueue,
            listOf(Cook(CookType.TOURNANT)),
            RestaurantType.EUROPEAN
        )
    }

    private fun foodPreference(excluded: List<Ingredient> = emptyList()) =
        FoodPreference(excluded, emptyList(), emptyList())

    private fun eventGroup(
        foodPreferences: List<FoodPreference>,
        eventDish: String = "Bread",
        restaurantType: RestaurantType? = RestaurantType.EUROPEAN
    ) = EventGroup(
        1,
        foodPreferences.size,
        TableType.COMMON,
        1,
        foodPreferences,
        listOf(RestaurantType.EUROPEAN),
        1,
        mapOf(RestaurantType.EUROPEAN to eventDish)
    ).also { it.currentRestaurantType = restaurantType }

    @Test
    fun `Every Customer Orders Event Dish`() {
        val eventGroup = eventGroup(List(3) { foodPreference() })
        val orderQueue = ArrayDeque<Order>()

        val ordered = eventGroup.placeOrder(mutableMapOf(Waiter() to 3), menu, countertop(orderQueue))

        assertTrue(ordered)
        assertEquals(mapOf("Bread" to 3), eventGroup.currentOrder?.dishNameToAmount())
        assertEquals(1, orderQueue.size)
    }

    @Test
    fun `No Chosen Restaurant Type Means No Event Dish`() {
        val eventGroup = eventGroup(List(2) { foodPreference() }, restaurantType = null)

        val ordered = eventGroup.placeOrder(mutableMapOf(Waiter() to 2), menu, countertop())

        assertTrue(ordered)
        assertEquals(mapOf("Rice" to 2), eventGroup.currentOrder?.dishNameToAmount())
    }

    @Test
    fun `Restaurant Type Without Event Dish Leaves Customers To Own Choice`() {
        val eventGroup = EventGroup(
            1,
            2,
            TableType.COMMON,
            1,
            List(2) { foodPreference() },
            listOf(RestaurantType.ASIAN),
            1,
            mapOf(RestaurantType.EUROPEAN to "Bread")
        ).also { it.currentRestaurantType = RestaurantType.ASIAN }

        assertTrue(eventGroup.placeOrder(mutableMapOf(Waiter() to 2), menu, countertop()))
        assertEquals(mapOf("Rice" to 2), eventGroup.currentOrder?.dishNameToAmount())
    }

    @Test
    fun `Waiter At Action Limit Is Skipped`() {
        val busyWaiter = Waiter().also { it.addToTickLoad(ActionType.TAKE_ORDER, Constants.ACTION_LIMIT) }
        val freeWaiter = Waiter()
        val waiterShares = linkedMapOf(busyWaiter to 3, freeWaiter to 3)
        val eventGroup = eventGroup(listOf(foodPreference()))

        assertTrue(eventGroup.placeOrder(waiterShares, menu, countertop()))

        assertEquals(1, freeWaiter.getTickLoad(ActionType.TAKE_ORDER))
        assertEquals(Constants.ACTION_LIMIT, busyWaiter.getTickLoad(ActionType.TAKE_ORDER))
        assertEquals(3, waiterShares[busyWaiter])
        assertEquals(2, waiterShares[freeWaiter])
    }

    @Test
    fun `No Waiter Tick Load Means No Order`() {
        val waiter = Waiter()
        val eventGroup = eventGroup(List(2) { foodPreference() })
        val orderQueue = ArrayDeque<Order>()

        val ordered = eventGroup.placeOrder(mutableMapOf(waiter to 0), menu, countertop(orderQueue))

        assertFalse(ordered)
        assertEquals(0, eventGroup.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, eventGroup.experience)
        assertNull(eventGroup.currentOrder)
        assertTrue(orderQueue.isEmpty())
    }

    @Test
    fun `Event Group Without Waiter Orders Nothing`() {
        val eventGroup = eventGroup(listOf(foodPreference()))

        assertFalse(eventGroup.placeOrder(mutableMapOf(), menu, countertop()))
        assertEquals(0, eventGroup.customersRemainingInRestaurant)
    }

    @Test
    fun `Customers Without Dish Leave And Experience Is Negative`() {
        val eventGroup = eventGroup(listOf(foodPreference(), foodPreference(listOf(flour, rice))))
        val orderQueue = ArrayDeque<Order>()

        val ordered = eventGroup.placeOrder(mutableMapOf(Waiter() to 2), menu, countertop(orderQueue))

        assertTrue(ordered)
        assertEquals(1, eventGroup.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, eventGroup.experience)
        assertEquals(mapOf("Bread" to 1), eventGroup.currentOrder?.dishNameToAmount())
        assertEquals(1, orderQueue.size)
    }

    @Test
    fun `Nobody Can Eat Means No Order`() {
        val eventGroup = eventGroup(List(2) { foodPreference(listOf(flour, rice)) })
        val orderQueue = ArrayDeque<Order>()

        assertFalse(eventGroup.placeOrder(mutableMapOf(Waiter() to 2), menu, countertop(orderQueue)))
        assertEquals(0, eventGroup.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, eventGroup.experience)
        assertTrue(orderQueue.isEmpty())
    }

    @Test
    fun `Waiter Tick Load Grows With Every Order`() {
        val waiter = Waiter()
        val eventGroup = eventGroup(List(4) { foodPreference() })

        eventGroup.placeOrder(mutableMapOf(waiter to 4), menu, countertop())

        assertEquals(4, waiter.getTickLoad(ActionType.TAKE_ORDER))
        assertEquals(0, waiter.currentLoad)
    }
}
