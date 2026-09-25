package waitingforfoodtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.helpers.EatingProcessor
import java.io.PrintWriter
import java.io.StringWriter

// Mirrors FrontOfHouse's private getServingPriority: REGULAR, then EVENT, then CASUAL.
private const val REGULAR_PRIORITY = 1
private const val EVENT_PRIORITY = 2
private const val CASUAL_PRIORITY = 3

/** shared setup for the F27 (customer waiting for food) tests: log capture, groups with orders, processors */
internal class WaitingFixtures {
    val output = StringWriter()

    /** the tick the orders of this fixture are placed in */
    val orderTick = 4

    init {
        Order.resetIds()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = orderTick
    }

    private val recipe = Recipe(1, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(), null)

    fun logLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    fun logLinesContaining(text: String): List<String> = logLines().filter { it.contains(text) }

    fun noEatingLines() = logLinesContaining("Restaurant No Eating")

    fun finishedEatingLines() = logLinesContaining("FOH Finished Eating")

    /** an order of [statuses.size] dishes with the given statuses, placed in the current tick */
    fun order(vararg statuses: DishStatus): Order =
        Order(statuses.map { status -> Dish(recipe).apply { this.status = status } })

    fun casual(id: Int, order: Order?, size: Int = order?.dishes?.size ?: 1): CasualGroup =
        CasualGroup(
            id = id,
            size = size,
            tableType = TableType.COMMON,
            visitingAt = orderTick,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = 0,
            ratingLikelihood = RatingLikelihood.NEVER
        ).also { it.currentOrder = order }

    fun regular(id: Int, order: Order?, size: Int = order?.dishes?.size ?: 1): RegularGroup =
        RegularGroup(
            id = id,
            size = size,
            tableType = TableType.COMMON,
            visitingAt = orderTick,
            foodPreferences = emptyList(),
            visitingStart = 1,
            visitingPeriod = 1,
            restaurantId = 1
        ).also { it.currentOrder = order }

    fun event(id: Int, order: Order?, size: Int = order?.dishes?.size ?: 1): EventGroup =
        EventGroup(
            id = id,
            size = size,
            tableType = TableType.COMMON,
            visitingAt = orderTick,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            eventEvening = 1,
            eventDishes = emptyMap()
        ).also { it.currentOrder = order }

    /** a delivery group (delivery distance > 0) that wanted its food at [visitingAt] */
    fun deliveryGroup(id: Int, order: Order, visitingAt: Int = orderTick): CasualGroup =
        CasualGroup(
            id = id,
            size = order.dishes.size,
            tableType = TableType.COMMON,
            visitingAt = visitingAt,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = 5,
            ratingLikelihood = RatingLikelihood.NEVER
        ).also { it.currentOrder = order }

    /** table id = group id unless [tables] says otherwise; priority: REGULAR before CASUAL, then id */
    fun eating(
        inHouse: List<CustomerGroup>,
        deliveryGroups: List<CustomerGroup> = emptyList(),
        tables: Map<CustomerGroup, Int> = inHouse.associateWith { it.id },
        delivered: MutableList<Int> = mutableListOf(),
    ): EatingProcessor = EatingProcessor(
        deliveryGroups = deliveryGroups,
        getInHouseGroups = { inHouse },
        getServingPriority = { group ->
            when (group) {
                is RegularGroup -> REGULAR_PRIORITY
                is EventGroup -> EVENT_PRIORITY
                else -> CASUAL_PRIORITY
            }
        },
        getAssignedTableId = { tables[it] },
        addCustomersDelivered = { delivered.add(it) },
    )

    fun advance(ticks: Int = 1) {
        Time.tick += ticks
    }

    /** moves the clock so that exactly [ticks] ticks have passed since the order was placed */
    fun atTicksSinceOrder(ticks: Int) {
        Time.tick = orderTick + ticks
    }
}
