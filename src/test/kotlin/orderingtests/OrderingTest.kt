package orderingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ordering (F18) of in-house groups through the real [FrontOfHouse]: who orders first, what happens when a dish
 * runs out during the order of one group, customers that find no dish, the ordering logs and the order ids.
 * The menu has a Soup (id 1, broth) and a Salad (id 2, lettuce). Customers without preferences take the highest
 * recipe id, so they take the Salad for as long as there is lettuce.
 */
class OrderingTest {
    private lateinit var output: StringWriter

    private val broth = Ingredient("broth", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)
    private val salt = Ingredient("salt", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)

    private fun lettuce(saladPortions: Int) =
        Ingredient("lettuce", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = SALAD_AMOUNT * saladPortions)

    private fun recipe(id: Int, name: String, ingredient: Ingredient, amount: Int) = Recipe(
        id = id,
        name = name,
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(ingredient to amount),
        basicDishFor = null
    )

    private val orderQueue = ArrayDeque<Order>()
    private lateinit var menu: List<Recipe>
    private lateinit var lettuceIngredient: Ingredient

    /** a counter with stock for exactly [saladPortions] salads and a lot of soup */
    private fun countertop(saladPortions: Int): Countertop {
        lettuceIngredient = lettuce(saladPortions)
        menu = listOf(recipe(1, "Soup", broth, 10), recipe(2, "Salad", lettuceIngredient, SALAD_AMOUNT))
        val pantry = Pantry(
            inventory = mutableListOf(IngredientPackage(broth), IngredientPackage(lettuceIngredient)),
            supplier = Supplier(Stock(listOf(broth, lettuceIngredient)))
        )
        return Countertop(pantry, orderQueue, listOf(Cook(CookType.TOURNANT)), RestaurantType.EUROPEAN)
    }

    private fun frontOfHouse(saladPortions: Int, tables: List<Table>, waiters: List<Waiter>) =
        FrontOfHouse(tables, waiters, drivers = emptyList(), countertop = countertop(saladPortions))

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Order.resetIds()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 4
        orderQueue.clear()
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun allLines(): String = lines().joinToString("\n")

    private fun plain() = FoodPreference(emptyList(), emptyList(), emptyList())

    private fun excludes(vararg ingredients: Ingredient) =
        FoodPreference(ingredients.toList(), emptyList(), emptyList())

    private fun favours(vararg dishes: String, excluded: List<Ingredient> = emptyList()) =
        FoodPreference(excluded, emptyList(), dishes.toList())

    private fun table(id: Int, size: Int) = Table(id, size, TableType.COMMON)

