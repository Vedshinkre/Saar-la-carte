package planningtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Kitchen
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Buying ingredients (F09): supplier packaging, stock availability, package expiry and the planning
 * of a whole evening through the restaurant.
 */
class BuyingIngredientsTest {
    private lateinit var output: StringWriter

    private val rice = Ingredient("rice", MeasurementUnit.G, 3, 100)
    private val beef = Ingredient("beef", MeasurementUnit.G, 3, 50)
    private val riceBowl = Recipe(1, "Rice Bowl", 10, listOf(CookType.TOURNANT), mutableMapOf(rice to 20), null)
    private val beefRice =
        Recipe(2, "Beef Rice", 10, listOf(CookType.TOURNANT), mutableMapOf(rice to 10, beef to 50), null)

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
        Time.evening = 1
    }

    private fun lines() = output.toString().lines().filter { it.isNotBlank() }.map { it.substringAfter("] ") }

    private fun stock() = Stock(listOf(rice, beef))

    private fun amountInPantry(pantry: Pantry, ingredient: Ingredient) =
        pantry.getPackagesForIngredient(ingredient).sumOf { it.currentAmount }

    @Test
    fun `Supplier Rounds Up To Whole Packages`() {
        val supplier = Supplier(stock())

        assertEquals(3, supplier.procure(rice, 250).size)
        assertEquals(2, supplier.procure(rice, 200).size)
        assertEquals(1, supplier.procure(rice, 1).size)
        assertTrue(supplier.procure(rice, 0).isEmpty())
        assertTrue(supplier.procure(rice, -5).isEmpty())
    }

    @Test
    fun `Supplier Uses The Stock Ingredient For Packaging`() {
        val supplier = Supplier(stock())
        val riceCopy = Ingredient("rice", MeasurementUnit.G, 3, 1)

        val packages = supplier.procure(riceCopy, 150)

        assertEquals(2, packages.size)
        assertSame(rice, packages.first().ingredient)
        assertFalse(packages.first().isOpen)
        assertEquals(4, packages.first().expiryDate)
    }

    @Test
    fun `Unavailable Or Unknown Ingredient Cannot Be Bought`() {
        val stock = stock()
        val supplier = Supplier(stock)
        val salt = Ingredient("salt", MeasurementUnit.G, 3, 10)
        stock.setIngredientToUnavailable(rice, 2)

        assertFalse(supplier.isAvailable(rice))
        assertTrue(supplier.procure(rice, 100).isEmpty())
        assertTrue(supplier.procure(salt, 10).isEmpty())
        assertNull(stock.getIngredient(salt))
        assertFailsWith<IllegalArgumentException> { stock.setIngredientToUnavailable(salt, 1) }
        assertFailsWith<IllegalArgumentException> { stock.setIngredientToAvailable(salt) }
    }

    @Test
    fun `Unavailability Counts Down Each Evening`() {
        val stock = stock()
        stock.setIngredientToUnavailable(rice, 2)

        stock.applyUnavailableDurations()
        assertFalse(stock.isIngredientAvailable(rice))
        stock.applyUnavailableDurations()
        assertTrue(stock.isIngredientAvailable(rice))
        stock.applyUnavailableDurations()
        assertTrue(stock.isIngredientAvailable(beef))

        stock.setIngredientToUnavailable(beef, 5)
        stock.setIngredientToAvailable(beef)
        assertTrue(stock.isIngredientAvailable(beef))
    }

    @Test
    fun `Package Expires On Its Best Before Evening`() {
        Time.evening = 2
        val ricePackage = IngredientPackage(rice)

        Time.evening = 4
        assertFalse(ricePackage.hasExpired())
        Time.evening = 5
        assertTrue(ricePackage.hasExpired())
    }

    @Test
    fun `Taking From A Package Opens It And Never Goes Below Zero`() {
        val ricePackage = IngredientPackage(rice)

        assertEquals(0, ricePackage.removeAmount(0))
        assertFalse(ricePackage.isOpen)
        assertEquals(30, ricePackage.removeAmount(30))
        assertTrue(ricePackage.isOpen)
        assertEquals(70, ricePackage.removeAmount(500))
        assertEquals(0, ricePackage.currentAmount)
        assertEquals(0, ricePackage.removeAmount(1))
    }

    @Test
    fun `Pantry Reservation Skips Expired Packages And Drops Empty Ones`() {
        val expiredPackage = IngredientPackage(rice, 100, 1, false)
        val soonerPackage = IngredientPackage(rice, 40, 3, false)
        val laterPackage = IngredientPackage(rice, 50, 4, false)
        val pantry = Pantry(mutableListOf(expiredPackage, laterPackage, soonerPackage), Supplier(stock()))

        assertEquals(60, pantry.checkInventory(rice, 60))
        assertEquals(90, pantry.checkInventory(rice, 200))
        pantry.reserveIngredients(mapOf(rice to 40))

        assertEquals(100, expiredPackage.currentAmount)
        assertEquals(listOf(laterPackage), pantry.getPackagesForIngredient(rice))
        assertEquals(50, laterPackage.currentAmount)
    }

    @Test
    fun `Nothing Bought Still Logs Restocked`() {
        val stock = stock()
        stock.setIngredientToUnavailable(rice, 1)
        val pantry = Pantry(stock)

        pantry.ensureQuantities(mapOf(rice to 100, beef to 0))

        assertEquals(listOf("Pantry (R 1): Restocked ingredients."), lines())
        assertEquals(0, amountInPantry(pantry, rice))
    }

    @Test
    fun `Planning Uses The Last Three Visits Plus Event Dishes`() {
        val regularGroup = RegularGroup(1, 1, TableType.COMMON, 1, emptyList(), 1, 1, 1)
        val oldestOrder = Order(listOf(Dish(beefRice)))
        regularGroup.addOrderToHistory(oldestOrder)
        repeat(3) { regularGroup.addOrderToHistory(Order(listOf(Dish(riceBowl)))) }
        val expiredPackage = IngredientPackage(rice, 70, 1, true)
        val pantry = Pantry(mutableListOf(expiredPackage, IngredientPackage(rice, 10, 3, true)), Supplier(stock()))
        val kitchen = Kitchen(listOf(Cook(CookType.TOURNANT)), pantry, mutableListOf(), RestaurantType.ASIAN)

        kitchen.planForIngredients(regularGroup.orderHistory, 0, listOf(riceBowl, beefRice), listOf(riceBowl to 2))

        assertFalse(oldestOrder in regularGroup.orderHistory)
        assertEquals("Pantry (R 1): Removed 70 g of rice from the pantry.", lines().first())
        // 3 history portions and 2 event portions of 20 g, 10 g still in the pantry
        assertEquals(110, amountInPantry(pantry, rice))
        assertEquals(0, amountInPantry(pantry, beef))
    }

    @Test
    fun `Restaurant Plans Events, Regular Seats Without History And Free Seats`() {
        val restaurantStats = RestaurantStats(1, RestaurantType.ASIAN, 5, 20, true, 0, 0, listOf(riceBowl))
        val restaurantStaff = RestaurantStaff(mutableListOf(Cook(CookType.TOURNANT)), mutableListOf(), mutableListOf())
        val tables = listOf(Table(1, 4, TableType.COMMON), Table(2, 3, TableType.COMMON), Table(3, 8, TableType.BAR))
        val restaurant = Restaurant(restaurantStats, "Wok", restaurantStaff, tables, stock())
        val eventGroup = EventGroup(
            7,
            3,
            TableType.COMMON,
            5,
            List(3) { FoodPreference(emptyList(), emptyList(), emptyList()) },
            listOf(RestaurantType.ASIAN),
            1,
            mapOf(RestaurantType.ASIAN to "Rice Bowl"),
        ).also { it.currentRestaurantType = RestaurantType.ASIAN }
        restaurant.eventCustomers.add(eventGroup)
        val newRegularGroup = RegularGroup(2, 4, TableType.COMMON, 5, emptyList(), 1, 1, 1)

        restaurant.prepareForEvening(listOf(newRegularGroup))

        // 3 event portions plus ceil((8 free + 4 regular seats) / 10) = 2 estimated portions, 20 g each
        assertEquals(
            listOf("Pantry (R 1): Procured 100 g of rice from the supplier.", "Pantry (R 1): Restocked ingredients."),
            lines()
        )
        assertEquals(8, restaurantStats.availableSeats[TableType.BAR])
        assertEquals(0, restaurantStats.availableSeats[TableType.COMMON])
    }
}
