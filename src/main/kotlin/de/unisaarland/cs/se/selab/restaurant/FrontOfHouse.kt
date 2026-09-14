package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Constants.ACTION_LIMIT
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.*
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.DeliveryLogger
import de.unisaarland.cs.se.selab.loggers.FohReceptionLogger
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logCustomerRateRestaurant
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logFohEscorting
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logFohEscortingStatus
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logRatingStatus
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger
import kotlin.math.ceil
import kotlin.math.min

// TODO: maybe make an abstract priority in customer group, each customer group will have an attribute for it
private const val REGULAR_PRIORITY = 0
private const val EVENT_PRIORITY = 1
private const val CASUAL_PRIORITY = 2

/** food arriving within this many ticks of ordering counts as a positive experience. */

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

    // statistics
    var numberOfCustomersServed: Int = 0
    var numberOfCustomersDelivered: Int = 0

    private var numberOfTablesSeatedOn: Int = 0
    private var numberOfCustomersSeated: Int = 0
    private var numberOfWaitersSeated: Int = 0

    private var nextWaiterId: Id = 1
        get() = field++

    private var nextDriverId: Id = 1
        get() = field++

    private fun getInHouseGroups(): List<CustomerGroup> {
        return inHouseGroupsToWaiter.keys.toList()
    }

    private fun getAssignedWaiter(customerId: Id): Waiter {
        return inHouseGroupsToWaiter.entries.first { it.key.id == customerId }.value
    }

    private fun getAssignedTableId(customerId: Id): List<Id> {
        return customerToTable.entries.first { it.key.id == customerId }.value.map { it.id }
    }

    /** Call with CustomerGroup and menu.
     *  Returns true if CustomerGroup was processed successfully, false otherwise.
     *  To decide whether to remove the CustomerGroup from the customerQueue use the formula
     *  processArrivalSeatingOrdering(customerGroup, menu) || customerGroup.isWaitingToBeSeated.
     *  If true keep in the customerQueue, otherwise remove from the customerQueue. */
    fun processArrivalSeatingOrdering(customerGroup: CustomerGroup, menu: List<Recipe>): Boolean {
        if (customerGroup is RegularGroup || (customerGroup is CasualGroup && !customerGroup.wantsDelivery)) {
            val waiter: Waiter = assignWaiter(customerGroup) ?: run {
                FohReceptionLogger.logFohNoSeatingNoWaitstaff(customerGroup.id)
                if (!customerGroup.isWaitingToBeSeated) { // TODO: set experience here
                    when (customerGroup) {
                        is RegularGroup -> {
                            customerToTable.remove(customerGroup)
                        }

                        is CasualGroup -> Unit
                    }
                    turnedAwayGroups.addLast(customerGroup)
                }
                return false
            }

            when (customerGroup) {
                is RegularGroup -> Unit
                is CasualGroup -> if (!assignTables(customerGroup)) {
                    FohReceptionLogger.logFohNoSeating(customerGroup.id, waiter.id!!) // TODO: set experience here
                    turnedAwayGroups.addLast(customerGroup)
                    customerGroup.isWaitingToBeSeated = false
                    return false
                }
            }
            successfulSeating(customerGroup, listOf(waiter))
            waiter.tickLoads[ActionType.SEAT] = waiter.tickLoads[ActionType.SEAT]!! + customerGroup.size
            waiter.currentLoad += customerGroup.size
        }

        return turnedAwayGroups.first().visitingAt == customerGroup.visitingAt
    }

    /** Call with CustomerGroup and menu. (Method overloading redirects EventGroups to this implementation)
     *  Returns true if EventGroup was processed successfully, false otherwise.
     *  To decide whether to remove the EventGroup from the customerQueue use the formula
     *  processArrivalSeatingOrdering(customerGroup, menu) || customerGroup.isWaitingToBeSeated.
     *  If true keep in the customerQueue, otherwise remove from the customerQueue. */
    fun processArrivalSeatingOrdering(eventGroup: EventGroup, menu: List<Recipe>): Boolean {
        val recruitedWaiters: List<Waiter> = recruitWaitersForEventGroup(ActionType.SEAT, eventGroup)
        val consumedWaiters: MutableList<Waiter> = mutableListOf()

        var eventGroupSize: Int = eventGroup.size
        for (waiter in recruitedWaiters) {
            if (eventGroupSize <= 0) {
                break
            }
            if (waiter.id == null) {
                waiter.id = nextWaiterId
            }

            val remainingSeatingLoad: Int = Constants.ACTION_LIMIT - waiter.tickLoads[ActionType.SEAT]!!
            val seatingLoad: Int = min(remainingSeatingLoad, eventGroupSize)
            waiter.tickLoads[ActionType.SEAT] = waiter.tickLoads[ActionType.SEAT]!! + seatingLoad
            eventGroupSize -= seatingLoad
            consumedWaiters.addLast(waiter)
        }

        if (eventGroupSize != 0) {
            FohReceptionLogger.logFohNoSeatingNoWaitstaff(eventGroup.id)
            customerToTable.remove(eventGroup)
            return false
        }

        successfulSeating(eventGroup, consumedWaiters)
        return turnedAwayGroups.first().visitingAt == eventGroup.visitingAt
    }

    /** Call only with Regular- or EventGroup.
     *  Returns true if reservation has been made and performs side effects on tables and customerToTable. */
    fun reserveTables(regularOrEventCustomerGroup: CustomerGroup): Boolean {
        when (regularOrEventCustomerGroup) {
            is RegularGroup, is EventGroup -> Unit
            is CasualGroup -> throw IllegalArgumentException("Casual customer groups cannot make reservations.")
        }

        val reserveSingleTable: (Table) -> Boolean = { table ->
            table.status = TableStatus.RESERVED
            customerToTable[regularOrEventCustomerGroup] = listOf(table)
            true
        }

        val reserveMultipleTables: (List<Table>) -> Boolean = { tables ->
            tables.forEach { table -> table.status = TableStatus.RESERVED }
            customerToTable[regularOrEventCustomerGroup] = tables
            true
        }

        // Step 1: Filter and sort
        val sortedTables: List<Table> = getSortedPreferredFreeTables(regularOrEventCustomerGroup)

        // Step 2 and 3: Perfect fit or single table with three quarters rule
        trySingleTable(regularOrEventCustomerGroup, sortedTables, true)?.also { return reserveSingleTable(it) }

        // Step 4: Multiple tables with three quarters rule
        mergeTables(regularOrEventCustomerGroup, sortedTables, true)?.also { return reserveMultipleTables(it) }

        // Step 5: Single table without three quarters rule
        trySingleTable(regularOrEventCustomerGroup, sortedTables, false)?.also { return reserveSingleTable(it) }

        // Step 6: Multiple tables without three quarters rule
        mergeTables(regularOrEventCustomerGroup, sortedTables, false)?.also { return reserveMultipleTables(it) }

        InitialAndPrepLogger.logFohNoReservation(regularOrEventCustomerGroup.id) // TODO: set experience
        turnedAwayGroups.addLast(regularOrEventCustomerGroup)
        return false
    }

    /** Call after processArrivalSeatingOrdering has been called with each customerGroup in customerQueue.
     *  Logs status and then performs side effect by resetting counters. */
    fun logAndResetSeatingOrderingTickStatus() {
        FohReceptionLogger.logSeatingStatus(numberOfWaitersSeated, numberOfCustomersSeated, numberOfTablesSeatedOn)
        numberOfTablesSeatedOn = 0
        numberOfCustomersSeated = 0
        numberOfWaitersSeated = 0 // TODO: add ordering status variables, log and then reset them
    }

    private fun assignWaiter(customerGroup: CustomerGroup): Waiter? {
        val freeWaiters: List<Waiter> =
            waiters.filter { it.tickLoads[ActionType.SEAT]!! + customerGroup.size <= Constants.ACTION_LIMIT }

        if (freeWaiters.isEmpty()) {
            return null
        }

        val currentLoadPool: List<Waiter> = freeWaiters.filter { it.currentLoad < Constants.ACTION_LIMIT }

        if (currentLoadPool.isNotEmpty()) {
            return currentLoadPool.sortedWith(compareByDescending(nullsLast()) { it.id }).sortedBy { it.currentLoad }
                .last().also { if (it.id == null) it.id = nextWaiterId }
        }

        return freeWaiters.sortedWith(compareBy(nullsLast()) { it.id }).sortedBy { it.currentLoad }.first()
            .also { if (it.id == null) it.id = nextWaiterId }
    }

    private fun assignTables(casualGroup: CasualGroup): Boolean {
        val reserveSingleTable: (Table) -> Boolean = { table ->
            table.status = TableStatus.OCCUPIED
            customerToTable[casualGroup] = listOf(table)
            true
        }

        val reserveMultipleTables: (List<Table>) -> Boolean = { tables ->
            tables.forEach { table -> table.status = TableStatus.OCCUPIED }
            customerToTable[casualGroup] = tables
            true
        }

        // Step 1: Filter and sort
        val sortedTables: List<Table> =
            tables.filter { it.status == TableStatus.FREE }.filter { it.tableType == casualGroup.tableType }
                .sortedBy { it.id }.sortedBy { it.size }

        // Step 2 and 3: Perfect fit or single table with three quarters rule
        trySingleTable(casualGroup, sortedTables, true)?.also { return reserveSingleTable(it) }

        // Step 4: Multiple tables with three quarters rule
        mergeTables(casualGroup, sortedTables, true)?.also { return reserveMultipleTables(it) }

        return false
    }

    private fun trySingleTable(customerGroup: CustomerGroup, tables: List<Table>, threeQuarters: Boolean): Table? {
        val groupFitsPredicate: (Table) -> Boolean = { customerGroup.size <= it.size }
        val threeQuartersPredicate: (Table) -> Boolean =
            { it.size.toDouble() * Constants.MIN_TABLE_OCCUPANCY <= customerGroup.size && groupFitsPredicate(it) }

        return tables.find(if (threeQuarters) threeQuartersPredicate else groupFitsPredicate)
    }

    private fun mergeTables(customerGroup: CustomerGroup, tables: List<Table>, threeQuarters: Boolean): List<Table>? {
        val acc: MutableList<Table> = mutableListOf()
        var mergeSize = 0

        for (table in tables) {
            if (customerGroup.size <= mergeSize) {
                break
            }
            acc.addLast(table)
            mergeSize += table.size
        }

        if (mergeSize < customerGroup.size) {
            return null
        }

        for (table in acc) {
            if (customerGroup.size < mergeSize && customerGroup.size <= mergeSize - table.size) {
                acc.removeFirst()
                mergeSize -= table.size
            } else {
                break
            }
        }

        if (threeQuarters && mergeSize.toDouble() * Constants.MIN_TABLE_OCCUPANCY > customerGroup.size) {
            return null
        }

        return acc
    }

    private fun getSortedPreferredFreeTables(customerGroup: CustomerGroup): List<Table> {
        val freeTables: List<Table> = tables.filter { it.status == TableStatus.FREE }
        val preferredTables: List<Table> = freeTables.filter { it.tableType == customerGroup.tableType }
        val sortedTables: List<Table> = preferredTables.sortedBy { it.id }.sortedBy { it.size }

        return sortedTables
    }

    private fun successfulSeating(customerGroup: CustomerGroup, waiters: List<Waiter>) {
        val assignedTables: List<Table> = customerToTable[customerGroup]!!
        val mergeId: Id = assignedTables.minBy { it.id }.id
        if (assignedTables.size > 1) {
            FohReceptionLogger.logFohMergingTables(
                customerGroup.id,
                assignedTables.map { it.id }.sorted(),
                mergeId
            )
        }

        numberOfCustomersSeated += customerGroup.size
        numberOfWaitersSeated += waiters.size
        numberOfTablesSeatedOn += assignedTables.size

        FohReceptionLogger.logFohSeating(customerGroup.id, mergeId, waiters.map { it.id!! })
    }

    // SERVING START

    // TODO: instead of using explicit getters for e.g. inHouseGroups, have a public attribute whose get function returns the keys of inHouseGroupsToWaiter

    /** serves cooked meals from kitchen to in-house groups and drivers, logs SERVING actions performed */
    fun processServing() { // split into three private function calls (unlike sequence diagram)
        serveInHouseGroups()
        serveDeliveryGroups()
        logServingStatus()
    }

    private fun serveInHouseGroups() {
        val sortedGroups = getInHouseGroups().sortedWith(compareBy({ getServingPriority(it) }, { it.id }))
        for (group in sortedGroups) {
            val order = group.currentOrder ?: continue
            if (group is EventGroup) {
                serveEventTable(group, order)
            } else {
                serveAssignedWaiterTable(group, order)
            }
        }
    }

    // can't have "magic numbers" must store priorities as global constants
    // TODO: would be better to store this as an attribute in customerGroup that is overridden by each type of group
    private fun getServingPriority(group: CustomerGroup): Int = when (group) {
        is RegularGroup -> REGULAR_PRIORITY
        is EventGroup -> EVENT_PRIORITY
        else -> CASUAL_PRIORITY
    }

    /** serves a REGULAR or CASUAL group who always have an assigned waiter */
    private fun serveAssignedWaiterTable(group: CustomerGroup, order: Order) {
        val waiter = inHouseGroupsToWaiter[group] ?: return
        val tableId = getAssignedTableId(group) ?: return // TODO: relies on an invariant, maybe assert it

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
            order.startServing() // waiter doesn't have capacity to serve ALL servable dishes in the complete order, so they don't serve ANY
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
        val tableId = getAssignedTableId(group) ?: return // invariant, could be asserted

        val readyDishes = order.getServableDishes()
        if (readyDishes.isEmpty()) return

        val complete = order.areAllDishesCooked()

        // proceed only if order is either complete or can be partially served
        if (!order.hasServingStarted() && !complete && isWithinTimeWindow(order)) { // log with the FIRST waiter that could've served
            // TODO: action type is also
            val candidate = recruitWaitersForEventGroup(ActionType.SERVE, group).firstOrNull()
            logNoServing(candidate, readyDishes.size, tableId)
            return
        }

        val recruitedWaiters = recruitWaitersForEventGroup(ActionType.SERVE, group)
        val totalCapacity =
            recruitedWaiters.sumOf {
                Constants.ACTION_LIMIT - it.getTickLoad(ActionType.SERVE)
            } // if order is complete (and not started serving), recruited waiters must be able to serve ALL servable dishes
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

    /** recruit waiters for an EVENT group, accumulates enough (ordered by asc id) to cover group's servable dishes. */ // TODO: implement recruitWaitersForEventGroup()
    private fun recruitWaitersForEventGroup(actionType: ActionType, eventGroup: EventGroup): List<Waiter> {
        when (actionType) {
            ActionType.SEAT ->
                return waiters.filter { it.getTickLoad(ActionType.SEAT) < ACTION_LIMIT }
                    .sortedByDescending { it.currentLoad }

            ActionType.TAKE_ORDER -> TODO()
            ActionType.SERVE -> return recruitWaiterForServing(eventGroup)
            ActionType.ESCORT -> return waiters.filter {
                it.getTickLoad(ActionType.ESCORT) < ACTION_LIMIT
            }
        }
    }

    private fun recruitWaiterForServing(customerGroup: EventGroup): List<Waiter> {
        val required = customerGroup.currentOrder?.getServableDishes()?.size ?: 0
        val eligible = waiters.filter { it.getTickLoad(ActionType.SERVE) < ACTION_LIMIT }
        val waiterToCookedDishes: MutableMap<Waiter, Int> = mutableMapOf()
        eligible.forEach { targetWaiter ->
            var res = 0
            val customerGroups = inHouseGroupsToWaiter.filterValues {
                it == targetWaiter
            }.keys
            customerGroups.forEach {
                val num = it.currentOrder!!.getServableDishes().size
                res += num
            }
            waiterToCookedDishes[targetWaiter] = res
        }
        val sortedWaiterToCookedDishes: MutableMap<Waiter, Int> =
            waiterToCookedDishes.entries.sortedBy { it.key.id }.sortedByDescending { it.value }
                .associate { it.key to it.value }.toMutableMap()
        val sortedWaiters = sortedWaiterToCookedDishes.keys.toList()
        var result = mutableListOf<Waiter>()
        var recruitedCapacity = 0
        for (waiter in sortedWaiters) {
            if (recruitedCapacity >= required) {
                break
            }
            result.add(waiter)
            recruitedCapacity += ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)
        }
        return result
    }

    private fun serveDeliveryGroups() {
        val readyGroups = deliveryGroups.filter { it.currentOrder?.areAllDishesCooked() == true }
            .sortedBy { it.currentOrder?.id ?: Int.MAX_VALUE }

        for (group in readyGroups) {
            val order = group.currentOrder ?: continue
            val readyDishes = order.getServableDishes()
            if (readyDishes.isEmpty()) continue
            val driver = getOrAssignDriver(group, order) ?: continue
            serveToDriver(readyDishes, driver, order)
            order.markFullyServed()
        }
    }

    /** picks waiter to serve driver delivery meals: waiter with min id whose SERVING tick load < action limit */
    private fun assignWaiterForDelivery(): Waiter? =
        waiters.filter { it.getTickLoad(ActionType.SERVE) < Constants.ACTION_LIMIT }
            .minByOrNull { it.id ?: Int.MAX_VALUE }

    private fun getOrAssignDriver(group: CustomerGroup, order: Order): Driver? {
        val assigned = drivers.find { it.targetGroup == group && it.currentOrder == order }
        if (assigned != null) return assigned

        val freeDriver = drivers.find { it.state == DriverState.IDLE } ?: return null
        if (freeDriver.id == null) {
            freeDriver.id = nextDriverId
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
            val capacity = Constants.ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)
            val batch = remaining.take(capacity)
            waiter.serve(batch)
            waiter.addToTickLoad(ActionType.SERVE, batch.size)

            val waiterId = waiter.id
            val driverId = driver.id
            if (waiterId != null && driverId != null) {
                FohServiceLogger.logFohDelivery(waiterId, toDishNameAmounts(batch), driverId, order.id)
            }
            remaining = remaining.drop(batch.size)
        }
    }

    // helpers

    /** serves as many given dishes as waiter's remaining capacity allows. */
    private fun serveBatch(waiter: Waiter, dishes: List<Dish>, tableId: Id, order: Order): List<Dish> {
        val capacity = Constants.ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)
        if (capacity <= 0) return dishes
        val batch = dishes.take(capacity)
        waiter.serve(batch)
        waiter.addToTickLoad(ActionType.SERVE, batch.size)

        val waiterId = waiter.id
        if (waiterId != null) {
            val ticksSinceOrdering = Time.tick - order.orderedAt
            FohServiceLogger.logFohServing(waiterId, toDishNameAmounts(batch), tableId, ticksSinceOrdering)
        }
        return dishes.drop(batch.size)
    }

    private fun isWithinTimeWindow(order: Order): Boolean {
        val firstCooked = order.firstDishCookedAt ?: return true
        return Time.tick - firstCooked <= Constants.PARTIAL_SERVING_WAIT_TICKS
    }

    private fun getAssignedTableId(group: CustomerGroup): Id? = customerToTable[group]?.minOfOrNull { it.id }

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
        val waiterId = waiter?.id ?: return
        FohServiceLogger.logFohNoServing(waiterId, mealCount, tableId)
    }

    private fun logServingStatus() {
        val activeWaiters = waiters.count { it.getTickLoad(ActionType.SERVE) > 0 }
        val totalMeals = waiters.sumOf { it.getTickLoad(ActionType.SERVE) }
        FohServiceLogger.logFohServingStatus(activeWaiters, totalMeals)
    }

    // SERVING END

    // DELIVERING START (drivers were served in SERVING)

    /** main delivering function: starts driving drivers who just received full order, advances already driving drivers */
    fun processDelivering() {
        val readyDrivers =
            drivers.filter { it.state == DriverState.WAITING }.sortedBy { it.targetGroup?.id ?: Int.MAX_VALUE }
        for (driver in readyDrivers) {
            prepareDelivery(driver)
        }

        val activeDrivers = drivers.filter { it.state == DriverState.DELIVERING || it.state == DriverState.RETURNING }
            .sortedBy { it.targetGroup?.id ?: Int.MAX_VALUE }
        for (driver in activeDrivers) {
            driver.processTick()
        }
    }

    /** whether any driver is currently free to take on a new delivery */
    fun isDriverAvailable(): Boolean = drivers.any { it.state == DriverState.IDLE }

    /** computes the trip length and starts driving the driver */
    private fun prepareDelivery(driver: Driver) {
        val group = driver.targetGroup as? CasualGroup ?: return
        val order = driver.currentOrder ?: return
        val driverId = driver.id ?: return

        val ticks = ceil(group.deliveryDistance / Constants.DRIVER_SPEED).toInt()
        driver.ticksToDest = ticks
        driver.totalTripTicks = ticks * 2
        driver.state = DriverState.DELIVERING

        DeliveryLogger.logDeliveryPreparation(driverId, order.id, group.id, ticks)
    }

    // DELIVERING END

    // EATING START

    /** main eating function: makes customers that waited too long leave and customers eating progress eating */
    fun processEating() {
        var eatingCount = 0
        var finishedCount = 0 // for logging

        val sortedGroups = getInHouseGroups().sortedWith(compareBy({ getServingPriority(it) }, { it.id }))
        for (group in sortedGroups) {
            val order = group.currentOrder ?: continue
            val tableId = getAssignedTableId(group) ?: continue

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
        } else {
            ticksSinceOrder >= Constants.UNSERVED_WAIT_TICKS + Constants.ADDITIONAL_UNSERVED_WAIT_TICKS // wait another 2 ticks if someone in the group was served
        }

        if (!unservedCustomersLeave) return

        // all unserved customers leave, any unserved dish is aborted, customers remaining decremented
        unservedDishes.forEach { it.status = DishStatus.ABORTED }
        group.customersRemainingInRestaurant -= if (noDishServed) {
            group.customersRemainingInRestaurant
        } else {
            unservedDishes.size
        }
        group.experience = ExperienceType.NEGATIVE
        FohServiceLogger.logRestaurantNoEating(unservedDishes.size, group.id, tableId)
    }

    /** decides the customer's experience first time the order is fully served */
    private fun handleFullyServedOrder(group: CustomerGroup, order: Order) {
        if (order.lastDishServedAt != null) return // run the function only as soon as the order is first completely served

        val fullyServed = order.dishes.all { wasServed(it) }
        if (!fullyServed) return

        order.lastDishServedAt = Time.tick
        numberOfCustomersServed += group.size // statistics

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
            val order = group.currentOrder ?: continue
            if (order.deliveredAt == null || order.areAllDishesEaten()) continue

            // for statistics, runs exactly once per order (when driver hands over the order)
            if (order.deliveredAt == Time.tick) {
                numberOfCustomersDelivered += group.size
            }

            for (dish in order.dishes) {
                if (dish.status == DishStatus.SERVED) dish.updateEating()
            }
            if (order.areAllDishesEaten()) {
                DeliveryLogger.logDeliveryFinishedEating(group.id)
            }
        }
    }

    // EATING END
    // ESCORTING START
    // TODO(maybe customerID as parameter is more work than just customer)
    private fun dismantleTable(customerId: Id) {
        val tables = customerToTable.entries.first { it.key.id == customerId }.value

        tables.forEach {
            it.status = TableStatus.FREE
        }

        customerToTable.remove(
            customerToTable.keys.first { it.id == customerId }
        )
    }

    /**
     * Processes escorting for all in-house customer groups.
     *
     * Groups whose dishes have all been eaten are escorted
     * by their assigned waitstaff. After escorting, casual
     * groups whose customers have all left have their tables
     * dismantled.
     */
    fun processEscorting() {
        val inHouseGroups = getInHouseGroups().sortedWith(compareBy({ getServingPriority(it) }, { it.id }))
        var waitstaffNumber = 0
        var customerEscortingNumber = 0
        inHouseGroups.forEach {
            val order = it.currentOrder
            if (order != null && order.areAllDishesEaten()) {
                if (it is EventGroup) { // TODO(EVENT GROUP ESCORTING)
                } else {
                    val waiter = getAssignedWaiter(it.id)
                    val customersBefore = it.customersRemainingInRestaurant
                    waiter.escort(it)
                    val customersEscorted = customersBefore - it.customersRemainingInRestaurant
                    logFohEscorting(
                        waiter.id!!,
                        customersEscorted,
                        it.id,
                        getAssignedTableId(it.id).min()
                    )

                    waitstaffNumber++
                    customerEscortingNumber += customersEscorted
                }
            }
            if (it.customersRemainingInRestaurant == 0) {
                if (it is CasualGroup) dismantleTable(it.id)
            }
        }
        logFohEscortingStatus(
            waitstaffNumber,
            customerEscortingNumber
        )
    }
    // ESCORTING END

    // RATING START
    /**
     * Processes customer ratings and updates the positive and negative rating counts.
     * @param positiveRatings current number of positive ratings
     * @param negativeRatings current number of negative ratings
     * @return updated positive and negative rating counts
     */
    fun processRatings(
        positiveRatings: Int,
        negativeRatings: Int
    ): Pair<Int, Int> {
        var positive = positiveRatings
        var negative = negativeRatings
        val filteredInHouseGroups = getInHouseGroups().filter { it.customersRemainingInRestaurant == 0 }

        val filteredDeliveryGroups = deliveryGroups.filter {
            val order = it.currentOrder

            order == null || order.areAllDishesEaten() || order.dishes.any { dish ->
                dish.status == DishStatus.ABORTED
            }
        }
        val groupsToRate = (filteredInHouseGroups + filteredDeliveryGroups + turnedAwayGroups).sortedWith(
            compareBy({ getServingPriority(it) }, { it.id })
        )
        var groupsGivingRatings = 0
        groupsToRate.forEach {
            when (it.determineRating()) {
                RatingType.POSITIVE -> {
                    positive++
                    groupsGivingRatings++

                    logCustomerRateRestaurant(
                        it.id,
                        RatingType.POSITIVE,
                        positive,
                        negative
                    )
                }

                RatingType.NEGATIVE -> {
                    negative++
                    groupsGivingRatings++

                    logCustomerRateRestaurant(
                        it.id,
                        RatingType.NEGATIVE,
                        positive,
                        negative
                    )
                }

                RatingType.NO_RATING -> {}
            }
        }
        logRatingStatus(groupsGivingRatings)
        return Pair(positive, negative)
    } // RATING END
}
