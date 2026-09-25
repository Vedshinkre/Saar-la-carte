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

// `if (order == null) continue` instead of `?: continue` on purpose: detekt can't follow elvis
// jumps inside loops and flags the loop body as unreachable (https://github.com/detekt/detekt/issues/6129)

/**
 * handles the EATING phase of a tick: drops customers who waited too long for their meals,
 * decides a group's experience once its order is fully served and progresses in-house and
 * delivery eating
 *
 * @param deliveryGroups CASUAL groups with a delivery order
 * @param getInHouseGroups every group currently seated, EVENT groups included
 * @param getServingPriority sort key by group type (REGULAR, EVENT, CASUAL)
 * @param getAssignedTableId the table an in-house group is logged against
 * @param addCustomersDelivered adds to the restaurant's delivered customers statistic
 * @param releaseWaiterLoad takes customers that walked out without being ESCORTED off their waiter's load
 */
class EatingProcessor(
    private val deliveryGroups: List<CustomerGroup>,
    private val getInHouseGroups: () -> List<CustomerGroup>,
    private val getServingPriority: (CustomerGroup) -> Int,
    private val getAssignedTableId: (CustomerGroup) -> Id?,
    private val addCustomersDelivered: (Int) -> Unit,
    private val releaseWaiterLoad: (CustomerGroup, Int) -> Unit = { _, _ -> },
) {
    /** runs the eating phase for in-house and delivery groups and logs the eating status */
    fun processEating() {
        var eatingCount = 0
        var finishedCount = 0

        // two separate runs over all groups, so every walk-out of the tick is logged before the
        // first "finished eating" line
        val sortedGroups = getInHouseGroups().sortedWith(compareBy({ getServingPriority(it) }, { it.id }))
            .mapNotNull { group ->
                val order = group.currentOrder
                val tableId = getAssignedTableId(group)
                if (order == null || tableId == null) null else Triple(group, order, tableId)
            }

        for ((group, order, tableId) in sortedGroups) {
            // receiving any food at all makes this visit a success, ending a REGULAR failure streak
            if (group is RegularGroup && order.dishes.any { wasServed(it) }) group.failedAttempts = 0

            handleLeavingCustomers(group, order, tableId)
        }

        for ((group, order, tableId) in sortedGroups) {
            handleFullyServedOrder(group, order)

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
        // a meal that is COOKED but still waiting in the kitchen has not been SERVED either, so it
        // doesn't stop its customer's patience from running out (spec page 15)
        val unservedDishes = order.dishes.filter {
            !it.abandoned && (
                it.status == DishStatus.UNCOOKED ||
                    it.status == DishStatus.COOKING ||
                    it.status == DishStatus.COOKED
                )
        }
        if (unservedDishes.isEmpty()) return

        val ticksSinceOrder = Time.tick - order.orderedAt

        val noDishServed = !(order.dishes.any { wasServed(it) })
        // -1 as clarified in office hours
        val basePatience = Constants.UNSERVED_WAIT_TICKS - 1
        val unservedCustomersLeave = if (noDishServed) {
            ticksSinceOrder >= basePatience
        } else {
            ticksSinceOrder >= basePatience + Constants.ADDITIONAL_UNSERVED_WAIT_TICKS
        }

        if (!unservedCustomersLeave) return

        // all unserved customers leave; their meals aren't aborted, the kitchen carries on with
        // them but they never get served
        unservedDishes.forEach { it.abandoned = true }
        val leavingCustomers = if (noDishServed) {
            // nobody was served: the whole group walks out, which is a failed attempt for a REGULAR group
            if (group is RegularGroup) group.failedAttempts++
            group.customersRemainingInRestaurant
        } else {
            unservedDishes.size
        }
        group.customersRemainingInRestaurant -= leavingCustomers
        releaseWaiterLoad(group, leavingCustomers)
        group.experience = ExperienceType.NEGATIVE
        FohServiceLogger.logRestaurantNoEating(leavingCustomers, group.id, tableId)
    }

    /** decides the customer's experience first time the order is fully served */
    private fun handleFullyServedOrder(
        group: CustomerGroup,
        order: Order
    ) {
        // run the function only as soon as the order is first completely served
        if (order.lastDishServedAt != null) return

        val fullyServed = order.dishes.all { wasServed(it) }
        if (!fullyServed) return

        order.lastDishServedAt = Time.tick

        // experience is negative as soon as food arrives too late or not at all for at least one customer
        if (group.experience == ExperienceType.NEGATIVE) return

        // the 4 tick expectation window is counted inclusively
        group.experience = if (Time.tick - order.orderedAt < Constants.EXPECTATION_WINDOW_TICKS) {
            ExperienceType.POSITIVE
        } else {
            ExperienceType.NEUTRAL
        }
    }

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
            if (order == null || order.deliveredAt == null || order.areAllDishesEaten()) continue

            // counted once, in the tick the driver hands the order over
            if (order.deliveredAt == Time.tick) {
                addCustomersDelivered(order.dishes.size)
            }

            order.dishes.forEach { dish ->
                if (dish.status == DishStatus.SERVED) dish.updateEating()
            }

            if (order.areAllDishesEaten()) {
                DeliveryLogger.logDeliveryFinishedEating(group.id)
            }
        }
    }

    private fun wasServed(dish: Dish): Boolean = dish.status == DishStatus.SERVED || dish.status == DishStatus.EATEN
}
