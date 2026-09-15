package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logCustomerRateRestaurant
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logRatingStatus

/** rating coordinator */
class RatingProcessor(
    private val deliveryGroups: List<CustomerGroup>,
    private val turnedAwayGroups: List<CustomerGroup>,
    private val getInHouseGroups: () -> List<CustomerGroup>,
    private val getServingPriority: (CustomerGroup) -> Int,
) {
    /**
     * Processes customer ratings and updates the positive and negative rating counts.
     * @param positiveRatings current number of positive ratings
     * @param negativeRatings current number of negative ratings
     * @return updated Positive and Negative rating counts.
     */
    fun processRatings(
        positiveRatings: Int,
        negativeRatings: Int
    ): Pair<Int, Int> {
        var positive = positiveRatings
        var negative = negativeRatings
        val filteredInHouseGroups = getInHouseGroups().filter { it.customersRemainingInRestaurant == 0 }

        val filteredDeliveryGroups = deliveryGroups.filter {
            val order = it.currentOrder

            order == null || order.areAllDishesEaten() || order.dishes.any { dish ->
                dish.status == DishStatus.ABORTED
            }
        }
        var groupsGivingRatings = 0
        val groupsToRate = (filteredInHouseGroups + filteredDeliveryGroups + turnedAwayGroups).sortedWith(
            compareBy({ getServingPriority(it) }, { it.id })
        )
        groupsToRate.forEach {
            when (it.determineRating()) {
                RatingType.POSITIVE -> {
                    positive++
                    groupsGivingRatings++

                    logCustomerRateRestaurant(
                        it.id,
                        RatingType.POSITIVE,
                        positive,
                        negative
                    )
                }

                RatingType.NEGATIVE -> {
                    negative++
                    groupsGivingRatings++

                    logCustomerRateRestaurant(
                        it.id,
                        RatingType.NEGATIVE,
                        positive,
                        negative
                    )
                }

                RatingType.NO_RATING -> {}
            }
        }
        logRatingStatus(groupsGivingRatings)
        return Pair(positive, negative)
    }
}