    private fun casual(id: Int, preferences: List<FoodPreference>) = CasualGroup(
        id = id,
        size = preferences.size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = preferences,
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    private fun regular(id: Int, preferences: List<FoodPreference>) = RegularGroup(
        id = id,
        size = preferences.size,
        tableType = TableType.COMMON,
        visitingAt = Time.tick,
        foodPreferences = preferences,
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1
    )

    private fun dishNames(group: CustomerGroup) = group.currentOrder!!.dishes.map { it.recipe.name }

    // ---- who orders first

    @Test
    fun `the customer with the most excluded ingredients orders first`() {
        val foh = frontOfHouse(1, listOf(table(1, 2)), listOf(Waiter()))
        // listed first, the plain customer would take the only salad and the other one could not order
        val group = casual(1, listOf(plain(), excludes(broth)))

        foh.processArrival(group, menu)

        assertEquals(listOf("Salad", "Soup"), dishNames(group))
        assertTrue(lines().none { it.contains("FOH No Ordering") })
        assertEquals(2, group.customersRemainingInRestaurant)
    }

    @Test
    fun `with equally many excluded ingredients the customer with fewer favourite dishes orders first`() {
        val foh = frontOfHouse(1, listOf(table(1, 2)), listOf(Waiter()))
        // both exclude one ingredient; the second one has no favourites and can only eat the salad
        val group = casual(1, listOf(favours("Salad", excluded = listOf(salt)), excludes(broth)))

        foh.processArrival(group, menu)

        assertEquals(listOf("Salad", "Soup"), dishNames(group))
        assertTrue(lines().none { it.contains("FOH No Ordering") })
    }

    @Test
    fun `a dish that runs out during the order is no longer offered to the following customers`() {
        val foh = frontOfHouse(2, listOf(table(1, 3)), listOf(Waiter()))
        val group = casual(1, List(3) { plain() })

        foh.processArrival(group, menu)

        assertEquals(mapOf("Salad" to 2, "Soup" to 1), group.currentOrder!!.dishNameToAmount())
        assertTrue(lines().any { it.endsWith("placed order 1 of Salad:2,Soup:1 with waitstaff 1.") }, allLines())
    }

    @Test
    fun `the ingredients of an order are reserved at once so the next group finds the dish unavailable`() {
        val foh = frontOfHouse(1, listOf(table(1, 2), table(2, 2)), listOf(Waiter()))
        val first = casual(1, listOf(plain(), plain()))
        val second = casual(2, listOf(plain(), plain()))

        foh.processArrival(first, menu)
        foh.processArrival(second, menu)

        assertEquals(mapOf("Salad" to 1, "Soup" to 1), first.currentOrder!!.dishNameToAmount())
        assertEquals(mapOf("Soup" to 2), second.currentOrder!!.dishNameToAmount())
    }

    // ---- customers who find no dish

    @Test
    fun `customers who find no dish leave and the group keeps only the customers that ordered`() {
        val waiter = Waiter()
        val foh = frontOfHouse(2, listOf(table(1, 3)), listOf(waiter))
        val group = casual(1, listOf(plain(), plain(), excludes(broth, lettuceOfMenu())))

        foh.processArrival(group, menu)

        assertEquals(2, group.currentOrder!!.dishes.size)
        assertEquals(2, group.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(
            lines().contains(
                "[IMPORTANT] FOH No Ordering (R 1): Group 1 could not place an order for 1 customers, " +
                    "they leave the restaurant."
            ),
            allLines()
        )
        assertEquals(2, waiter.currentLoad, "the customer who left is not waited on any more")
    }

    @Test
    fun `a CASUAL group in which nobody can order is turned away and its table is free again`() {
        val waiter = Waiter()
        val table = table(1, 2)
        val foh = frontOfHouse(1, listOf(table), listOf(waiter))
        val group = casual(1, List(2) { excludes(broth, lettuceOfMenu()) })

        foh.processArrival(group, menu)

        assertNull(group.currentOrder)
        assertTrue(orderQueue.isEmpty())
        assertTrue(lines().any { it.contains("Group 1 could not place an order for 2 customers") })
        assertTrue(lines().none { it.contains("FOH Ordering (R 1)") })
        assertEquals(0, waiter.currentLoad)
        assertEquals(TableStatus.FREE, table.status)
        val next = casual(2, listOf(plain(), plain()))
        foh.processArrival(next, menu)
        assertNotNull(next.currentOrder, "the table of the group that left can be used again")
    }

    @Test
    fun `a REGULAR group in which nobody can order gets a failed attempt`() {
        val foh = frontOfHouse(1, listOf(table(1, 2)), listOf(Waiter()))
        val group = regular(1, List(2) { excludes(broth, lettuceOfMenu()) })
        assertTrue(foh.reserveTables(group))

        foh.processArrival(group, menu)

        assertNull(group.currentOrder)
        assertEquals(1, group.failedAttempts)
        assertTrue(lines().any { it.contains("Group 1 could not place an order for 2 customers") })
    }

    // ---- the waiter and the logs

    @Test
    fun `the waiter who seated the group takes the order and every ordered customer counts as an order action`() {
        val waiter = Waiter()
        val foh = frontOfHouse(2, listOf(table(1, 3)), listOf(waiter))
        val group = casual(1, listOf(plain(), plain(), excludes(broth, lettuceOfMenu())))

        foh.processArrival(group, menu)

        assertEquals(1, waiter.id)
        assertEquals(2, waiter.getTickLoad(ActionType.TAKE_ORDER))
        assertTrue(lines().any { it.endsWith("placed order 1 of Salad:2 with waitstaff 1.") }, allLines())
    }

    @Test
    fun `the status logs count only successful seatings and the customers that ordered`() {
        val waiter = Waiter()
        val foh = frontOfHouse(2, listOf(table(1, 3)), listOf(waiter))
        val seated = casual(1, listOf(plain(), plain(), excludes(broth, lettuceOfMenu())))
        val sentAway = casual(2, listOf(plain(), plain(), plain()))

        foh.processArrival(seated, menu)
        foh.processArrival(sentAway, menu)
        foh.logAndResetSeatingOrderingTickStatus()

        assertTrue(
            lines().contains("[DEBUG] FOH Seating Status (R 1): 1 waitstaff seated 3 customers on 1 tables."),
            allLines()
        )
        assertTrue(
            lines().contains(
                "[DEBUG] FOH Ordering Status (R 1): The restaurant received orders from 2 customers, " +
                    "1 waitstaff took orders."
            ),
            allLines()
        )
    }

    @Test
    fun `the status counters start again for the next tick`() {
        val foh = frontOfHouse(2, listOf(table(1, 2)), listOf(Waiter()))
        foh.processArrival(casual(1, listOf(plain(), plain())), menu)
        foh.logAndResetSeatingOrderingTickStatus()
        output.buffer.setLength(0)

        foh.logAndResetSeatingOrderingTickStatus()

        assertEquals(
            listOf(
                "[DEBUG] FOH Seating Status (R 1): 0 waitstaff seated 0 customers on 0 tables.",
                "[DEBUG] FOH Ordering Status (R 1): The restaurant received orders from 0 customers, " +
                    "0 waitstaff took orders."
            ),
            lines()
        )
    }

    // ---- order ids

    @Test
    fun `order ids count up over all groups and restaurants and start again at 1 after a reset`() {
        val first = frontOfHouse(2, listOf(table(1, 2)), listOf(Waiter()))
        val groupOne = casual(1, listOf(plain(), plain()))
        first.processArrival(groupOne, menu)
        Logger.restaurantID = 2
        val second = frontOfHouse(2, listOf(table(1, 2)), listOf(Waiter()))
        val groupTwo = casual(2, listOf(plain(), plain()))
        second.processArrival(groupTwo, menu)

        assertEquals(listOf(1, 2), listOf(groupOne, groupTwo).map { it.currentOrder!!.id })

        Order.resetIds()
        assertEquals(1, Order(emptyList()).id)
    }

    @Test
    fun `an order that is placed is queued for the kitchen`() {
        val foh = frontOfHouse(2, listOf(table(1, 2)), listOf(Waiter()))
        val group = casual(1, listOf(plain(), plain()))

        foh.processArrival(group, menu)

        assertEquals(listOf(group.currentOrder), orderQueue.toList())
        assertFalse(group.currentOrder!!.dishes.isEmpty())
    }

    // ---- order actions of the waiters

    @Test
    fun `a waiter at the order limit hands the remaining customers of the group to the next waiter`() {
        val first = Waiter()
        val second = Waiter()
        val group = casual(1, List(12) { plain() })

        val counter = countertop(12)
        group.placeOrder(listOf(first, second), menu, counter)

        assertEquals(10, first.getTickLoad(ActionType.TAKE_ORDER))
        assertEquals(2, second.getTickLoad(ActionType.TAKE_ORDER))
        assertEquals(12, group.currentOrder!!.dishes.size)
    }

    @Test
    fun `a group of ten is taken by a single waiter`() {
        val first = Waiter()
        val second = Waiter()
        val group = casual(1, List(10) { plain() })

        val counter = countertop(10)
        group.placeOrder(listOf(first, second), menu, counter)

        assertEquals(10, first.getTickLoad(ActionType.TAKE_ORDER))
        assertEquals(0, second.getTickLoad(ActionType.TAKE_ORDER))
    }

    @Test
    fun `when even the last waiter is at the limit the overflow stays with the last waiter`() {
        val only = Waiter()
        val group = casual(1, List(12) { plain() })

        val counter = countertop(12)
        group.placeOrder(listOf(only), menu, counter)

        assertEquals(12, only.getTickLoad(ActionType.TAKE_ORDER))
        assertEquals(12, group.currentOrder!!.dishes.size)
    }

    // ---- groups that are refused in the last ticks

    @Test
    fun `a CASUAL group that is refused late is turned away with a negative experience`() {
        val foh = frontOfHouse(1, listOf(table(1, 2)), listOf(Waiter()))
        val group = casual(1, listOf(plain(), plain()))

        foh.refuseLateArrival(group)

        assertEquals(ExperienceType.NEGATIVE, group.experience)
        assertTrue(lines().none { it.contains("Seating") || it.contains("Ordering") }, allLines())
    }

    @Test
    fun `a REGULAR group that is refused late gives its table back and gets a failed attempt`() {
        val table = table(1, 2)
        val foh = frontOfHouse(1, listOf(table), listOf(Waiter()))
        val group = regular(1, listOf(plain(), plain()))
        assertTrue(foh.reserveTables(group))
        assertEquals(TableStatus.RESERVED, table.status)

        foh.refuseLateArrival(group)

        assertEquals(TableStatus.FREE, table.status)
        assertEquals(1, group.failedAttempts)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
    }

    /** excluding the broth and the lettuce rules out every dish of the menu */
    private fun lettuceOfMenu(): Ingredient = lettuceIngredient

    private companion object {
        const val SALAD_AMOUNT = 100
    }
}
