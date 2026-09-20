package casualtablemergingtests

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
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
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.ArrivalProcessor

private const val A_LOT = 1000000

/** shared setup for unit and integration tests */
internal object CasualTableMergingFixtures {
    private val rice = Ingredient("rice", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = A_LOT)
    private val stew = Recipe(
        id = 1,
        name = "Stew",
        duration = 1,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(rice to 1),
        basicDishFor = null
    )
    val menu: List<Recipe> = listOf(stew)

    fun countertop(orderQueue: ArrayDeque<Order> = ArrayDeque()): Countertop {
        val stock = Stock(listOf(rice))
        val pantry = Pantry(
            inventory = mutableListOf(IngredientPackage(rice)),
            supplier = Supplier(stock)
        )
        return Countertop(
            pantry = pantry,
            orderQueue = orderQueue,
            cooks = listOf(Cook(CookType.TOURNANT)),
            restaurantType = RestaurantType.EUROPEAN
        )
    }

    /** a customer with no exclusions who always finds Stew on the menu */
    fun fed(): FoodPreference = FoodPreference(emptyList(), emptyList(), emptyList())

    /** a customer who excludes the only ingredient on the menu and so never orders */
    fun starving(): FoodPreference = FoodPreference(listOf(rice), emptyList(), emptyList())

    fun table(id: Int, size: Int, type: TableType = TableType.COMMON): Table =
        Table(id = id, size = size, tableType = type)

    fun waiter(id: Int? = null, currentLoad: Int = 0): Waiter = Waiter().also {
        it.id = id
        it.currentLoad = currentLoad
    }

    fun casualGroup(
        id: Int,
        size: Int,
        tableType: TableType = TableType.COMMON,
        visitingAt: Int = 1,
        preferences: List<FoodPreference> = List(size) { fed() }
    ): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = tableType,
        visitingAt = visitingAt,
        foodPreferences = preferences,
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = 0,
        ratingLikelihood = RatingLikelihood.NEVER
    )

    fun regularGroup(id: Int, size: Int, tableType: TableType = TableType.COMMON, visitingAt: Int = 1): RegularGroup =
        RegularGroup(
            id = id,
            size = size,
            tableType = tableType,
            visitingAt = visitingAt,
            foodPreferences = List(size) { fed() },
            visitingStart = 1,
            visitingPeriod = 1,
            restaurantId = 1
        )

    fun eventGroup(id: Int, size: Int, tableType: TableType = TableType.COMMON, visitingAt: Int = 1): EventGroup =
        EventGroup(
            id = id,
            size = size,
            tableType = tableType,
            visitingAt = visitingAt,
            foodPreferences = List(size) { fed() },
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            eventEvening = 1,
            eventDishes = emptyMap()
        ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    /** a direct [ArrivalProcessor], bypassing [FrontOfHouse], for unit-level tests */
    fun processor(
        tables: List<Table>,
        waiters: List<Waiter>,
        customerToTable: MutableMap<CustomerGroup, List<Table>> = mutableMapOf(),
        turnedAwayGroups: MutableList<CustomerGroup> = mutableListOf(),
        orderQueue: ArrayDeque<Order> = ArrayDeque()
    ): ArrivalProcessor {
        var lastId = 0
        return ArrivalProcessor(
            tables = tables,
            waiters = waiters,
            customerToTable = customerToTable,
            inHouseGroupsToWaiter = mutableMapOf(),
            turnedAwayGroups = turnedAwayGroups,
            eventGroups = mutableListOf(),
            countertop = countertop(orderQueue),
            recruitWaitersForEventGroup = { _, _ -> emptyList() },
            getNextWaiterId = { ++lastId }
        )
    }

    fun frontOfHouse(
        tables: List<Table>,
        waiters: List<Waiter>,
        orderQueue: ArrayDeque<Order> = ArrayDeque()
    ): FrontOfHouse = FrontOfHouse(
        tables = tables,
        waiters = waiters,
        drivers = emptyList(),
        countertop = countertop(orderQueue)
    )

    /** marks tables RESERVED and records them for [group], simulating an earlier reservation */
    fun reserve(map: MutableMap<CustomerGroup, List<Table>>, group: CustomerGroup, vararg tables: Table) {
        tables.forEach { it.status = TableStatus.RESERVED }
        map[group] = tables.toList()
    }
}
