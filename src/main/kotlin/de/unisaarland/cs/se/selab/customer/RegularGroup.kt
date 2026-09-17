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
    val visitingPeriod: Tick,
    val restaurantId: Id,
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences) {
    val orderHistory: MutableList<Order> = mutableListOf()
    var failedAttempts: Int = 0
    var hasReservationTonight: Boolean = false

    override fun isVisitingTonight(): Boolean {
        val isPeriodicVisitEvening: Boolean = (Time.evening - visitingStart) % visitingPeriod == 0
        return hasReservationTonight && failedAttempts < 2 && isPeriodicVisitEvening
        // DOIT what is hasReservationTonight's purpose in this function?
        // isVisitingTonight() is used in simulation to discern if regulars need to be prepared for in the prep phase.
        // When I call the function it will always return false because the default value is false and
        // no regulars will be processed
    }
}
