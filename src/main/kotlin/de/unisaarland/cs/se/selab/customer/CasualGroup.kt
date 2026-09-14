package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType

// move this to a common constants file
private const val DISTANCE_PER_TICK = 5.0

/** extra ticks a delivery order factors in for cooking, in addition to travel time */
private const val DELIVERY_COOKING_TICKS = 3

/** Represents a casual customer group. */
class CasualGroup(
    id: Id,
    size: Int,
    tableType: TableType,
    visitingAt: Tick,
    foodPreferences: List<FoodPreference>,
    val restaurantTypes: List<RestaurantType>,
    private val visitingEvenings: List<Evening>,
    val deliveryDistance: Int,
    val ratingLikelihood: RatingLikelihood
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences) {

    /**
     * returns true if they want to visit a restaurant tonight
     */
    fun isVisitingTonight(): Boolean {
        return true
    }

    /**
     * returns true if they are coming to the restaurant this tick
     */
    fun isVisitingThisTick(): Boolean {
        deliverOrderTick()
        return true
    }

    /**
     * returns the tick at which they will put the order to the restaurant
     */
    private fun deliverOrderTick(): Tick {
        return Time.tick
    }
    // isVisitingThisTick() = check if currentTick equals visitingAt
    // (if no delivery) else deliveryOrderTick (according to spec calculation)

    /** get if the customer wants delivery */
    fun getWantsDelivery(): Boolean = deliveryDistance > 0

    override fun determineRating(): RatingType {
        return when (ratingLikelihood) {
            RatingLikelihood.NEVER -> {
                RatingType.NO_RATING
            }

            RatingLikelihood.SOME -> {
                when (experience) {
                    ExperienceType.NEGATIVE -> RatingType.NEGATIVE
                    ExperienceType.NEUTRAL -> RatingType.NO_RATING
                    ExperienceType.POSITIVE -> RatingType.POSITIVE
                }
            }

            RatingLikelihood.ALWAYS -> {
                when (experience) {
                    ExperienceType.NEGATIVE -> RatingType.NEGATIVE
                    ExperienceType.NEUTRAL -> RatingType.POSITIVE
                    ExperienceType.POSITIVE -> RatingType.POSITIVE
                }
            }
        }
    }
}
