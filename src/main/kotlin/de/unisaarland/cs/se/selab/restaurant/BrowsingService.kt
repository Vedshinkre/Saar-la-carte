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
    fun getEligibleRestaurants(group: CustomerGroup): List<RestaurantStats> {
        require(group is CasualGroup || group is EventGroup) {
            "Group must be either CasualGroup or EventGroup"
        }
        when (group) {
            is CasualGroup -> return getELigibleRestaurantsForCasuals(group)
            is EventGroup -> return getELigibleRestaurantsForEvent(group)
        }
    }
    private fun getELigibleRestaurantsForCasuals(group: CasualGroup): MutableList<RestaurantStats> {
        var list = mutableListOf<RestaurantStats>()
        if (group.deliveryDistance == 0) {
            val visitingTick = group.getVisitingAt()

            restaurantStats.filter { it.restaurantType in group.restaurantTypes }.forEach { stats ->
                if (
                    stats.openingTickStart < visitingTick &&
                    visitingTick < stats.openingTickEnd
                ) {
                    list.add(stats)
                }
            }
            var llist = mutableListOf<RestaurantStats>()
            list.forEach { stats ->
                if (stats.availableSeats[group.tableType]!! >= group.size) {
                    llist.plus(stats)
                }
            }
            llist.filter { isDietaryCompatible(it, group) }
            return llist
        } else {
            var deliveryRests = mutableListOf<RestaurantStats>()
            val decisionTick = group.visitingAt - ceil(group.deliveryDistance.toDouble() / DPT).toInt() - 3
            restaurantStats.filter { it.restaurantType in group.restaurantTypes }.forEach { stats ->
                if (
                    stats.openingTickStart < decisionTick &&
                    decisionTick < stats.openingTickEnd
                ) {
                    deliveryRests.add(stats)
                }
            }
            deliveryRests.filter { it.availableDrivers > 0 }.filter { isDietaryCompatible(it, group) }
            return deliveryRests
        }
    }
    private fun getELigibleRestaurantsForEvent(group: EventGroup): List<RestaurantStats> {
        var eventRests = mutableListOf<RestaurantStats>()
        var result = mutableListOf<RestaurantStats>()
        restaurantStats.filter { it.restaurantType in group.restaurantTypes }.filter { it.event }.forEach { stats ->
            if (
                stats.openingTickStart < group.visitingAt &&
                group.visitingAt < stats.openingTickEnd
            ) {
                eventRests.add(stats)
            }
        }
        eventRests.forEach {
            if (it.availableEventSeats.values.sum() >= group.size) {
                result.add(it)
            }
        }
        result.filter { isDietaryCompatible(it, group) }
        return result
    }

    private fun matchPreference(menu: List<Recipe>, fp: FoodPreference): Boolean {
        if (menu.isEmpty()) return false

        val excludedNames: Set<Ingredient> = fp.excludedIngredients.toSet()

        if (excludedNames.isEmpty()) return true

        return menu.any { recipe ->
            recipe.getIngredients().keys.none { ingredient ->
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
