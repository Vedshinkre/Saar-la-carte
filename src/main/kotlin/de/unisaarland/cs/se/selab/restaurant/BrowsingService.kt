package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.food.Recipe

const val FIVE = 5.0

/**
 * manages browsingService
 */

class BrowsingService(private val restaurantStats: List<RestaurantStats>) {
    /**
     * gives eligible Restaurants
     */
    fun getEligibleRestaurants(group: CustomerGroup): Int? {
        require(group is CasualGroup || group is EventGroup) {
            "Group must be either CasualGroup or EventGroup"
        }
        return when (group) {
            is CasualGroup -> getEligibleRestaurantsForCasuals(group)
            is EventGroup -> getELigibleRestaurantsForEvent(group)
        }
    }

    private fun getEligibleRestaurantsForCasuals(group: CasualGroup): Int? {
        if (!group.wantsDelivery) {
            return getEligibleRestaurantForDineIn(group)
        }
        val openRestaurants = collectOpenRestaurants(group)
        val travelTicks = kotlin.math.ceil(group.deliveryDistance.toDouble() / FIVE).toInt()
        val cookingFinishTick = group.visitingAt - travelTicks - 3

        val deliveryCandidates = mutableListOf<RestaurantStats>()
        for (stats in openRestaurants) {
            if (stats.availableDrivers > 0 && cookingFinishTick <= stats.openingTickEnd) {
                deliveryCandidates.add(stats)
            }
        }

        val dietaryCandidates = filterDietaryCompatible(deliveryCandidates, group)
        val best = pickBestRestaurant(dietaryCandidates)
        if (best != null) {
            best.availableDrivers = best.availableDrivers - 1
            return best.restaurantId
        }
        return null
    }

    private fun getEligibleRestaurantForDineIn(group: CasualGroup): Int? {
        val openRestaurants = collectOpenRestaurants(group)

        val seatCandidates = mutableListOf<RestaurantStats>()
        for (stats in openRestaurants) {
            val numberOfAvailableSeats = stats.availableSeats[group.tableType]
            if (numberOfAvailableSeats != null && numberOfAvailableSeats >= group.size) {
                seatCandidates.add(stats)
            }
        }

        val dietaryCandidates = filterDietaryCompatible(seatCandidates, group)
        val best = pickBestRestaurant(dietaryCandidates)
        if (best != null) {
            val remainingSeats = best.availableSeats[group.tableType] ?: 0
            best.availableSeats[group.tableType] = remainingSeats - group.size
            return best.restaurantId
        }
        return null
    }

    private fun getELigibleRestaurantsForEvent(group: EventGroup): Int? {
        val eventRestaurants = mutableListOf<RestaurantStats>()
        for (stats in restaurantStats) {
            if (stats.restaurantType in group.restaurantTypes && stats.event && stats.isOpenAt(
                    group.visitingAt, group.eventEvening
                )
            ) {
                eventRestaurants.add(stats)
            }
        }

        val seatCandidates = mutableListOf<RestaurantStats>()
        for (stats in eventRestaurants) {
            val numberOfAvailableSeats = stats.availableEventSeats[group.tableType] ?: 0
            if (numberOfAvailableSeats >= group.size) {
                seatCandidates.add(stats)
            }
        }

        val dietaryCandidates = filterDietaryCompatible(seatCandidates, group)
        val best = pickBestRestaurant(dietaryCandidates)
        if (best != null) {
            val remainingSeats = best.availableEventSeats[group.tableType] ?: 0
            best.availableEventSeats[group.tableType] = remainingSeats - group.size
            return best.restaurantId
        }
        return null
    }

    private fun collectOpenRestaurants(group: CasualGroup): List<RestaurantStats> {
        val openRestaurants = mutableListOf<RestaurantStats>()
        for (stats in restaurantStats) {
            if (stats.restaurantType in group.restaurantTypes && stats.isOpen()) {
                openRestaurants.add(stats)
            }
        }
        return openRestaurants
    }

    private fun filterDietaryCompatible(
        candidates: List<RestaurantStats>,
        group: CustomerGroup
    ): List<RestaurantStats> {
        val compatible = mutableListOf<RestaurantStats>()
        for (stats in candidates) {
            if (isDietaryCompatible(stats, group)) {
                compatible.add(stats)
            }
        }
        return compatible
    }

    /**
     * Best restaurant = highest (positive - negative) ratings; ties go to the smaller restaurant id.
     */
    private fun pickBestRestaurant(candidates: List<RestaurantStats>): RestaurantStats? {
        var best: RestaurantStats? = null
        for (candidate in candidates) {
            if (best == null || isBetter(candidate, best)) {
                best = candidate
            }
        }
        return best
    }

    private fun isBetter(candidate: RestaurantStats, best: RestaurantStats): Boolean {
        val candidateScore = candidate.positiveRatings - candidate.negativeRatings
        val bestScore = best.positiveRatings - best.negativeRatings
        if (candidateScore != bestScore) {
            return candidateScore > bestScore
        }
        return candidate.restaurantId < best.restaurantId
    }

    private fun matchPreference(menu: List<Recipe>, fp: FoodPreference): Boolean {
        if (menu.isEmpty()) return false

        val excludedNames: Set<String> = fp.excludedIngredients.map { it.name }.toSet()

        return excludedNames.isEmpty() || menu.any { recipe ->
            recipe.ingredients.keys.none { ingredient -> ingredient.name in excludedNames }
        }
    }

    private fun isDietaryCompatible(r: RestaurantStats, group: CustomerGroup): Boolean {
        for (preference in group.foodPreferences) {
            if (!matchPreference(r.menu, preference)) {
                return false
            }
        }
        return true
    }
}
