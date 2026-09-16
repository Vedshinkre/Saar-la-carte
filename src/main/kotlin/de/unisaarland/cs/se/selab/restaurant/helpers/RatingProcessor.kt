package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logCustomerRateRestaurant
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logRatingStatus

/** rating coordinator */
class RatingProcessor(
    private val deliveryGroups: MutableList<CustomerGroup>,
    private val turnedAwayGroups: List<CustomerGroup>,
    private val eventGroups: MutableList<EventGroup>,
    private val getInHouseGroups: () -> List<CustomerGroup>,
    private val getServingPriority: (CustomerGroup) -> Int,
    private val removeProcessedGroup: (CustomerGroup) -> Unit,
) {
    /**
     * Processes all customer groups eligible to give a rating in the current tick,
     * ordered by serving priority and ID.
     *
     * @param positiveRatings current number of positive ratings
     * @param negativeRatings current number of negative ratings
     * @return updated positive and negative rating counts
     */
    fun processRatings(
        positiveRatings: Int,
        negativeRatings: Int
    ): Pair<Int, Int> {
        var positive = positiveRatings
        var negative = negativeRatings
        val filteredInHouseGroups = getInHouseGroups().filter { it.customersRemainingInRestaurant == 0 }
        val filteredEventGroups = eventGroups.filter { it.customersRemainingInRestaurant == 0 }
        val filteredDeliveryGroups = deliveryGroups.filter {
            val order = it.currentOrder

            order == null || order.areAllDishesEaten() || order.dishes.any { dish ->
                dish.status == DishStatus.ABORTED
            }
        }
        var groupsGivingRatings = 0
        val groupsToRate = (
            filteredInHouseGroups +
                filteredDeliveryGroups +
                turnedAwayGroups +
                filteredEventGroups
            ).sortedWith(
            compareBy({ getServingPriority(it) }, { it.id })
        )
        groupsToRate.forEach { group ->
            val oldPositive = positive
            val oldNegative = negative

            val result = rate(
                group,
                positive,
                negative
            )

            positive = result.first
            negative = result.second
            removeProcessedGroup(group)
            if (positive != oldPositive || negative != oldNegative) {
                groupsGivingRatings++
            }
        }
        logRatingStatus(groupsGivingRatings)
        return Pair(positive, negative)
    }

    /**
     * Rates a customer group and updates the rating counters.
     * When closing is true, unfinished groups receive a negative experience.
     *
     * @param group customer group to rate
     * @param positiveRatings current number of positive ratings
     * @param negativeRatings current number of negative ratings
     * @param closing whether the rating is processed at closing
     * @return updated positive and negative rating counts
     */
    fun rate(
        group: CustomerGroup,
        positiveRatings: Int,
        negativeRatings: Int,
        closing: Boolean = false
    ): Pair<Int, Int> {
        var positive = positiveRatings
        var negative = negativeRatings
        if (closing) {
            val order = group.currentOrder

            if (order == null || !order.areAllDishesEaten()) {
                group.experience = ExperienceType.NEGATIVE
            }
        }
        when (group.determineRating()) {
            RatingType.POSITIVE -> {
                positive++

                logCustomerRateRestaurant(
                    group.id,
                    RatingType.POSITIVE,
                    positive,
                    negative
                )
            }

            RatingType.NEGATIVE -> {
                negative++

                logCustomerRateRestaurant(
                    group.id,
                    RatingType.NEGATIVE,
                    positive,
                    negative
                )
            }

            RatingType.NO_RATING -> {}
        }

        return Pair(positive, negative)
    }
}
