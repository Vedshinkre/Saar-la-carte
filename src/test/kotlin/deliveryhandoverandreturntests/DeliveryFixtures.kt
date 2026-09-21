package deliveryhandoverandreturntests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
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
import de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor
import java.io.PrintWriter
import java.io.StringWriter

/** shared setup for the F20 hand-over, return and eating tests */
class DeliveryFixtures {
    val output = StringWriter()

    init {
        Order.resetIds()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 4
    }

    private val recipe = Recipe(1, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(), null)

    fun logLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    fun logLinesContaining(text: String): List<String> = logLines().filter { it.contains(text) }

    /** a delivery group whose order holds [dishCount] dishes in the given [status] */
    fun deliveryGroup(
        id: Int,
        dishCount: Int = 2,
        status: DishStatus = DishStatus.COOKED,
        distance: Int = 5,
        visitingAt: Int = 10,
    ): CasualGroup {
        val group = CasualGroup(
            id = id,
            size = dishCount,
            tableType = TableType.COMMON,
            visitingAt = visitingAt,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = distance,
            ratingLikelihood = RatingLikelihood.NEVER
        )
        group.currentOrder = Order(List(dishCount) { Dish(recipe).apply { this.status = status } })
        return group
    }

    fun serving(
        waiters: List<Waiter>,
        drivers: List<Driver>,
        groups: List<CustomerGroup>,
    ): ServingProcessor {
        var nextWaiterId = waiters.mapNotNull { it.id }.maxOrNull() ?: 0
        return ServingProcessor(
            waiters = waiters,
            drivers = drivers,
            deliveryGroups = groups,
            getInHouseGroups = { emptyList() },
            waiterFor = { null },
            getServingPriority = { 2 },
            getAssignedTableId = { null },
            recruitWaitersForEventGroup = { _, _ -> emptyList() },
            getNextWaiterId = { ++nextWaiterId }
        )
    }
}
