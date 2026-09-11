package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order

/** Represents a regular customer group. */
class RegularGroup(
    id: Id,
    size: Int,
    tableType: TableType,
    visitingAt: Tick,
    foodPreferences: List<FoodPreference>,
    val visitingStart: Evening,
    val visitingPeriod: Tick,
    val restaurantId: Id,
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences) {
    private val orderHistory: MutableList<Order> = mutableListOf()
    private var failedReservations: Int = 0
}
