package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
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
    private val eventDishes: Map<RestaurantType, String>
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences) {
    private var currentRestaurantType: RestaurantType? = null
    private var failedReservations: Int = 0
    // faildeReservations should exist only for EventGroups it seems from the spec
}
