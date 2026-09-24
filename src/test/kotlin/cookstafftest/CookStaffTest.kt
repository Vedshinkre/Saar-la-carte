package cookstafftest

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.CookResult
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.StaffType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.incidents.StaffChangeIncident
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.Kitchen
import de.unisaarland.cs.se.selab.restaurant.Pantry
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
 * The cook staff (F11): cook lifecycle, staff change incidents and the cook ids of the kitchen.
 */
class CookStaffTest {
    private lateinit var output: StringWriter

    private val fish = Ingredient("fish", MeasurementUnit.G, 10, 1000)
    private val fishRecipe = Recipe(1, "Fish", 30, listOf(CookType.FISH), mutableMapOf(fish to 1), null)
    private val cakeRecipe = Recipe(2, "Cake", 10, listOf(CookType.EXEC, CookType.PASTRY), mutableMapOf(), null)

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
        Time.evening = 1
    }

    private fun recipe(duration: Int) = Recipe(9, "Stew", duration, listOf(CookType.SOUS), mutableMapOf(), null)

    private fun staff(vararg cooks: Cook) = RestaurantStaff(cooks.toMutableList(), mutableListOf(), mutableListOf())

    private fun incident(restaurantStaff: RestaurantStaff, number: Int, cookType: CookType?, staffType: StaffType) =
        StaffChangeIncident(1, 1, number, staffType, cookType, restaurantStaff)

    private fun cookIncident(restaurantStaff: RestaurantStaff, number: Int, cookType: CookType) =
        incident(restaurantStaff, number, cookType, StaffType.COOK)

    private fun kitchen(restaurantStaff: RestaurantStaff, orderQueue: MutableList<Order> = mutableListOf()) =
        Kitchen(restaurantStaff.cooks, Pantry(Stock(listOf(fish))), orderQueue, RestaurantType.EUROPEAN)

    private fun callsUntilCooked(duration: Int): Int {
        val cook = Cook(CookType.SOUS)
        cook.startCooking(recipe(duration), listOf(Dish(recipe(duration))), 1)
        var calls = 1
        while (cook.cookDishes().finishedThisTick == 0) calls++
        return calls
    }

    @Test
    fun `Cook Id Is Only Set Once`() {
        val cook = Cook(CookType.SOUS)
        cook.setId(4)
        cook.setId(7)

        assertEquals(4, cook.id)
    }

    @Test
    fun `Cooking Takes The Duration Rounded Up To Whole Ticks`() {
        assertEquals(1, callsUntilCooked(5))
        assertEquals(1, callsUntilCooked(10))
        assertEquals(2, callsUntilCooked(11))
        assertEquals(2, callsUntilCooked(20))
        assertEquals(3, callsUntilCooked(25))
    }

    @Test
    fun `Cook Stays Busy Until The Last Tick And Then Frees Up`() {
        val cook = Cook(CookType.SOUS)
        val dishes = listOf(Dish(recipe(20)), Dish(recipe(20)), Dish(recipe(20)))
        cook.startCooking(recipe(20), dishes, 3, mapOf(3 to dishes.take(1), 1 to dishes.drop(1)))

        assertTrue(dishes.all { it.status == DishStatus.COOKING })
        assertEquals(mapOf(1 to 2, 3 to 1), cook.getAssignedCountsByOrder())
        assertEquals(listOf(1, 3), cook.getAssignedCountsByOrder().keys.toList())

        assertEquals(CookResult(true, 3, 0), cook.cookDishes())
        assertTrue(cook.isCooking)
        assertTrue(dishes.all { it.status == DishStatus.COOKING })

        assertEquals(CookResult(true, 3, 3), cook.cookDishes())
        assertFalse(cook.isCooking)
        assertTrue(dishes.all { it.status == DishStatus.COOKED })
        assertNull(cook.currentRecipe)
        assertNull(cook.orderId)
        assertTrue(cook.getDishes().isEmpty())
        assertEquals(CookResult(false, 0, 0), cook.cookDishes())
    }

    @Test
    fun `Added Cooks Are Idle And Put In Front`() {
        val existingCook = Cook(CookType.SOUS).also { it.id = 1 }
        val restaurantStaff = staff(existingCook)
        val incident = cookIncident(restaurantStaff, 2, CookType.PASTRY)

        incident.apply()

        assertEquals("STAFF", incident.type)
        assertEquals(listOf(CookType.PASTRY, CookType.PASTRY, CookType.SOUS), restaurantStaff.cooks.map { it.type })
        assertTrue(restaurantStaff.cooks.take(2).all { it.id == null && !it.isCooking })
        assertSame(existingCook, restaurantStaff.cooks.last())
    }

    @Test
    fun `Removing Cooks Only Takes The Matching Type And Stops When None Are Left`() {
        val restaurantStaff = staff(Cook(CookType.FISH), Cook(CookType.SOUS), Cook(CookType.FISH))

        cookIncident(restaurantStaff, 0, CookType.SOUS).apply()
        assertEquals(3, restaurantStaff.cooks.size)

        cookIncident(restaurantStaff, -5, CookType.FISH).apply()
        assertEquals(listOf(CookType.SOUS), restaurantStaff.cooks.map { it.type })

        incident(restaurantStaff, 3, null, StaffType.COOK).apply()
        assertEquals(1, restaurantStaff.cooks.size)
    }

    @Test
    fun `Waiters And Drivers Are Added In Front And Removed From The Back`() {
        val firstWaiter = Waiter()
        val restaurantStaff = RestaurantStaff(mutableListOf(), mutableListOf(firstWaiter), mutableListOf(Driver()))

        incident(restaurantStaff, 1, null, StaffType.WAITSTAFF).apply()
        incident(restaurantStaff, 2, null, StaffType.DRIVER).apply()
        assertSame(firstWaiter, restaurantStaff.waiters.last())
        assertEquals(3, restaurantStaff.drivers.size)

        incident(restaurantStaff, -1, null, StaffType.WAITSTAFF).apply()
        incident(restaurantStaff, -9, null, StaffType.DRIVER).apply()
        assertFalse(firstWaiter in restaurantStaff.waiters)
        assertTrue(restaurantStaff.drivers.isEmpty())
    }

    @Test
    fun `Cook Ids Count Up And Sorting Puts Cooks Without Id Last`() {
        val cookWithoutId = Cook(CookType.SOUS)
        val cookThree = Cook(CookType.SOUS).also { it.id = 3 }
        val cookOne = Cook(CookType.SOUS).also { it.id = 1 }
        val kitchen = kitchen(staff(cookThree, cookWithoutId, cookOne))

        assertEquals(listOf(1, 2, 3), List(3) { kitchen.getNextCookId() })
        assertEquals(listOf(cookOne, cookThree, cookWithoutId), kitchen.getCooksSorted())
    }

    @Test
    fun `Newly Added Cook Is Chosen Right Away Because The Roster Is Live`() {
        val busyExecCook = Cook(CookType.EXEC).also { it.isCooking = true }
        val restaurantStaff = staff(busyExecCook)
        val kitchen = kitchen(restaurantStaff)
        assertNull(kitchen.chooseCook(cakeRecipe))

        cookIncident(restaurantStaff, 1, CookType.PASTRY).apply()
        val chosenCook = kitchen.chooseCook(cakeRecipe)

        assertEquals(CookType.PASTRY, chosenCook?.type)
        assertEquals(1, chosenCook?.id)
    }

    @Test
    fun `Removing Every Cook Of A Type Makes Its Recipe Unorderable`() {
        val restaurantStaff = staff(Cook(CookType.FISH))
        val pantry = Pantry(mutableListOf(IngredientPackage(fish)), Supplier(Stock(listOf(fish))))
        val countertop = Countertop(pantry, ArrayDeque(), restaurantStaff.cooks, RestaurantType.EUROPEAN)
        assertEquals(listOf(fishRecipe), countertop.getAvailableRecipes(listOf(fishRecipe)))

        cookIncident(restaurantStaff, -1, CookType.FISH).apply()

        assertTrue(countertop.getAvailableRecipes(listOf(fishRecipe)).isEmpty())
    }

    @Test
    fun `Dishes Of A Removed Busy Cook Stay Stuck In Cooking`() {
        // staff incidents only happen between evenings after resetKitchen, so this cannot happen in a real run
        val restaurantStaff = staff(Cook(CookType.FISH))
        val order = Order(listOf(Dish(fishRecipe)))
        val kitchen = kitchen(restaurantStaff, mutableListOf(order))
        kitchen.processCooking()

        cookIncident(restaurantStaff, -1, CookType.FISH).apply()
        cookIncident(restaurantStaff, 1, CookType.FISH).apply()
        repeat(5) { kitchen.processCooking() }

        assertEquals(DishStatus.COOKING, order.dishes.single().status)
        assertFalse(restaurantStaff.cooks.single().isCooking)
    }

    @Test
    fun `Reset Between Evenings Keeps Roster Changes But Clears Cook State`() {
        val restaurantStaff = staff(Cook(CookType.FISH), Cook(CookType.SOUS))
        val servedDish = Dish(fishRecipe).also { it.status = DishStatus.SERVED }
        val eatenDish = Dish(fishRecipe).also { it.status = DishStatus.EATEN }
        val order = Order(listOf(Dish(fishRecipe), servedDish, eatenDish))
        val kitchen = kitchen(restaurantStaff, mutableListOf(order))
        kitchen.processCooking()
        kitchen.getNextCookId()

        kitchen.resetKitchen()
        cookIncident(restaurantStaff, 2, CookType.PASTRY).apply()
        cookIncident(restaurantStaff, -1, CookType.SOUS).apply()

        assertEquals(listOf(CookType.PASTRY, CookType.PASTRY, CookType.FISH), restaurantStaff.cooks.map { it.type })
        assertTrue(restaurantStaff.cooks.all { it.id == null && !it.isCooking && it.currentRecipe == null })
        assertEquals(listOf(DishStatus.ABORTED, DishStatus.SERVED, DishStatus.EATEN), order.dishes.map { it.status })
        assertEquals(1, kitchen.chooseCook(cakeRecipe)?.id)
    }
}
