package customer

import enums.TableType
import food.Order
import types.Evening
import types.Id
import types.Tick

class RegularGroup(
    private val id: Id,
    val size: Int,
    val tableType: TableType,
    private val visitingAt: Tick,
    private val foodPreference: List<FoodPreference>,
    val visitingStart: Evening,
    val visitingPeriod: Tick,
    val restaurantId: Id,
) : CustomerGroup(id, size, tableType, visitingAt, foodPreference) {
    private val orderHistory: MutableList<Order> = mutableListOf()
    private var failedReservations: Int = 0
}