package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order

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
