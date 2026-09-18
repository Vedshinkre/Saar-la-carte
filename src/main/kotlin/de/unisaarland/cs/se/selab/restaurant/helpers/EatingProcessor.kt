package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.DeliveryLogger
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger

/**
 * Handles the EATING phase of a tick: progressing in-house and delivery groups' eating,
 * dropping customers who waited too long for unserved dishes, and deciding a group's
 * experience once its order is fully served. Extracted out of  (which still
 * owns the shared seating/statistics state) to keep that class's function count manageable;
 * the collections, lookups and statistics counters it needs are handed in from [de.unisaarland.cs.se.selab.restaurant.FrontOfHouse]
 * so both classes see the same shared state.
 */
class EatingProcessor(
    private val deliveryGroups: List<CustomerGroup>,
    private val getInHouseGroups: () -> List<CustomerGroup>,
    private val getServingPriority: (CustomerGroup) -> Int,
    private val getAssignedTableId: (CustomerGroup) -> Id?,
    private val addCustomersDelivered: (Int) -> Unit,
) {
    /** main eating function: makes customers that waited too long leave and customers eating progress eating */
    fun processEating() {
        var eatingCount = 0
        var finishedCount = 0 // for logging

        val sortedGroups = getInHouseGroups().sortedWith(compareBy({ getServingPriority(it) }, { it.id }))
        for (group in sortedGroups) {
            val order = group.currentOrder
            if (order == null) continue
            val tableId = getAssignedTableId(group)
            if (tableId == null) continue

            // receiving any food at all makes this visit a success, ending a REGULAR failure streak
            if (group is RegularGroup && order.dishes.any { wasServed(it) }) group.failedAttempts = 0

            handleLeavingCustomers(group, order, tableId)
            handleFullyServedOrder(group, order) // for experience

            val (eating, finished) = progressEating(order)
            eatingCount += eating
            finishedCount += finished
            if (finished > 0) {
                FohServiceLogger.logFohFinishedEating(finished, group.id, tableId)
            }
        }

        processDeliveryEating()
        FohServiceLogger.logFohEatingStatus(eatingCount, finishedCount)
    }

    /** aborts dishes and drops customers who have waited too long */
    private fun handleLeavingCustomers(group: CustomerGroup, order: Order, tableId: Id) {
        val unservedDishes = order.dishes.filter {
            it.status == DishStatus.UNCOOKED || it.status == DishStatus.COOKING
        }
        if (unservedDishes.isEmpty()) return // everyone served

        val ticksSinceOrder = Time.tick - order.orderedAt

        val noDishServed = !(order.dishes.any { wasServed(it) })
        val unservedCustomersLeave = if (noDishServed) {
            ticksSinceOrder >= Constants.UNSERVED_WAIT_TICKS
        } else { // wait another 2 ticks if someone in the group was served
            ticksSinceOrder >= Constants.UNSERVED_WAIT_TICKS + Constants.ADDITIONAL_UNSERVED_WAIT_TICKS
        }

        if (!unservedCustomersLeave) return

        // all unserved customers leave, any unserved dish is aborted, customers remaining decremented
        unservedDishes.forEach { it.status = DishStatus.ABORTED }
        val leavingCustomers = if (noDishServed) {
            // nobody was served: the whole group walks out, which is a failed attempt for a REGULAR group
            if (group is RegularGroup) group.failedAttempts++
            group.customersRemainingInRestaurant
        } else {
            unservedDishes.size
        }
        group.customersRemainingInRestaurant -= leavingCustomers
        group.experience = ExperienceType.NEGATIVE
        FohServiceLogger.logRestaurantNoEating(leavingCustomers, group.id, tableId)
    }

    /** decides the customer's experience first time the order is fully served */
    private fun handleFullyServedOrder(
        group: CustomerGroup,
        order: Order
    ) { // run the function only as soon as the order is first completely served
        if (order.lastDishServedAt != null) return

        val fullyServed = order.dishes.all { wasServed(it) }
        if (!fullyServed) return

        order.lastDishServedAt = Time.tick

        // experience is negative as soon as food arrives too late or not at all for at least one customer
        if (group.experience == ExperienceType.NEGATIVE) return

        group.experience = if (Time.tick - order.orderedAt <= Constants.EXPECTATION_WINDOW_TICKS) {
            ExperienceType.POSITIVE
        } else {
            ExperienceType.NEUTRAL
        }
    }

    // helpers

    private fun wasServed(dish: Dish): Boolean = dish.status == DishStatus.SERVED || dish.status == DishStatus.EATEN

    private fun progressEating(order: Order): Pair<Int, Int> {
        var eating = 0
        var finished = 0
        for (dish in order.getServedDishes()) {
            dish.updateEating()
            if (dish.status == DishStatus.EATEN) finished++ else eating++
        }
        return Pair(eating, finished)
    }

    // delivery orders only start eating once the order is delivered
    private fun processDeliveryEating() {
        for (group in deliveryGroups.sortedBy { it.id }) {
            val order = group.currentOrder
            if (order == null) continue
            if (order.deliveredAt == null || order.areAllDishesEaten()) continue

            // for statistics, runs exactly once per order (when driver hands over the order)
            if (order.deliveredAt == Time.tick) {
                addCustomersDelivered(group.size)
            }

            eatDeliveredDishes(order)
            if (order.areAllDishesEaten()) {
                DeliveryLogger.logDeliveryFinishedEating(group.id)
            }
        }
    }

    // split out of processDeliveryEating to keep its cognitive complexity under the detekt threshold
    private fun eatDeliveredDishes(order: Order) {
        for (dish in order.dishes) {
            if (dish.status == DishStatus.SERVED) dish.updateEating()
        }
    }
}
