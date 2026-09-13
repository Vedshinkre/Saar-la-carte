package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType

// TODO: move this to a common constants file
private const val DISTANCE_PER_TICK = 5.0

/** extra ticks a delivery order factors in for cooking, in addition to travel time */
private const val DELIVERY_COOKING_TICKS = 3

/** Represents a casual customer group. */
class CasualGroup(
    id: Id,
    size: Int,
    tableType: TableType,
    visitingAt: Tick, //I also want to access this
    foodPreferences: List<FoodPreference>,
    val restaurantTypes: List<RestaurantType>,
    private val visitingEvenings: List<Evening>,
    val deliveryDistance: Int,
    val ratingLikelihood: RatingLikelihood
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences) {

    // TODO: make visitingAt public
    // TODO: isVisitingTonight(), isVisitingThisTick(), deliveryOrderTick(), determineRating()
    // isVisitingThisTick() = check if currentTick equals visitingAt (if no delivery) else deliveryOrderTick (according to spec calculation)

    /** get if the customer wants delivery */
    fun getWantsDelivery(): Boolean = deliveryDistance > 0
}
