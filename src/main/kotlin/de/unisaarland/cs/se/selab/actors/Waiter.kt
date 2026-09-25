package de.unisaarland.cs.se.selab.actors

import de.unisaarland.cs.se.selab.Constants.ACTION_LIMIT
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.food.Dish

/** waiter: seats, takes orders, serves and escorts; gets an id the first time they act each evening */
class Waiter {

    var id: Id? = null
    var currentLoad: Int = 0
    val tickLoads = mutableMapOf(
        Pair(ActionType.SEAT, 0),
        Pair(ActionType.TAKE_ORDER, 0),
        Pair(ActionType.SERVE, 0),
        Pair(ActionType.ESCORT, 0)
    )

    /**
     * adds to the number of customers this waiter is looking after
     *
     * @param number customers to add, negative when customers leave
     */
    fun addToCurrentLoad(number: Int) {
        currentLoad += number
    }

    /**
     * @param action the action type to look up
     * @return how many actions of that type the waiter performed this tick
     */
    fun getTickLoad(action: ActionType): Int = tickLoads[action] ?: 0

    /** Returns the waiter's current id or assigns a new one if it does not exist. */
    fun ensureId(getNextWaiterId: () -> Id): Id = id ?: getNextWaiterId().also { id = it }

    /**
     * adds to how many actions of the given type this waiter performed this tick
     *
     * @param action the action type to count
     * @param number how many actions to add
     */
    fun addToTickLoad(action: ActionType, number: Int) {
        tickLoads[action] = getTickLoad(action) + number
    }

    /**
     * marks the given dishes as SERVED, the caller handles tick load and logging
     *
     * @param dishes the dishes to serve
     */
    fun serve(dishes: List<Dish>) {
        for (dish in dishes) {
            dish.status = DishStatus.SERVED
        }
    }

    /** resets all per-tick action loads, called before every tick */
    fun resetActionLoads() {
        for (action in tickLoads.keys) {
            tickLoads[action] = 0
        }
    }

    /**
     * Escorts the customer group out of the restaurant.
     * Adds to waiter's tick load
     * @param cg customer group to escort
     */
    fun escort(cg: CustomerGroup) {
        val escortLoad = getTickLoad(ActionType.ESCORT)
        val remainingCapacity = ACTION_LIMIT - escortLoad

        val customersToEscort = minOf(
            cg.customersRemainingInRestaurant,
            remainingCapacity
        )

        if (customersToEscort <= 0) {
            return
        }

        cg.customersRemainingInRestaurant -= customersToEscort

        addToTickLoad(ActionType.ESCORT, customersToEscort)
        addToCurrentLoad(-customersToEscort)
    }

    /**
     * Escorts as many of an event group's remaining customers out of the restaurant as this
     * tick's escort action limit still allows.
     * @param eventGroup the event group to escort
     */
    fun escortEventGroups(eventGroup: EventGroup) {
        val customersToEscort = minOf(
            eventGroup.customersRemainingInRestaurant,
            ACTION_LIMIT - getTickLoad(ActionType.ESCORT)
        )
        if (customersToEscort <= 0) {
            return
        }
        eventGroup.customersRemainingInRestaurant -= customersToEscort
        addToTickLoad(ActionType.ESCORT, customersToEscort)
    }
}
