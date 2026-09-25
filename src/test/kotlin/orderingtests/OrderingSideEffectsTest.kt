package orderingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Ordering (F18) beyond the in-house basics: delivery orders, the ordering log contents, the waiter
 * load and order history an order leaves behind, and the basic dish flag.
 */
class OrderingSideEffectsTest {
    private lateinit var output: StringWriter

    private val broth = Ingredient("broth", MeasurementUnit.ML, 5, 1000)
    private val lettuce = Ingredient("lettuce", MeasurementUnit.G, 5, 1000)
    private val soup = Recipe(1, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(broth to 10), null)
    private val salad =
        Recipe(2, "Salad", 10, listOf(CookType.TOURNANT), mutableMapOf(lettuce to 100), RestaurantType.EUROPEAN)
    private val menu = listOf(soup, salad)
    private val menuIngredients = listOf(broth, lettuce)
    private val orderQueue = ArrayDeque<Order>()

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 4
        Time.evening = 1
        Order.resetIds()
        orderQueue.clear()
    }

    private fun lines() = output.toString().lines().filter { it.isNotBlank() }.map { it.substringAfter("] ") }

    private fun plain() = FoodPreference(emptyList(), emptyList(), emptyList())

    private fun picky() = FoodPreference(listOf(broth, lettuce), emptyList(), emptyList())

    private fun likesSoup() = FoodPreference(emptyList(), emptyList(), listOf("Soup"))

    private fun countertop() = Countertop(
        Pantry(mutableListOf(IngredientPackage(broth), IngredientPackage(lettuce)), Supplier(Stock(menuIngredients))),
        orderQueue,
        listOf(Cook(CookType.TOURNANT)),
        RestaurantType.EUROPEAN
    )

    private fun frontOfHouse(waiters: List<Waiter> = emptyList(), tableSize: Int = 4) =
        FrontOfHouse(listOf(Table(1, tableSize, TableType.COMMON)), waiters, emptyList(), countertop())

    private fun casualGroup(id: Int, preferences: List<FoodPreference>, distance: Int = 0) = CasualGroup(
        id,
        preferences.size,
        TableType.COMMON,
        Time.tick,
        preferences,
        listOf(RestaurantType.EUROPEAN),
        listOf(1),
        distance,
        RatingLikelihood.NEVER,
    )

    @Test
    fun `Delivery Order Is Logged Without Waitstaff`() {
        val frontOfHouse = frontOfHouse()
        val deliveryGroup = casualGroup(1, listOf(plain(), plain()), distance = 5)

        assertTrue(frontOfHouse.processArrival(deliveryGroup, menu))
        frontOfHouse.logAndResetSeatingOrderingTickStatus()

        assertEquals(
            listOf(
                "FOH Ordering (R 1): Group 1 placed order 1 of Salad:2.",
                "FOH Seating Status (R 1): 0 waitstaff seated 0 customers on 0 tables.",
                "FOH Ordering Status (R 1): The restaurant received orders from 2 customers, 0 waitstaff took orders.",
            ),
            lines()
        )
        assertSame(deliveryGroup.currentOrder, orderQueue.single())
    }

    @Test
    fun `Delivery Customer Without A Dish Leaves`() {
        val deliveryGroup = casualGroup(1, listOf(picky(), plain()), distance = 5)

        frontOfHouse().processArrival(deliveryGroup, menu)

        assertEquals(
            "FOH No Ordering (R 1): Group 1 could not place an order for 1 customers, they leave the restaurant.",
            lines().last()
        )
        assertEquals(1, deliveryGroup.currentOrder!!.dishes.size)
        assertEquals(ExperienceType.NEGATIVE, deliveryGroup.experience)
    }

    @Test
    fun `Delivery Group Where Nobody Can Order Places No Order`() {
        val frontOfHouse = frontOfHouse()
        val deliveryGroup = casualGroup(1, listOf(picky(), picky()), distance = 5)

        frontOfHouse.processArrival(deliveryGroup, menu)
        frontOfHouse.logAndResetSeatingOrderingTickStatus()

        assertNull(deliveryGroup.currentOrder)
        assertTrue(orderQueue.isEmpty())
        assertTrue(lines().none { it.startsWith("FOH Ordering (") })
        assertTrue(lines().last().contains("received orders from 0 customers"))
    }

    @Test
    fun `Order Log Counts Equal Dishes Sorted By Name`() {
        val casualGroup = casualGroup(1, listOf(likesSoup(), plain(), likesSoup()))

        frontOfHouse(listOf(Waiter())).processArrival(casualGroup, menu)

        assertEquals("FOH Ordering (R 1): Group 1 placed order 1 of Salad:1,Soup:2 with waitstaff 1.", lines().last())
    }

    @Test
    fun `Customers Who Leave Free The Waiter Load`() {
        val waiter = Waiter()
        val casualGroup = casualGroup(1, listOf(plain(), picky(), plain()))

        frontOfHouse(listOf(waiter)).processArrival(casualGroup, menu)

        assertEquals(2, waiter.currentLoad)
        assertEquals(2, waiter.getTickLoad(ActionType.TAKE_ORDER))
        assertEquals(2, casualGroup.customersRemainingInRestaurant)
    }

    @Test
    fun `Regular Keeps Only Its Last Three Orders`() {
        val regularGroup = RegularGroup(1, 2, TableType.COMMON, Time.tick, listOf(plain(), plain()), 1, 1, 1)
        val placedOrders = mutableListOf<Order>()

        repeat(4) {
            val frontOfHouse = frontOfHouse(listOf(Waiter()), tableSize = 2)
            regularGroup.startNewVisit()
            assertTrue(frontOfHouse.reserveTables(regularGroup))
            frontOfHouse.processArrival(regularGroup, menu)
            placedOrders.add(regularGroup.currentOrder!!)
        }

        assertEquals(placedOrders.drop(1), regularGroup.orderHistory.toList())
    }

    @Test
    fun `Ordered Dish Knows If It Is Basic In This Restaurant`() {
        val europeanDish = plain().decideDish(menu, "", RestaurantType.EUROPEAN)
        val asianDish = plain().decideDish(menu, "", RestaurantType.ASIAN)

        assertEquals(salad, europeanDish?.recipe)
        assertTrue(europeanDish!!.isBasic)
        assertFalse(asianDish!!.isBasic)
    }
}
