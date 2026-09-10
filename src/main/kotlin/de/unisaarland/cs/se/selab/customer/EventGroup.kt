package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType

class EventGroup(
    private val id: Id,
    val size: Int,
    val tableType: TableType,
    private val visitingAt: Tick,
    private val foodPreference: List<FoodPreference>,
    val restaurantTypes: List<RestaurantType>,
    val eventEvening: Evening,
    private val eventDishes: Map<RestaurantType, String>
) : CustomerGroup(id, size, tableType, visitingAt, foodPreference) {
    private var currentRestaurantType: RestaurantType? = null
    private var failedReservations: Int = 0
}
