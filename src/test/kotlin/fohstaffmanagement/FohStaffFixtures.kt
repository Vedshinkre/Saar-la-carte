package fohstaffmanagement

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
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
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Shared builders for the waiter-id tests. The scenarios are written against the specification
 * (front of house chapter), not against the implementation: ordinary group sizes, ordinary
 * tables, and expectations taken from the spec's wording.
 */
internal object FohStaffFixtures {
    private val flour = Ingredient("flour", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)
    private val breadRecipe = Recipe(
        id = 1,
        name = "Bread",
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(flour to 10),
        basicDishFor = null
    )
    val menu = listOf(breadRecipe)

    /** captures everything the simulation logs, so tests can assert on the graded log lines */
    class LogCapture {
        private val sink = StringWriter()

        init {
            Logger.setup(LogLevel.DEBUG)
            Logger.restaurantID = 1
            Logger.setup(PrintWriter(sink))
        }

        val lines: List<String> get() = sink.toString().lines().filter { it.isNotBlank() }

        fun lines(prefix: String): List<String> = lines.filter { it.contains(prefix) }

        /** forget everything logged so far, e.g. the output of a priming phase */
        fun clear() = sink.buffer.setLength(0)

        fun release() = Logger.setup(PrintWriter(System.out))
    }

    fun resetClock() {
        Time.tick = 1
        Time.evening = 1
    }

    fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse {
        val stock = Stock(listOf(flour))
        val pantry = Pantry(inventory = mutableListOf(IngredientPackage(flour)), supplier = Supplier(stock))
        val countertop = Countertop(
            pantry = pantry,
            orderQueue = ArrayDeque(),
            cooks = listOf(Cook(CookType.TOURNANT)),
            restaurantType = RestaurantType.EUROPEAN
        )
        return FrontOfHouse(tables = tables, waiters = waiters, drivers = emptyList(), countertop = countertop)
    }

    fun tables(count: Int, size: Int = 4): List<Table> =
        List(count) { Table(id = it + 1, size = size, tableType = TableType.COMMON) }

    fun waiters(count: Int): List<Waiter> = List(count) { Waiter() }

    private fun preferences(size: Int) = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) }

    fun casualGroup(id: Int, size: Int = 4): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = preferences(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    fun eventGroup(id: Int, size: Int): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = preferences(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = 1,
        eventDishes = mapOf(RestaurantType.EUROPEAN to "Bread")
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    /** an order of [count] dishes that all have the given status */
    fun orderWith(status: DishStatus, count: Int = 3): Order =
        Order(List(count) { Dish(breadRecipe).apply { this.status = status } })

    fun setDishes(group: CustomerGroup, status: DishStatus) {
        group.currentOrder?.dishes?.forEach { it.status = status }
    }

    /** ids handed out so far, ascending; waiters that have not acted are left out */
    fun assignedIds(waiters: List<Waiter>): List<Int> = waiters.mapNotNull { it.id }.sorted()

    /** a restaurant whose first [assigned] waiters already acted this evening */
    data class Primed(val foh: FrontOfHouse, val waiters: MutableList<Waiter>)

    /**
     * Builds a restaurant of [total] waiters in which [assigned] of them have really acted
     * earlier this evening (each seated and escorted one full group), so ids 1..[assigned] were
     * handed out by the restaurant's own counter and nobody is busy any more. Waiters are then
     * listed the worst way for tie-breaking: unassigned first, then by descending id. Table
     * [assigned] + 1 (4 seats) is free for the scenario under test.
     */
    fun primed(assigned: Int, total: Int, log: LogCapture): Primed {
        val waiters = MutableList(total) { Waiter() }
        val foh = frontOfHouse(tables(assigned, size = 10) + Table(assigned + 1, 4, TableType.COMMON), waiters)
        val groups = List(assigned) { casualGroup(id = 900 + it, size = 10) }
        groups.forEach { foh.processArrival(it, menu) }
        check(assignedIds(waiters) == (1..assigned).toList()) { "priming must hand out ids 1..$assigned" }
        groups.forEach { setDishes(it, DishStatus.EATEN) }
        foh.processEscorting()
        foh.processRatings(0, 0)
        foh.clearActionLoads()
        waiters.sortWith(compareBy<Waiter> { it.id != null }.thenByDescending { it.id })
        log.clear()
        return Primed(foh, waiters)
    }
}
