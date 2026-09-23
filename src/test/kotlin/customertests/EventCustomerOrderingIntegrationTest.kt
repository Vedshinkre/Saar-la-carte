package customertests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
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
import kotlin.test.assertTrue

/**
 * Tests the ordering of EventGroups through the FrontOfHouse.
 */
class EventCustomerOrderingIntegrationTest {
    private lateinit var output: StringWriter
    private lateinit var kitchenQueue: ArrayDeque<Order>

    private val water = Ingredient("water", MeasurementUnit.ML, 5, 1000)
    private val soup = Recipe(1, "soup", 10, listOf(CookType.TOURNANT), mutableMapOf(water to 1), null)
    private val stew = Recipe(2, "stew", 10, listOf(CookType.TOURNANT), mutableMapOf(water to 1), null)
    private val menu = listOf(soup, stew)

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
        Time.evening = 1
        kitchenQueue = ArrayDeque()
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse {
        val pantry = Pantry(
            mutableListOf(IngredientPackage(water)),
            Supplier(Stock(listOf(water)))
        )
        val countertop = Countertop(pantry, kitchenQueue, listOf(Cook(CookType.TOURNANT)), RestaurantType.EUROPEAN)
        return FrontOfHouse(tables, waiters, emptyList(), countertop)
    }

    private fun waiter(id: Int) = Waiter().apply { this.id = id }

    private fun eventGroup(
        id: Int,
        foodPreferences: List<FoodPreference>,
        eventDish: String = "soup",
        restaurantType: RestaurantType? = RestaurantType.EUROPEAN
    ) = EventGroup(
        id,
        foodPreferences.size,
        TableType.COMMON,
        5,
        foodPreferences,
        listOf(RestaurantType.EUROPEAN),
        1,
        mapOf(RestaurantType.EUROPEAN to eventDish)
    ).also { it.currentRestaurantType = restaurantType }

    private fun foodPreference(excluded: List<Ingredient> = emptyList()) =
        FoodPreference(excluded, emptyList(), emptyList())

    @Test
    fun `Seated Event Group Orders Event Dish`() {
        val frontOfHouse = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val eventGroup = eventGroup(1, List(4) { foodPreference() })

        assertTrue(frontOfHouse.reserveTables(eventGroup))
        frontOfHouse.processArrival(eventGroup, menu)

        assertEquals(mapOf("soup" to 4), eventGroup.currentOrder?.dishNameToAmount())
    }

    @Test
    fun `No Chosen Restaurant Type Orders Highest Recipe Id`() {
        val frontOfHouse = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val eventGroup = eventGroup(1, List(4) { foodPreference() }, restaurantType = null)

        assertTrue(frontOfHouse.reserveTables(eventGroup))
        frontOfHouse.processArrival(eventGroup, menu)

        assertEquals(mapOf("stew" to 4), eventGroup.currentOrder?.dishNameToAmount())
    }

    @Test
    fun `Event Dish Not On Menu Falls Back To Usual Choice`() {
        val frontOfHouse = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val eventGroup = eventGroup(1, List(4) { foodPreference() }, eventDish = "paella")

        assertTrue(frontOfHouse.reserveTables(eventGroup))
        frontOfHouse.processArrival(eventGroup, menu)

        assertEquals(mapOf("stew" to 4), eventGroup.currentOrder?.dishNameToAmount())
    }

    @Test
    fun `Event Group Order Reaches Kitchen Queue`() {
        val frontOfHouse = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val eventGroup = eventGroup(1, List(4) { foodPreference() })

        assertTrue(frontOfHouse.reserveTables(eventGroup))
        frontOfHouse.processArrival(eventGroup, menu)

        assertEquals(1, kitchenQueue.size)
        assertEquals(4, kitchenQueue.first().dishes.size)
        assertEquals(eventGroup.currentOrder, kitchenQueue.first())
    }

    @Test
    fun `Ordering Log Names Group Dishes And Waiter`() {
        val frontOfHouse = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val eventGroup = eventGroup(1, List(4) { foodPreference() })

        assertTrue(frontOfHouse.reserveTables(eventGroup))
        frontOfHouse.processArrival(eventGroup, menu)

        val orderingLine = lines().single { it.contains("FOH Ordering (R 1)") }
        assertTrue(orderingLine.contains("Group 1 placed order"))
        assertTrue(orderingLine.endsWith("of soup:4 with waitstaff 1."))
    }

    @Test
    fun `Event Customers Without Dish Leave`() {
        val frontOfHouse = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val eventGroup = eventGroup(1, listOf(foodPreference(), foodPreference(), foodPreference(listOf(water))))

        assertTrue(frontOfHouse.reserveTables(eventGroup))
        frontOfHouse.processArrival(eventGroup, menu)

        assertEquals(2, eventGroup.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, eventGroup.experience)
        assertTrue(
            lines().any { it.contains("FOH No Ordering (R 1): Group 1 could not place an order for 1 customers") }
        )
    }

    @Test
    fun `Taking Event Group Order Costs One Action Per Customer`() {
        val waiter = waiter(1)
        val frontOfHouse = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter))
        val eventGroup = eventGroup(1, List(4) { foodPreference() })

        assertTrue(frontOfHouse.reserveTables(eventGroup))
        frontOfHouse.processArrival(eventGroup, menu)

        assertEquals(4, waiter.getTickLoad(ActionType.TAKE_ORDER))
        assertEquals(0, waiter.currentLoad)
    }
}
