package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.helpers.ArrivalProcessor
import de.unisaarland.cs.se.selab.restaurant.helpers.DeliveryProcessor
import de.unisaarland.cs.se.selab.restaurant.helpers.EatingProcessor
import de.unisaarland.cs.se.selab.restaurant.helpers.EscortingProcessor
import de.unisaarland.cs.se.selab.restaurant.helpers.RatingProcessor
import de.unisaarland.cs.se.selab.restaurant.helpers.ServingProcessor

// Priorities per customer group type, used when ordering groups for serving/eating/escorting/rating.
// DOIT: maybe move this to the customer classes
private const val REGULAR_PRIORITY = 0
private const val EVENT_PRIORITY = 1
private const val CASUAL_PRIORITY = 2

/** front of house */
class FrontOfHouse(
    private val tables: List<Table>,
    private val waiters: List<Waiter>,
    private val drivers: List<Driver>,
    private val countertop: Countertop
) {
    private val customerToTable: MutableMap<CustomerGroup, List<Table>> = mutableMapOf()
    private val inHouseGroupsToWaiter: MutableMap<CustomerGroup, Waiter> = mutableMapOf()
    private val eventGroups: MutableList<EventGroup> = mutableListOf()
    private val deliveryGroups: MutableList<CustomerGroup> = mutableListOf()
    private val turnedAwayGroups: MutableList<CustomerGroup> = mutableListOf()

    // statistics
    var numberOfCustomersDelivered: Int = 0

    private var waiterIdCounter: Id = 1
    private fun getNextWaiterId(): Id = waiterIdCounter++

    private fun getInHouseGroups(): List<CustomerGroup> {
        return inHouseGroupsToWaiter.keys.toList()
    }

    // DOTO: use this consistently instead of adding lists everywhere
    private fun getSeatedGroups(): List<CustomerGroup> = getInHouseGroups() + eventGroups

    // priority order used when sorting groups for serving/eating/escorting/rating; see the constants above.
    private fun getServingPriority(group: CustomerGroup): Int = when (group) {
        is RegularGroup -> REGULAR_PRIORITY
        is EventGroup -> EVENT_PRIORITY
        else -> CASUAL_PRIORITY
    }

    private fun getAssignedTableId(group: CustomerGroup): Id? = customerToTable[group]?.minOfOrNull { it.id }

    // recruit waiters for an EVENT group, accumulates enough (ordered by asc id) to cover group's servable dishes.
    // shared by SEATING (SEAT), ORDERING (TAKE_ORDER) and SERVING (SERVE).
    private fun recruitWaitersForEventGroup(actionType: ActionType, eventGroup: EventGroup): List<Waiter> {
        return when (actionType) {
            ActionType.SEAT -> waiters.filter {
                it.getTickLoad(ActionType.SEAT) < Constants.ACTION_LIMIT
            }.sortedByDescending { it.currentLoad }

            ActionType.TAKE_ORDER -> waiters.filter {
                it.getTickLoad(ActionType.TAKE_ORDER) < Constants.ACTION_LIMIT
            }.sortedByDescending { it.currentLoad }

            ActionType.SERVE -> recruitWaiterForServing(eventGroup)
            ActionType.ESCORT -> waiters.filter {
                it.getTickLoad(ActionType.ESCORT) < Constants.ACTION_LIMIT
            }.sortedBy { it.currentLoad }
        }
    }

    private fun recruitWaiterForServing(customerGroup: EventGroup): List<Waiter> {
        val required = customerGroup.currentOrder?.getServableDishes()?.size ?: 0
        val eligible = waiters.filter { it.getTickLoad(ActionType.SERVE) < Constants.ACTION_LIMIT }
        val waiterToCookedDishes: MutableMap<Waiter, Int> = mutableMapOf()
        eligible.forEach { targetWaiter ->
            var res = 0
            val customerGroups = inHouseGroupsToWaiter.filterValues {
                it == targetWaiter
            }.keys
            customerGroups.forEach {
                val num = it.currentOrder?.getServableDishes()?.size ?: 0
                res += num
            }
            waiterToCookedDishes[targetWaiter] = res
        }
        val sortedWaiterToCookedDishes: MutableMap<Waiter, Int> =
            waiterToCookedDishes.entries.sortedBy { it.key.id }.sortedByDescending { it.value }
                .associate { it.key to it.value }.toMutableMap()
        val sortedWaiters = sortedWaiterToCookedDishes.keys.toList()
        val result = mutableListOf<Waiter>()
        var recruitedCapacity = 0
        for (waiter in sortedWaiters) {
            if (recruitedCapacity >= required) {
                break
            }
            result.add(waiter)
            recruitedCapacity += Constants.ACTION_LIMIT - waiter.getTickLoad(ActionType.SERVE)
        }
        return result
    }

    private val arrival = ArrivalProcessor(
        tables,
        waiters,
        customerToTable,
        inHouseGroupsToWaiter,
        turnedAwayGroups,
        eventGroups,
        countertop,
        ::recruitWaitersForEventGroup,
        ::getNextWaiterId
    )

    private val serving = ServingProcessor(
        waiters = waiters,
        drivers = drivers,
        deliveryGroups = deliveryGroups,
        getInHouseGroups = { getSeatedGroups() },
        waiterFor = { group -> inHouseGroupsToWaiter[group] },
        getServingPriority = { group -> getServingPriority(group) },
        getAssignedTableId = { group -> getAssignedTableId(group) },
        recruitWaitersForEventGroup = { actionType, eventGroup -> recruitWaitersForEventGroup(actionType, eventGroup) },
        getNextWaiterId = ::getNextWaiterId,
    )

    // statistics: counted by the serving step, where the meals actually change hands (item 180)
    val numberOfCustomersServed: Int get() = serving.numberOfCustomersServed

    private val delivering = DeliveryProcessor(drivers = drivers, deliveryGroups = deliveryGroups)

    private val eating = EatingProcessor(
        deliveryGroups = deliveryGroups,
        getInHouseGroups = { getSeatedGroups() },
        getServingPriority = { group -> getServingPriority(group) },
        getAssignedTableId = { group -> getAssignedTableId(group) },
        addCustomersDelivered = { count -> numberOfCustomersDelivered += count },
    )

    private val escorting = EscortingProcessor(
        customerToTable = customerToTable,
        eventGroups = eventGroups,
        inHouseGroupsToWaiter = inHouseGroupsToWaiter,
        getInHouseGroups = { getInHouseGroups() },
        getServingPriority = { group -> getServingPriority(group) },
        ::recruitWaitersForEventGroup,
        ::getNextWaiterId
    )

    private val rating = RatingProcessor(
        deliveryGroups = deliveryGroups,
        turnedAwayGroups = turnedAwayGroups,
        eventGroups = eventGroups,
        getInHouseGroups = { getInHouseGroups() },
        getServingPriority = { group -> getServingPriority(group) },
        removeProcessedGroup = { group -> removeProcessedGroup(group) }
    )

    /** Call with the CustomerGroup and menu.
     *  Returns true if CustomerGroup was processed successfully, false otherwise.
     *  To decide whether to remove the CustomerGroup from the customerQueue, use the formula
     *  processArrival(customerGroup, menu) || customerGroup.isWaitingToBeSeated.
     *  If true, keep in the customerQueue, otherwise remove from the customerQueue. */
    fun processArrival(customerGroup: CustomerGroup, menu: List<Recipe>): Boolean {
        val isProcessed = when (customerGroup) {
            is CasualGroup, is RegularGroup -> arrival.processArrival(customerGroup, menu)
            is EventGroup -> arrival.processArrival(customerGroup, menu)
        }
        registerDeliveryGroup(customerGroup)
        return isProcessed
    }

    /** Add CasualGroup that wants delivery to deliveryGroups if the order was made successfully. */
    private fun registerDeliveryGroup(customerGroup: CustomerGroup) {
        if (customerGroup !is CasualGroup || !customerGroup.wantsDelivery) {
            return
        }
        if (customerGroup.currentOrder != null && !turnedAwayGroups.contains(customerGroup)) {
            deliveryGroups.addLast(customerGroup)
        }
    }

    /** Call only with Regular- or EventGroup.
     *  Returns true if a reservation has been made and performs side effects on tables and customerToTable. */
    fun reserveTables(regularOrEventCustomerGroup: CustomerGroup): Boolean =
        arrival.reserveTables(regularOrEventCustomerGroup)

    /** Call after processArrivalSeatingOrdering has been called with each customerGroup in customerQueue.
     *  Logs status and then performs side effect by resetting counters. */
    fun logAndResetSeatingOrderingTickStatus() = arrival.logAndResetSeatingOrderingTickStatus()

    /** Turns a group away unlogged because the restaurant is in the last 3 ticks of its opening
     *  time and no longer accepts new customers. */
    fun refuseLateArrival(customerGroup: CustomerGroup) = arrival.refuseLateArrival(customerGroup)

    /** process serving */
    fun processServing() = serving.processServing()

    /** starts driving drivers who just received a full order, and advances already-driving drivers */
    fun processDelivering() = delivering.processDelivering()

    /** whether any driver is currently free to take on a new delivery */
    fun isDriverAvailable(): Boolean = delivering.isDriverAvailable()

    /** process eating */
    fun processEating() = eating.processEating()

    /** process escorting */
    fun processEscorting() = escorting.processEscorting()

    /**
     * returns the number of free seats per table type
     */
    fun getFreeSeats(): MutableMap<TableType, Int> {
        val freeTables = tables.filter { it.status == TableStatus.FREE }
        val map = mutableMapOf<TableType, Int>()
        for (type in TableType.entries) {
            map[type] = 0
        }
        freeTables.forEach { freeTable ->
            map[freeTable.tableType] = map.getValue(freeTable.tableType) + freeTable.size
        }
        return map
    }

    /**
     * Processes customer ratings and updates the positive and negative rating counts.
     * @param positiveRatings current number of positive ratings
     * @param negativeRatings current number of negative ratings
     * @return updated positive and negative rating counts
     */
    fun processRatings(
        positiveRatings: Int,
        negativeRatings: Int
    ): Pair<Int, Int> = rating.processRatings(positiveRatings, negativeRatings)

    /** Clears the load of waiters */
    fun clearActionLoads() {
        waiters.forEach {
            it.resetActionLoads()
        }
    }

    /**
     * Escorts everybody still inside out of the restaurant when the opening time ends, without
     * the waiters having to perform an action. Groups that had not finished eating keep a
     * negative experience. Call this *before* the tick's rating step so that the closing ratings
     * are part of that step and are counted by its status log.
     */
    fun startFohClosing() {
        val closingGroups = getInHouseGroups() + eventGroups
        escorting.escortAllAtClosing(closingGroups)
        closingGroups.forEach { group ->
            val order = group.currentOrder
            if (order == null || !order.areAllDishesEaten()) {
                group.experience = ExperienceType.NEGATIVE
            }
        }
    }

    /**
     * Ends the opening time: frees all tables, discards this evening's reservations and clears
     * the waiter assignments. Call after the rating step, i.e. after [startFohClosing].
     */
    fun endFohOpeningTime() {
        val customerIds = customerToTable.keys.map { it.id }

        customerIds.forEach {
            escorting.dismantleTable(it)
        }

        inHouseGroupsToWaiter.clear()
        resetWaiters()
    }

    private fun resetWaiters() {
        clearActionLoads()
        waiters.forEach {
            it.id = null
            it.currentLoad = 0
        }
        waiterIdCounter = 1
    }

    /** lets drivers that are RETURNING continue, otherwise abort their order and make them IDLE */
    fun resetDrivers() {
        for (driver in drivers) {
            if (driver.state == DriverState.RETURNING) {
                continue
            }
            driver.currentOrder?.dishes?.forEach {
                if (it.status != DishStatus.EATEN) {
                    it.status = DishStatus.ABORTED
                }
            }
            driver.state = DriverState.IDLE
            driver.currentOrder = null
            driver.targetGroup = null
            driver.totalTripTicks = 0
            driver.ticksToDest = 0
            driver.tripDistance = 0
            driver.distanceDriven = 0
        }
    }

    private fun removeProcessedGroup(group: CustomerGroup) {
        if (getInHouseGroups().contains(group)) {
            removeInHouseGroup(group)
        } else if (deliveryGroups.contains(group)) {
            deliveryGroups.remove(group)
        } else if (turnedAwayGroups.contains(group)) {
            turnedAwayGroups.remove(group)
        } else if (group is EventGroup) {
            eventGroups.remove(group)
        }
    }
    private fun removeInHouseGroup(group: CustomerGroup) {
        inHouseGroupsToWaiter.remove(group)
    }

    /**
     * Needed to set drivers in restaurant Stats
     */
    fun getAvailableDrivers(): Int {
        return drivers.size
    }

    /**
     * Sums the seat capacity of all free tables, grouped by table type.
     * Needed to set available seats in restaurant Stats.
     */
    fun getAvailableSeats(): Map<TableType, Int> {
        val result = mutableMapOf<TableType, Int>()
        for (type in TableType.values()) {
            result[type] = 0
        }
        for (table in tables) {
            if (table.status == TableStatus.FREE) {
                result[table.tableType] = result[table.tableType]!! + table.size
            }
        }
        return result
    }
}
