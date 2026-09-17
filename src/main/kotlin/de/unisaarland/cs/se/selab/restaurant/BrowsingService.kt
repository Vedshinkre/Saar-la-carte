package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe

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
        } else {
            val deliveryRests = mutableListOf<RestaurantStats>()

            restaurantStats.filter { it.restaurantType in group.restaurantTypes }.forEach { stats ->
                if (stats.isOpen()) {
                    deliveryRests.add(stats)
                }
            }
            deliveryRests.filter { it.availableDrivers > 0 }.filter { isDietaryCompatible(it, group) }
            val res = deliveryRests.maxWithOrNull(
                compareBy<RestaurantStats> {
                    it.positiveRatings - it.negativeRatings
                }.thenByDescending { it.restaurantId }
            )
            if (res != null) {
                res.availableDrivers = res.availableDrivers - 1
                return res.restaurantId
            }
            return null
        }
    }

    private fun getEligibleRestaurantForDineIn(group: CasualGroup): Int? {
        val list = mutableListOf<RestaurantStats>()

        restaurantStats.filter { it.restaurantType in group.restaurantTypes }.forEach { stats ->
            if (stats.isOpen()) {
                list.add(stats)
            }
        }
        val llist = mutableListOf<RestaurantStats>()
        list.forEach { stats ->

            val numberOfAvailabeSeats = stats.availableSeats[group.tableType]
            if (numberOfAvailabeSeats != null) {
                if (numberOfAvailabeSeats >= group.size) {
                    llist.add(stats)
                }
            }
        }
        llist.filter { isDietaryCompatible(it, group) }
        val res = llist.maxWithOrNull(
            compareBy<RestaurantStats> {
                it.positiveRatings - it.negativeRatings
            }.thenByDescending { it.restaurantId }
        )
        if (res != null) {
            res.availableSeats[group.tableType] = res.availableSeats[group.tableType]!! - group.size
            return res.restaurantId
        } else {
            return null
        }
    }

    private fun getELigibleRestaurantsForEvent(group: EventGroup): Int? {
        val eventRests = mutableListOf<RestaurantStats>()
        val result = mutableListOf<RestaurantStats>()
        restaurantStats.filter { it.restaurantType in group.restaurantTypes }.filter { it.event }.forEach { stats ->
            if (stats.isOpen()) {
                eventRests.add(stats)
            }
        }
        eventRests.forEach {
            if (it.availableEventSeats.values.sum() >= group.size) {
                result.add(it)
            }
        }
        result.filter { isDietaryCompatible(it, group) }
        val res = result.maxWithOrNull(
            compareBy<RestaurantStats> {
                it.positiveRatings - it.negativeRatings
            }.thenByDescending { it.restaurantId }
        )
        if (res != null) {
            res.availableEventSeats[group.tableType] = res.availableEventSeats[group.tableType]!! - group.size
            return res.restaurantId
        }

        return null
    }

    private fun matchPreference(menu: List<Recipe>, fp: FoodPreference): Boolean {
        if (menu.isEmpty()) return false

        val excludedNames: Set<Ingredient> = fp.excludedIngredients.toSet()

        return excludedNames.isEmpty() || menu.any { recipe ->
            recipe.ingredients.keys.none { ingredient ->
                ingredient in excludedNames
            }
        }
    }

    private fun isDietaryCompatible(r: RestaurantStats, group: CustomerGroup): Boolean {
        return group.foodPreferences.all {
            matchPreference(r.menu, it)
        }
    }
}
