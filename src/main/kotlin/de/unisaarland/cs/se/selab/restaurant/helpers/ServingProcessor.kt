package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger

/** serving processor */
class ServingProcessor(
    private val waiters: List<Waiter>,
    private val drivers: List<Driver>,
    private val deliveryGroups: List<CustomerGroup>,
    private val getInHouseGroups: () -> List<CustomerGroup>,
    private val waiterFor: (CustomerGroup) -> Waiter?,
    private val getServingPriority: (CustomerGroup) -> Int,
    private val getAssignedTableId: (CustomerGroup) -> Id?,
    private val recruitWaitersForEventGroup: (ActionType, EventGroup) -> List<Waiter>,
    private val getNextWaiterId: () -> Id,
) {
    var numberOfCustomersServed: Int = 0
        private set // shouldn't be set from outside

    private var nextDriverIdCounter: Id = 1

    private fun getNextDriverId() = nextDriverIdCounter++

    /** driver ids are handed out per restaurant and evening, so the counter restarts every evening */
    fun resetDriverIdCounter() {
        nextDriverIdCounter = 1
    }

    /** serves cooked meals from kitchen to in-house groups and drivers, logs SERVING actions performed */
    fun processServing() {
        serveInHouseGroups()
        serveDeliveryGroups()
        logServingStatus()
    }

    private fun serveInHouseGroups() {
        val sortedGroups = getInHouseGroups().sortedWith(compareBy({ getServingPriority(it) }, { it.id }))
        for (group in sortedGroups) {
            val order = group.currentOrder
            if (order == null) continue

            if (group is EventGroup) {
                serveEventTable(group, order)
            } else {
                serveAssignedWaiterTable(group, order)
            }
        }
    }

    /** serves a REGULAR or CASUAL group who always have an assigned waiter */
    private fun serveAssignedWaiterTable(group: CustomerGroup, order: Order) {
        // DOTO: cleanup cases
        val waiter = waiterFor(group) ?: return
        val tableId = getAssignedTableId(group) ?: return
        // NOTE: relies on the invariant that a seated group always has an assigned table.

        val servableDishes = order.getServableDishes()
        if (servableDishes.isEmpty()) return

        val complete = order.areAllDishesCooked()
        val capacity = Constants.ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)

        // proceed only if order is either complete or can be partially served yet
        if (!order.hasServingStarted() && !complete && isWithinTimeWindow(order)) {
            logNoServing(waiter, servableDishes.size, tableId)
            return
        }

        // if order is complete (and not started serving), waiter must be able to serve ALL servable dishes at once
        if (!order.hasServingStarted() && complete && capacity < servableDishes.size) {
            // waiter doesn't have capacity to serve ALL servable dishes in the complete order, so they serve none
            order.startServing()
            logNoServing(waiter, servableDishes.size, tableId)
            return
        }

        // can serve, serve as many as possible based on waiter's load
        order.startServing()
        val remaining = serveBatch(waiter, servableDishes, tableId, order)
        if (remaining.isNotEmpty()) {
            logNoServing(waiter, remaining.size, tableId)
        }
        order.markFullyServed()
    }

    /** serves an EVENT group, recruits as many waiters as needed. */
    private fun serveEventTable(group: EventGroup, order: Order) {
        val tableId = getAssignedTableId(group) ?: return // invariant could be asserted

        val readyDishes = order.getServableDishes()
        if (readyDishes.isEmpty()) return

        val complete = order.areAllDishesCooked()

        // proceed only if the order is either complete or can be partially served
        if (!order.hasServingStarted() && !complete && isWithinTimeWindow(order)) {
            val candidate = recruitWaitersForEventGroup(ActionType.SERVE, group).firstOrNull()
            logNoServing(candidate, readyDishes.size, tableId)
            // logs with the FIRST waiter that could've served, if any
            return
        }

        val recruitedWaiters = recruitWaitersForEventGroup(ActionType.SERVE, group)
        // if order is complete (and not started serving), recruited waiters must be able to serve ALL servable dishes
        val totalCapacity =
            recruitedWaiters.sumOf {
                Constants.ACTION_LIMIT - it.getTickLoad(ActionType.SERVE)
            }
        if (!order.hasServingStarted() && complete && totalCapacity < readyDishes.size) {
            order.startServing()
            logNoServing(recruitedWaiters.firstOrNull(), readyDishes.size, tableId)
            return
        }

        // can start serving the order, serve as many as possible for each recruited waiter
        order.startServing()
        var remaining = readyDishes
        for (waiter in recruitedWaiters) {
            if (remaining.isEmpty()) break
            remaining = serveBatch(waiter, remaining, tableId, order)
        }
        if (remaining.isNotEmpty()) {
            logNoServing(recruitedWaiters.lastOrNull(), remaining.size, tableId)
        }
        order.markFullyServed()
    }

    private fun serveDeliveryGroups() {
        val readyGroups = deliveryGroups.filter { isReadyForHandOver(it) }
            .sortedBy { it.currentOrder?.id ?: Int.MAX_VALUE }

        for (group in readyGroups) {
            val order = group.currentOrder
            if (order == null) continue
            val driver = getOrAssignDriver(group, order)
            if (driver != null) {
                serveToDriver(order.getServableDishes(), driver, order)
            }
        }
    }

    /** order queues until all meals are ready
     * once the hand-off has begun, already handed-over meals are SERVED and wait with driver
     * remaining cooked meals must keep being handed over in the following ticks
     */
    private fun isReadyForHandOver(group: CustomerGroup): Boolean {
        val order = group.currentOrder ?: return false
        if (order.getServableDishes().isEmpty()) return false
        return order.areAllDishesCooked() || drivers.any { it.currentOrder === order }
    }

    /** picks waiter to serve driver delivery meals: waiter with min id whose SERVING tick load < action limit */
    private fun assignWaiterForDelivery(): Waiter? =
        waiters.filter { it.getTickLoad(ActionType.SERVE) < Constants.ACTION_LIMIT }
            .minByOrNull { it.id ?: Int.MAX_VALUE }

    private fun getOrAssignDriver(group: CustomerGroup, order: Order): Driver? {
        val assigned = drivers.find { it.targetGroup == group && it.currentOrder == order }
        if (assigned != null) return assigned

        val freeDriver = drivers.find { it.state == DriverState.IDLE } ?: return null
        if (freeDriver.id == null) { freeDriver.id = getNextDriverId() }

        freeDriver.targetGroup = group
        freeDriver.currentOrder = order
        freeDriver.state = DriverState.WAITING

        return freeDriver
    }

    private fun serveToDriver(dishes: List<Dish>, driver: Driver, order: Order) {
        var remaining = dishes

        while (remaining.isNotEmpty()) {
            val waiter = assignWaiterForDelivery() ?: break
            val capacity = Constants.ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)
            val batch = remaining.take(capacity)

            waiter.serve(batch)
            waiter.addToTickLoad(ActionType.SERVE, batch.size)

            val waiterId = waiter.ensureId(getNextWaiterId)
            val driverId = requireNotNull(driver.id) { "a driver taking an order always has an id" }

            FohServiceLogger.logFohDelivery(waiterId, toDishNameAmounts(batch), driverId, order.id)
            remaining = remaining.drop(batch.size)
        }
    }

    /** serves as many given dishes as waiter's remaining capacity allows. */
    private fun serveBatch(waiter: Waiter, dishes: List<Dish>, tableId: Id, order: Order): List<Dish> {
        val capacity = Constants.ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)
        if (capacity <= 0) return dishes
        val batch = dishes.take(capacity)
        waiter.serve(batch)
        waiter.addToTickLoad(ActionType.SERVE, batch.size)
        numberOfCustomersServed += batch.size

        val waiterId = waiter.ensureId(getNextWaiterId)
        val ticksSinceOrdering = Time.tick - order.orderedAt
        FohServiceLogger.logFohServing(waiterId, toDishNameAmounts(batch), tableId, ticksSinceOrdering)
        return dishes.drop(batch.size)
    }

    // HELPERS

    private fun isWithinTimeWindow(order: Order): Boolean {
        // DOTO: look into how to set firstDishCookedAt
        val firstCooked = order.firstDishCookedAt ?: return true
        return Time.tick - firstCooked <= Constants.PARTIAL_SERVING_WAIT_TICKS
    }

    // for logging, could also be moved to the logic in the logging function
    private fun toDishNameAmounts(dishes: List<Dish>): Map<String, Int> {
        val amounts = mutableMapOf<String, Int>()
        for (dish in dishes) {
            val name = dish.recipe.name
            amounts[name] = (amounts[name] ?: 0) + 1
        }
        return amounts
    }

    private fun logNoServing(waiter: Waiter?, mealCount: Int, tableId: Id) {
        // no candidate at all means nobody attempted, so there is nothing to report
        val waiterId = (waiter ?: return).ensureId(getNextWaiterId)
        FohServiceLogger.logFohNoServing(waiterId, mealCount, tableId)
    }

    private fun logServingStatus() {
        val activeWaiters = waiters.count { it.getTickLoad(ActionType.SERVE) > 0 }
        val totalMeals = waiters.sumOf { it.getTickLoad(ActionType.SERVE) }
        FohServiceLogger.logFohServingStatus(activeWaiters, totalMeals)
    }
}
