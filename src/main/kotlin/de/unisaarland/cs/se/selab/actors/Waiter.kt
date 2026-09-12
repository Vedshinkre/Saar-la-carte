package de.unisaarland.cs.se.selab.actors

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.food.Dish

// TODO: maybe get rid of explicit getters and setters in favour of kotlin's default get(), set()

/** waiter */
class Waiter {
    var id: Id? = null
    private var currentLoad: Int = 0
    private val tickLoads = mutableMapOf(
        Pair(ActionType.SEAT, 0),
        Pair(ActionType.TAKE_ORDER, 0),
        Pair(ActionType.SERVE, 0),
        Pair(ActionType.ESCORT, 0)
    )

    /** get current load */
    fun getCurrentLoad(): Int = currentLoad

    /** adds to the number of customers being waited on */
    fun addToCurrentLoad(number: Int) {
        currentLoad += number
    }

    /** get how many actions of a given type the waiter performed this tick */
    fun getTickLoad(action: ActionType): Int = tickLoads[action] ?: 0

    /** add to how many actions of the given type this waiter performed this tick */
    fun addToTickLoad(action: ActionType, number: Int) {
        tickLoads[action] = getTickLoad(action) + number
    }

    /** mark the given dishes as served */
    fun serve(dishes: List<Dish>) {
        for (dish in dishes) {
            dish.setStatus(DishStatus.SERVED)
        }
    }

    /** resets all per-tick action loads, called before every tick */
    fun resetActionLoads() {
        for (action in tickLoads.keys) {
            tickLoads[action] = 0
        }
    }
}
