package eventorderingtests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
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

/**
 * Shared setup for the P03 event-ordering tests. The menu has Bread (id 1) and Rice (id 2); `salt` and
 * `pepper` appear in no recipe, so a customer excluding them can still order, while a customer
 * excluding `flour` finds nothing on the menu and has to leave.
 */
internal object EventOrderingFixtures {
    private val flour = ingredient("flour")
    private val rice = ingredient("rice")
    val salt: Ingredient = ingredient("salt")
    val pepper: Ingredient = ingredient("pepper")
    val flourIngredient: Ingredient = flour
    val riceIngredient: Ingredient = rice
    private val bread = recipe(1, "Bread", flour)
    private val riceDish = recipe(2, "Rice", rice)
    val menu: List<Recipe> = listOf(bread, riceDish)

    /** the event's favourite dish is not on the menu, so it never overrides a customer's own choice */
    private const val UNAVAILABLE_EVENT_DISH = "Nothing"

    private fun ingredient(name: String) =
        Ingredient(name, MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 1000)

    private fun recipe(id: Int, name: String, ingredient: Ingredient) = Recipe(
        id = id,
        name = name,
        duration = 10,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(ingredient to 10),
        basicDishFor = null
    )

    fun countertop(orderQueue: ArrayDeque<Order> = ArrayDeque()): Countertop {
        val stock = Stock(listOf(flour, rice))
        val pantry = Pantry(
            inventory = mutableListOf(IngredientPackage(flour), IngredientPackage(rice)),
            supplier = Supplier(stock)
        )
        return Countertop(
            pantry = pantry,
            orderQueue = orderQueue,
            cooks = listOf(Cook(CookType.TOURNANT)),
            restaurantType = RestaurantType.EUROPEAN
        )
    }

    fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>, orderQueue: ArrayDeque<Order>): FrontOfHouse =
        FrontOfHouse(
            tables = tables,
            waiters = waiters,
            drivers = emptyList(),
            countertop = countertop(orderQueue)
        )

    /** A customer who ordered nothing special, and thus gets the highest-id recipe (Rice). */
    fun noPreference() = FoodPreference(emptyList(), emptyList(), emptyList())

    /** A customer who can still order Bread and Rice, but has two exclusions and so is asked first. */
    fun twoExclusions() = FoodPreference(listOf(salt, pepper), emptyList(), emptyList())

    /** A customer who refuses flour: with Bread as the only flour dish they only get Rice. */
    fun excludesFlour() = FoodPreference(listOf(flour), emptyList(), emptyList())

    /** A customer who cannot order anything from a menu of just flour and rice dishes. */
    fun excludesEverything() = FoodPreference(listOf(flour, rice), emptyList(), emptyList())

    fun eventGroup(id: Int, preferences: List<FoodPreference>): EventGroup = EventGroup(
        id = id,
        size = preferences.size,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = preferences,
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        eventEvening = 1,
        eventDishes = mapOf(RestaurantType.EUROPEAN to UNAVAILABLE_EVENT_DISH)
    ).also { it.currentRestaurantType = RestaurantType.EUROPEAN }

    fun regularGroup(id: Int, size: Int): RegularGroup = RegularGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 1,
        foodPreferences = List(size) { noPreference() },
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1
    )

    fun table(id: Int, size: Int): Table = Table(id = id, size = size, tableType = TableType.COMMON)

    fun waiter(id: Int? = null, currentLoad: Int = 0): Waiter =
        Waiter().also {
            it.id = id
            it.currentLoad = currentLoad
        }

    fun reserve(map: MutableMap<CustomerGroup, List<Table>>, group: CustomerGroup, vararg tables: Table) {
        tables.forEach { it.status = TableStatus.RESERVED }
        map[group] = tables.toList()
    }

    /** An [ArrivalProcessor] whose EVENT waiter recruiting follows the spec's SEATING rule. */
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
