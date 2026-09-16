package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType

/** Represents an event customer group. */
class EventGroup(
    id: Id,
    size: Int,
    tableType: TableType,
    visitingAt: Tick,
    foodPreferences: List<FoodPreference>,
    val restaurantTypes: List<RestaurantType>,
    val eventEvening: Evening,
    val eventDishes: Map<RestaurantType, String>
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences) {
    var currentRestaurantType: RestaurantType? = null

    /**
     * beep beep I;m a document
     */
    fun visitingInThreeEvenings(): Boolean {
        return Time.evening + 3 == eventEvening
    }

    /**
     * sake of detect
     */

    override fun isVisitingTonight(): Boolean {
        return eventEvening == Time.evening
    }

    /**
     * sake of detect
     */
    override fun isVisitingThisTick(): Boolean {
        return visitingAt == Time.tick
    }
}
