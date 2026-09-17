package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
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
    val visitingPeriod: Evening,
    val restaurantId: Id,
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences) {
    val orderHistory: ArrayDeque<Order> = ArrayDeque()
    var failedAttempts: Int = 0

    override fun isVisitingTonight(): Boolean {
        val isPeriodicVisitEvening: Boolean = (Time.evening - visitingStart) % visitingPeriod == 0
        return failedAttempts < 2 && isPeriodicVisitEvening
    }

    /** Call with the order that was made.
     *  Updates the orderHistory of the regular customer group. */
    fun addOrderToHistory(order: Order) {
        if (orderHistory.size >= 3) {
            orderHistory.removeFirst()
        }
        orderHistory.addLast(order)
    }
}
