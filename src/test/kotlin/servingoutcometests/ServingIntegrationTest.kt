package servingoutcometests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for F19's REGULAR/CASUAL serving path through the real [FrontOfHouse]
 * wiring. Kitchen cooking itself (F10/F11/F12) is out of scope, so dishes are flipped to
 * COOKED directly rather than run through a real Kitchen tick loop.
 */
class ServingIntegrationTest {

    @BeforeEach
    fun setup() {
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
    }

    private val flour = Ingredient("flour", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)
    private val breadRecipe = Recipe(
        id = 1,
        name = "Bread",
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(flour to 10),
        basicDishFor = null
    )
    private val menu = listOf(breadRecipe)

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse {
        val stock = Stock(listOf(flour))
        val pantry = Pantry(inventory = mutableListOf(IngredientPackage(flour)), supplier = Supplier(stock))
        val countertop = Countertop(pantry = pantry, orderQueue = ArrayDeque(), cooks = listOf(Cook(CookType.TOURNANT)))
        return FrontOfHouse(tables = tables, waiters = waiters, drivers = emptyList(), countertop = countertop)
    }

    private fun foodPreferences(count: Int): List<FoodPreference> =
        List(count) { FoodPreference(emptyList(), emptyList(), emptyList()) }

    private fun regularGroup(id: Int, size: Int): RegularGroup = RegularGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = foodPreferences(size),
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1
    )

    private fun casualGroup(id: Int, size: Int): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = foodPreferences(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    private fun markAllCooked(group: de.unisaarland.cs.se.selab.customer.CustomerGroup) {
        group.currentOrder?.dishes?.forEach { it.status = DishStatus.COOKED }
    }

    @Test
    fun `a REGULAR group's order is only served once every dish is cooked`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val waiter = Waiter()
        val foh = frontOfHouse(listOf(table), listOf(waiter))
        val group = regularGroup(id = 1, size = 4)
        foh.reserveTables(group)

        foh.processArrival(group, menu)
        val dishes = requireNotNull(group.currentOrder).dishes
        assertTrue(dishes.all { it.status == DishStatus.UNCOOKED }, "nothing cooked yet")

        foh.processServing()
        assertTrue(dishes.all { it.status == DishStatus.UNCOOKED }, "still nothing to serve")

        markAllCooked(group)
        foh.processServing()
        assertTrue(dishes.all { it.status == DishStatus.SERVED })
    }

    @Test
    fun `REGULAR is served before CASUAL when both share one capacity-limited waiter`() {
        // Both tables are a perfect fit for their group's size, satisfying the 3/4 rule
        // that applies to CASUAL's ad-hoc table assignment (no rule-lift like REGULAR gets).
        val regularsTable = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val casualsTable = Table(id = 2, size = 4, tableType = TableType.COMMON)
        val waiter = Waiter()
        val foh = frontOfHouse(listOf(regularsTable, casualsTable), listOf(waiter))
        val regular = regularGroup(id = 5, size = 4)
        val casual = casualGroup(id = 1, size = 4)
        foh.reserveTables(regular)

        foh.processArrival(regular, menu)
        foh.processArrival(casual, menu)
        markAllCooked(regular)
        markAllCooked(casual)
        // Exactly 4 SERVE slots left: enough for one group's order, not both.
        waiter.addToTickLoad(ActionType.SERVE, Constants.ACTION_LIMIT - 4)

        foh.processServing()

        val regularDishes = requireNotNull(regular.currentOrder).dishes
        val casualDishes = requireNotNull(casual.currentOrder).dishes
        assertTrue(regularDishes.all { it.status == DishStatus.SERVED }, "REGULAR takes the only remaining capacity")
        assertTrue(casualDishes.all { it.status == DishStatus.COOKED }, "CASUAL gets nothing this tick")
        assertFalse(waiter.id == null, "sanity check: seating must have assigned the shared waiter an id")
    }
}
