package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import kotlin.math.ceil

/**
 * Distance per Tick
 */
private const val DPT = 5.0

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
            is CasualGroup -> getELigibleRestaurantsForCasuals(group)
            is EventGroup -> getELigibleRestaurantsForEvent(group)
        }
    }

    private fun getELigibleRestaurantsForCasuals(group: CasualGroup): Int? {
        if (group.deliveryDistance == 0) {
            return getEligibleRestaurantForDelivery(group)
        } else {
            val deliveryRests = mutableListOf<RestaurantStats>()
            val decisionTick = group.visitingAt - ceil(group.deliveryDistance.toDouble() / DPT).toInt() - 3
            restaurantStats.filter { it.restaurantType in group.restaurantTypes }.forEach { stats ->
                if (stats.openingTickStart < decisionTick && decisionTick < stats.openingTickEnd) {
                    deliveryRests.add(stats)
                }
            }
            deliveryRests.filter { it.availableDrivers > 0 }.filter { isDietaryCompatible(it, group) }
            val res =
                deliveryRests.maxWithOrNull(
                    compareBy<RestaurantStats> {
                        it.positiveRatings - it.negativeRatings
                    }.thenByDescending { it.restaurantId }
                )
            if (res != null) {
                res.availableDrivers = res.availableDrivers - 1
            }
            return res?.restaurantId
        }
    }
    private fun getEligibleRestaurantForDelivery(group: CasualGroup): Int? {
        val list = mutableListOf<RestaurantStats>()
        val visitingTick = group.visitingAt

        restaurantStats.filter { it.restaurantType in group.restaurantTypes }.forEach { stats ->
            if (stats.openingTickStart < visitingTick && visitingTick < stats.openingTickEnd) {
                list.add(stats)
            }
        }
        val llist = mutableListOf<RestaurantStats>()
        list.forEach { stats ->
            if (stats.availableSeats[group.tableType]!! >= group.size) {
                llist.plus(stats)
            }
        }
        llist.filter { isDietaryCompatible(it, group) }
        val res =
            llist.maxWithOrNull(
                compareBy<RestaurantStats> {
                    it.positiveRatings - it.negativeRatings
                }
                    .thenByDescending { it.restaurantId }
            )
        if (res != null) {
            res.availableSeats[group.tableType] = res.availableSeats[group.tableType]!! - group.size
        }
        return res?.restaurantId
    }

    private fun getELigibleRestaurantsForEvent(group: EventGroup): Int {
        val eventRests = mutableListOf<RestaurantStats>()
        val result = mutableListOf<RestaurantStats>()
        restaurantStats.filter { it.restaurantType in group.restaurantTypes }.filter { it.event }.forEach { stats ->
            if (stats.openingTickStart < group.visitingAt && group.visitingAt < stats.openingTickEnd) {
                eventRests.add(stats)
            }
        }
        eventRests.forEach {
            if (it.availableEventSeats.values.sum() >= group.size) {
                result.add(it)
            }
        }
        result.filter { isDietaryCompatible(it, group) }
        val res =
            result.maxWithOrNull(
                compareBy<RestaurantStats> {
                    it.positiveRatings - it.negativeRatings
                }
                    .thenByDescending { it.restaurantId }
            )
        if (res != null) {
            res.availableEventSeats[group.tableType] = res.availableEventSeats[group.tableType]!! - group.size
        }
        return res!!.restaurantId
    }

    private fun matchPreference(menu: List<Recipe>, fp: FoodPreference): Boolean {
        if (menu.isEmpty()) return false

        val excludedNames: Set<Ingredient> = fp.excludedIngredients.toSet()

        if (excludedNames.isEmpty()) return true

        return menu.any { recipe ->
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
