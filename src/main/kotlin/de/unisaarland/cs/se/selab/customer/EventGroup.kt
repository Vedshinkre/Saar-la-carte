package customer

import enums.RestaurantType
import enums.TableType
import types.Evening
import types.Id
import types.Tick

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