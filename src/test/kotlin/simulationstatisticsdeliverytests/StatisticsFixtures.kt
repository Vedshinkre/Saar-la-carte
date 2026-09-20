package simulationstatisticsdeliverytests

import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.helpers.EatingProcessor
import de.unisaarland.cs.se.selab.restaurant.helpers.RatingProcessor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val DELIVERY_PRIORITY = 2
private const val LAST_TICK = 24

/** Shared setup for the F07 statistics tests that cover delivered customers, ratings and the final log. */
internal object StatisticsFixtures {
    private val soup = Recipe(1, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(), null)

    fun order(status: DishStatus, dishes: Int = 2): Order =
        Order(List(dishes) { Dish(soup).apply { this.status = status } })

    fun deliveryGroup(
        id: Int,
        size: Int = 2,
        distance: Int = 5,
        visitingAt: Int = 10,
        likelihood: RatingLikelihood = RatingLikelihood.NEVER
    ): CasualGroup = CasualGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = visitingAt,
        foodPreferences = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        restaurantTypes = listOf(RestaurantType.EUROPEAN),
        visitingEvenings = listOf(1),
        deliveryDistance = distance,
        ratingLikelihood = likelihood
    )

    /** A driver already on the way to the group, so driving can be tested without the preparation step. */
    fun drivingDriver(group: CasualGroup, order: Order, oneWayTicks: Int): Driver {
        group.currentOrder = order
        return Driver().apply {
            id = 1
            targetGroup = group
            currentOrder = order
            state = DriverState.DELIVERING
            ticksToDest = oneWayTicks
            totalTripTicks = oneWayTicks * 2
            tripDistance = group.deliveryDistance
        }
    }

    fun eatingProcessor(groups: List<CustomerGroup>, onDelivered: (Int) -> Unit): EatingProcessor =
        EatingProcessor(
            deliveryGroups = groups,
            getInHouseGroups = { emptyList() },
            getServingPriority = { DELIVERY_PRIORITY },
            getAssignedTableId = { null },
            addCustomersDelivered = onDelivered
        )

    fun ratingProcessor(): RatingProcessor = RatingProcessor(
        deliveryGroups = mutableListOf(),
        turnedAwayGroups = emptyList(),
        eventGroups = mutableListOf(),
        getInHouseGroups = { emptyList() },
        getServingPriority = { DELIVERY_PRIORITY },
        removeProcessedGroup = {}
    )

    fun stats(id: Int, positive: Int = 0, negative: Int = 0): RestaurantStats =
        RestaurantStats(id, RestaurantType.EUROPEAN, 1, LAST_TICK, false, positive, negative, emptyList())

    /** A restaurant whose statistics getters return the given numbers, for the final statistics log. */
    fun restaurantMock(stats: RestaurantStats, cooked: Int, served: Int, delivered: Int): Restaurant =
        mock {
            on { getRestaurantStats() } doReturn stats
            on { getNumberOfCookedMeals() } doReturn cooked
            on { getNumberOfCustomersServed() } doReturn served
            on { getNumberOfCustomersDelivered() } doReturn delivered
        }
}
