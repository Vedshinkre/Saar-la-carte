package eveningclosetests

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table

/**
 * Shared setup:
 * a small [Restaurant] builder
 * a recipe slow enough that a dish ordered from it never finishes cooking within one
 * 24-tick evening, so a group that orders it is still mid-meal whenever the restaurant to closes
 */
internal object EveningCloseFixtures {

    private const val INGREDIENT_AMOUNT_PER_DISH = 10
    private const val PACKAGE_VOLUME = 1000
    private const val BEST_BEFORE_EVENINGS = 5

    /** minutes; ceil(duration / 10) - 1 remaining ticks, far longer than one 24-tick evening */
    private const val NEVER_FINISHES_DURATION = 1000

    private val flour = Ingredient(
        name = "flour",
        unit = MeasurementUnit.G,
        bestBefore = BEST_BEFORE_EVENINGS,
        initialPackagingVolume = PACKAGE_VOLUME
    )

    /** the only recipe on [slowMenu]; always orderable, never finishes cooking in one evening */
    val slowRecipe: Recipe = Recipe(
        id = 1,
        name = "SlowDish",
        duration = NEVER_FINISHES_DURATION,
        cookType = listOf(CookType.TOURNANT),
        ingredients = mutableMapOf(flour to INGREDIENT_AMOUNT_PER_DISH),
        basicDishFor = null
    )

    val slowMenu: List<Recipe> = listOf(slowRecipe)

    fun stock(): Stock = Stock(listOf(flour))

    fun noPreference(): FoodPreference = FoodPreference(emptyList(), emptyList(), emptyList())

    fun table(id: Int, size: Int = 2): Table = Table(id = id, size = size, tableType = TableType.COMMON)

    fun cook(): Cook = Cook(CookType.TOURNANT)

    /** a trip long enough that the driver never arrives during a single test's ticks */
    const val LARGE_TRIP = 100

    fun deliveringDriver(id: Int? = 1, ticksToDest: Int = LARGE_TRIP): Driver = Driver().apply {
        this.id = id
        state = DriverState.DELIVERING
        this.ticksToDest = ticksToDest
        totalTripTicks = ticksToDest * 2
    }

    fun returningDriver(id: Int? = 1, ticksToDest: Int): Driver = Driver().apply {
        this.id = id
        state = DriverState.RETURNING
        this.ticksToDest = ticksToDest
        totalTripTicks = ticksToDest * 2
    }

    fun regularGroup(id: Int, size: Int = 2, visitingAt: Int = 1): RegularGroup = RegularGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = List(size) { noPreference() },
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1
    )

    fun restaurant(
        openingTickStart: Int = 1,
        openingTickEnd: Int = 24,
        staff: RestaurantStaff = RestaurantStaff(mutableListOf(), mutableListOf(), mutableListOf()),
        tables: List<Table> = emptyList(),
        menu: List<Recipe> = emptyList(),
        stock: Stock = Stock(emptyList())
    ): Restaurant = Restaurant(
        restaurantStats = RestaurantStats(
            restaurantId = 1,
            restaurantType = RestaurantType.EUROPEAN,
            openingTickStart = openingTickStart,
            openingTickEnd = openingTickEnd,
            event = false,
            positiveRatings = 0,
            negativeRatings = 0,
            menu = menu
        ),
        name = "Testaurant",
        staff = staff,
        tables = tables,
        stock = stock
    )
}
