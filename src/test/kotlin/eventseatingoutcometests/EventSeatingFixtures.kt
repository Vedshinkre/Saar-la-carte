package eventseatingoutcometests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.ArrivalProcessor

/** Shared setup for the P02 event-seating outcome tests (a single "Bread" dish everyone can order). */
internal object EventSeatingFixtures {
    private val flour = Ingredient("flour", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)
    private val breadRecipe = Recipe(
        id = 1,
        name = "Bread",
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(flour to 10),
        basicDishFor = null
    )
    val menu: List<Recipe> = listOf(breadRecipe)

    fun countertop(): Countertop {
        val stock = Stock(listOf(flour))
        val pantry = Pantry(inventory = mutableListOf(IngredientPackage(flour)), supplier = Supplier(stock))
        return Countertop(
            pantry = pantry,
            orderQueue = ArrayDeque(),
            cooks = listOf(Cook(CookType.TOURNANT)),
            restaurantType = RestaurantType.EUROPEAN
        )
    }

    fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse =
        FrontOfHouse(tables = tables, waiters = waiters, drivers = emptyList(), countertop = countertop())

    private fun preferences(count: Int): List<FoodPreference> =
        List(count) { FoodPreference(emptyList(), emptyList(), emptyList()) }

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

    fun regularGroup(id: Int, size: Int): RegularGroup = RegularGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = preferences(size),
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1
    )

    fun casualGroup(id: Int, size: Int): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = preferences(size),
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.SOME
    )

    fun table(id: Int, size: Int): Table = Table(id = id, size = size, tableType = TableType.COMMON)

    /** A waiter with a fixed id and current load, so the order the manager picks them in is observable. */
    fun waiter(id: Int? = null, currentLoad: Int = 0): Waiter =
        Waiter().also {
            it.id = id
            it.currentLoad = currentLoad
        }

    /** Marks the tables as reserved for the group, the way the preparation phase does before the evening. */
    fun reserve(map: MutableMap<CustomerGroup, List<Table>>, group: CustomerGroup, vararg tables: Table) {
        tables.forEach { it.status = TableStatus.RESERVED }
        map[group] = tables.toList()
    }

    /** Builds an [ArrivalProcessor] whose EVENT waiter recruiting follows the spec's SEATING rule. */
    fun processor(
        tables: List<Table>,
        waiters: List<Waiter>,
        customerToTable: MutableMap<CustomerGroup, List<Table>>,
        turnedAwayGroups: MutableList<CustomerGroup> = mutableListOf(),
        eventGroups: MutableList<EventGroup> = mutableListOf()
    ): ArrivalProcessor {
        var lastId = 0
        return ArrivalProcessor(
            tables = tables,
            waiters = waiters,
            customerToTable = customerToTable,
            inHouseGroupsToWaiter = mutableMapOf(),
            turnedAwayGroups = turnedAwayGroups,
            eventGroups = eventGroups,
            countertop = countertop(),
            recruitWaitersForEventGroup = { _, _ ->
                waiters.filter { it.getTickLoad(ActionType.SEAT) < Constants.ACTION_LIMIT }
                    .sortedByDescending { it.currentLoad }
            },
            getNextWaiterId = { ++lastId }
        )
    }
}
