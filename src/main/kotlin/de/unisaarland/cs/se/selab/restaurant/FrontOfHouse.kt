package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger

private const val THREE_QUARTERS: Double = 3.0 / 4.0

private const val ACTION_LIMIT = 10
private const val PARTIAL_SERVING_WAIT_TICKS = 1
private const val REGULAR_PRIORITY = 0
private const val EVENT_PRIORITY = 1
private const val CASUAL_PRIORITY = 2

/** Represents the front of the house. */
class FrontOfHouse(
    private val tables: List<Table>,
    private val waiters: List<Waiter>,
    private val drivers: List<Driver>,
    private val countertop: Countertop,
) {
    private val customerToTable: MutableMap<CustomerGroup, List<Table>> = mutableMapOf()
    private val inHouseGroupsToWaiter: MutableMap<CustomerGroup, Waiter> = mutableMapOf()
    private val deliveryGroups: MutableList<CustomerGroup> = mutableListOf()
    private val turnedAwayGroups: MutableList<CustomerGroup> = mutableListOf()

    /** Call only with Regular- or EventGroup.
     *  Returns true if reservation has been made and performs side effects on tables and customerToTable. */
    fun reserveTables(regularOrEventCustomerGroup: CustomerGroup): Boolean {
        when (regularOrEventCustomerGroup) {
            is RegularGroup, is EventGroup -> Unit
            is CasualGroup -> throw IllegalArgumentException("Casual customer groups cannot make reservations.")
        }

        val reserveSingleTable: (Table) -> Unit = { table ->
            table.status = TableStatus.RESERVED
            customerToTable[regularOrEventCustomerGroup] = listOf(table)
        }

        val reserveMultipleTables: (List<Table>) -> Unit = { tables ->
            tables.forEach { table -> table.status = TableStatus.RESERVED }
            customerToTable[regularOrEventCustomerGroup] = tables
        }

        // Step 1: Filter and sort
        val sortedTables: List<Table> = getSortedPreferredFreeTables(regularOrEventCustomerGroup)

        // Step 2 and 3: Perfect fit or single table with three quarters rule
        trySingleTable(regularOrEventCustomerGroup, sortedTables, true)
            ?.also {
                reserveSingleTable(it)
                return true
            }

        // Step 4: Multiple tables with three quarters rule
        mergeTables(regularOrEventCustomerGroup, sortedTables, true)
            ?.also {
                reserveMultipleTables(it)
                return true
            }

        // Step 5: Single table without three quarters rule
        trySingleTable(regularOrEventCustomerGroup, sortedTables, false)
            ?.also {
                reserveSingleTable(it)
                return true
            }

        // Step 6: Multiple tables without three quarters rule
        mergeTables(regularOrEventCustomerGroup, sortedTables, false)
            ?.also {
                reserveMultipleTables(it)
                return true
            }

        return false
    }

    private fun getSortedPreferredFreeTables(customerGroup: CustomerGroup): List<Table> {
        val freeTables: List<Table> = tables.filter { it.status == TableStatus.FREE }
        val preferredTables: List<Table> = freeTables.filter { it.tableType == customerGroup.tableType }
        val sortedTables: List<Table> = preferredTables.sortedBy { it.id }.sortedBy { it.size }

        return sortedTables
    }

    private fun trySingleTable(customerGroup: CustomerGroup, tables: List<Table>, threeQuarters: Boolean): Table? {
        val groupFitsPredicate: (Table) -> Boolean =
            { customerGroup.size <= it.size }
        val threeQuartersPredicate: (Table) -> Boolean =
            { it.size.toDouble() * THREE_QUARTERS <= customerGroup.size && groupFitsPredicate(it) }

        return tables.find(if (threeQuarters) threeQuartersPredicate else groupFitsPredicate)
    }

    private fun mergeTables(customerGroup: CustomerGroup, tables: List<Table>, threeQuarters: Boolean): List<Table>? {
        val acc: MutableList<Table> = mutableListOf()
        var mergeSize = 0

        for (table in tables) {
            if (customerGroup.size <= mergeSize) { break }
            acc.addLast(table)
            mergeSize += table.size
        }

        if (mergeSize < customerGroup.size) { return null }

        for (table in acc) {
            if (customerGroup.size < mergeSize && customerGroup.size <= mergeSize - table.size) {
                acc.removeFirst()
                mergeSize -= table.size
            } else { break }
        }

        if (threeQuarters && mergeSize.toDouble() * THREE_QUARTERS > customerGroup.size) { return null }

        return acc
    }

    // SERVING START

    // TODO: instead of using explicit getters for e.g. inHouseGroups, have a public attribute whose get function returns the keys of inHouseGroupsToWaiter

    /** get all customer groups currently seated in the restaurant. */
    fun getInHouseGroups(): List<CustomerGroup> = customerToTable.keys.toList()

    /** picks waiter to serve driver delivery meals: waiter with min id whose SERVING tick load < action limit */
    private fun assignWaiterForDelivery(): Waiter? =
        waiters.filter { it.getTickLoad(ActionType.SERVE) < ACTION_LIMIT }.minByOrNull { it.id ?: Int.MAX_VALUE }

    /** serves cooked meals from kitchen to in-house groups and drivers, logs SERVING actions performed */
    fun processServing() {
        // split into three private function calls (unlike sequence diagram)
        serveInHouseGroups()
        serveDeliveryGroups()
        logServingStatus()
    }

    private fun serveInHouseGroups() {
        val sortedGroups = getInHouseGroups().sortedWith(compareBy({ servingPriority(it) }, { it.id }))
        for (group in sortedGroups) {
            val order = group.currentOrder ?: continue
            if (group is EventGroup) {
                serveEventTable(group, order)
            } else {
                serveAssignedWaiterTable(group, order)
            }
        }
    }

    /** can't have "magic numbers" must store priorities as global constants */
    private fun servingPriority(group: CustomerGroup): Int = when (group) {
        is RegularGroup -> REGULAR_PRIORITY
        is EventGroup -> EVENT_PRIORITY
        else -> CASUAL_PRIORITY
    }

    /** serves a REGULAR or CASUAL group who always have an assigned waiter */
    private fun serveAssignedWaiterTable(group: CustomerGroup, order: Order) {
        val waiter = inHouseGroupsToWaiter[group] ?: return
        val tableId = getAssignedTableId(group) ?: return
        // TODO: relies on an invariant, maybe assert it

        val servableDishes = order.getServableDishes()
        if (servableDishes.isEmpty()) return

        val complete = order.areAllDishesCooked()
        val capacity = ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)

        // proceed only if order is either complete or can be partially served yet
        if (!order.hasServingStarted() && !complete && isWithinTimeWindow(order)) {
            logNoServing(waiter, servableDishes.size, tableId)
            return
        }

        // if order is complete (and not started serving), waiter must be able to serve ALL servable dishes at once
        if (!order.hasServingStarted() && complete && capacity < servableDishes.size) {
            order.startServing()
            // waiter doesn't have capacity to serve ALL servable dishes in the complete order, so they don't serve ANY
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
        val tableId = getAssignedTableId(group) ?: return
        // invariant, could be asserted

        val readyDishes = order.getServableDishes()
        if (readyDishes.isEmpty()) return

        val complete = order.areAllDishesCooked()

        // proceed only if order is either complete or can be partially served
        if (!order.hasServingStarted() && !complete && isWithinTimeWindow(order)) {
            // log with the FIRST waiter that could've served
            val candidate = recruitWaitersForEventGroup(group).firstOrNull()
            logNoServing(candidate, readyDishes.size, tableId)
            return
        }

        val recruitedWaiters = recruitWaitersForEventGroup(group)
        val totalCapacity = recruitedWaiters.sumOf { ACTION_LIMIT - it.getTickLoad(ActionType.SERVE) }
        // if order is complete (and not started serving), recruited waiters must be able to serve ALL servable dishes
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

    /** recruit waiters for an EVENT group, accumulates enough (ordered by asc id) to cover group's servable dishes. */
    // TODO: implement recruitWaitersForEventGroup()

    private fun serveDeliveryGroups() {
        val readyGroups = deliveryGroups
            .filter { it.currentOrder?.areAllDishesCooked() == true }
            .sortedBy { it.currentOrder?.getId() ?: Int.MAX_VALUE }

        for (group in readyGroups) {
            val order = group.currentOrder ?: continue
            val readyDishes = order.getServableDishes()
            if (readyDishes.isEmpty()) continue
            val driver = getOrAssignDriver(group, order) ?: continue
            serveToDriver(readyDishes, driver, order)
            order.markFullyServed()
        }
    }

    private fun getOrAssignDriver(group: CustomerGroup, order: Order): Driver? {
        val assigned = drivers.find { it.targetGroup == group && it.currentOrder == order }
        if (assigned != null) return assigned

        val freeDriver = drivers.find { it.state == DriverState.IDLE } ?: return null
        if (freeDriver.id == null) {
            freeDriver.id = getNextDriverId()
        }
        freeDriver.targetGroup = group
        freeDriver.currentOrder = order
        freeDriver.state = DriverState.WAITING
        return freeDriver
    }

    private fun serveToDriver(dishes: List<Dish>, driver: Driver, order: Order) {
        var remaining = dishes
        while (remaining.isNotEmpty()) {
            val waiter = assignWaiterForDelivery() ?: break
            val capacity = ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)
            val batch = remaining.take(capacity)
            waiter.serve(batch)
            waiter.addToTickLoad(ActionType.SERVE, batch.size)

            val waiterId = waiter.id
            val driverId = driver.id
            if (waiterId != null && driverId != null) {
                FohServiceLogger.logFohDelivery(waiterId, toDishNameAmounts(batch), driverId, order.getId())
            }
            remaining = remaining.drop(batch.size)
        }
    }

    // TODO: implement getNextDriverId()

    // helpers

    /** serves as many given dishes as waiter's remaining capacity allows. */
    private fun serveBatch(waiter: Waiter, dishes: List<Dish>, tableId: Id, order: Order): List<Dish> {
        val capacity = ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)
        if (capacity <= 0) return dishes
        val batch = dishes.take(capacity)
        waiter.serve(batch)
        waiter.addToTickLoad(ActionType.SERVE, batch.size)

        val waiterId = waiter.id
        if (waiterId != null) {
            val ticksSinceOrdering = Time.tick - order.getOrderedAt()
            FohServiceLogger.logFohServing(waiterId, toDishNameAmounts(batch), tableId, ticksSinceOrdering)
        }
        return dishes.drop(batch.size)
    }

    private fun isWithinTimeWindow(order: Order): Boolean {
        val firstCooked = order.getFirstCookedAt() ?: return true
        return Time.tick - firstCooked <= PARTIAL_SERVING_WAIT_TICKS
    }

    private fun getAssignedTableId(group: CustomerGroup): Id? = customerToTable[group]?.minOfOrNull { it.id }

    // for logging, could also be moved to the logic in the logging function
    private fun toDishNameAmounts(dishes: List<Dish>): Map<String, Int> {
        val amounts = mutableMapOf<String, Int>()
        for (dish in dishes) {
            val name = dish.getRecipe().getName()
            amounts[name] = (amounts[name] ?: 0) + 1
        }
        return amounts
    }

    private fun logNoServing(waiter: Waiter?, mealCount: Int, tableId: Id) {
        val waiterId = waiter?.id ?: return
        FohServiceLogger.logFohNoServing(waiterId, mealCount, tableId)
    }

    private fun logServingStatus() {
        val activeWaiters = waiters.count { it.getTickLoad(ActionType.SERVE) > 0 }
        val totalMeals = waiters.sumOf { it.getTickLoad(ActionType.SERVE) }
        FohServiceLogger.logFohServingStatus(activeWaiters, totalMeals)
    }

    // SERVING END
}
