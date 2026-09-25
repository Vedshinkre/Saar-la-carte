package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import kotlin.math.absoluteValue
import kotlin.math.sign

/** Represents a casual customer group. */
class CasualGroup(
    id: Id,
    size: Int,
    tableType: TableType,
    visitingAt: Tick,
    foodPreferences: List<FoodPreference>,
    val restaurantTypes: List<RestaurantType>,
    val visitingEvenings: List<Evening>,
    val deliveryDistance: Int,
    val ratingLikelihood: RatingLikelihood
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences) {

    /** Integer division rounded up (towards positive infinity). */
    fun Int.ceilDiv(other: Int): Int {
        return this.floorDiv(other) + this.rem(other).sign.absoluteValue
    }

    /**
     * a CASUAL group orders for delivery iff `deliveryDistance > 0`; `"deliveryDistance": 0` with no
     * `tableType` is an ordinary in-restaurant group
     */
    var wantsDelivery: Boolean = deliveryDistance > 0

    /**
     * Whether the group acts in the current tick: a dine-in group on its visiting tick, a delivery
     * group on its ordering tick (see [getDeliveryOrderTick]).
     */
    override fun isVisitingThisTick(): Boolean {
        if (wantsDelivery) {
            val orderingTick = getDeliveryOrderTick()
            return orderingTick == Time.tick
        }

        return Time.tick == visitingAt
    }

    /** Whether the current evening is one of the group's visiting evenings. */
    override fun isVisitingTonight(): Boolean {
        return visitingEvenings.contains(Time.evening)
    }

    /**
     * The tick a delivery group orders in: early enough for three ticks of cooking and the drive
     * (distance / 5, rounded up) before its visiting tick.
     */
    private fun getDeliveryOrderTick(): Tick {
        val cookingTicks = Constants.DELIVERY_COOKING_TICKS
        val deliveryTicks = deliveryDistance.ceilDiv(Constants.DISTANCE_PER_TICK)
        val orderTick = visitingAt - cookingTicks - deliveryTicks
        return orderTick
    }

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
