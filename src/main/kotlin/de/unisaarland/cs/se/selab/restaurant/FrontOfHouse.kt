package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
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
    var numberOfCustomersServed: Int = 0
    var numberOfCustomersDelivered: Int = 0

    private var waiterIdCounter: Id = 1
    private fun getNextWaiterId(): Id = waiterIdCounter++

    private fun getInHouseGroups(): List<CustomerGroup> {
        return inHouseGroupsToWaiter.keys.toList()
    }

    // priority order used when sorting groups for serving/eating/escorting/rating; see the constants above.
    private fun getServingPriority(group: CustomerGroup): Int = when (group) {
        is RegularGroup -> REGULAR_PRIORITY
        is EventGroup -> EVENT_PRIORITY
        else -> CASUAL_PRIORITY
    }

    private fun getAssignedTableId(group: CustomerGroup): Id? = customerToTable[group]?.minOfOrNull { it.id }

    // recruit waiters for an EVENT group, accumulates enough (ordered by asc id) to cover group's servable dishes.
    // shared by SEATING (SEAT) and SERVING (SERVE); NOTE: does not yet handle ActionType.TAKE_ORDER.
    private fun recruitWaitersForEventGroup(actionType: ActionType, eventGroup: EventGroup): List<Waiter> {
        return when (actionType) {
            ActionType.SEAT -> waiters.filter {
                it.getTickLoad(ActionType.SEAT) < Constants.ACTION_LIMIT
            }.sortedByDescending { it.currentLoad }

            ActionType.TAKE_ORDER -> TODO()
            ActionType.SERVE -> recruitWaiterForServing(eventGroup)
            ActionType.ESCORT -> waiters.filter {
                it.getTickLoad(ActionType.ESCORT) < Constants.ACTION_LIMIT
            }
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
                val num = it.currentOrder!!.getServableDishes().size
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
        getInHouseGroups = { getInHouseGroups() },
        waiterFor = { group -> inHouseGroupsToWaiter[group] },
        getServingPriority = { group -> getServingPriority(group) },
        getAssignedTableId = { group -> getAssignedTableId(group) },
        recruitWaitersForEventGroup = { actionType, eventGroup -> recruitWaitersForEventGroup(actionType, eventGroup) },
    )

    private val delivering = DeliveryProcessor(drivers = drivers)

    private val eating = EatingProcessor(
        deliveryGroups = deliveryGroups,
        getInHouseGroups = { getInHouseGroups() },
        getServingPriority = { group -> getServingPriority(group) },
        getAssignedTableId = { group -> getAssignedTableId(group) },
        addCustomersServed = { count -> numberOfCustomersServed += count },
        addCustomersDelivered = { count -> numberOfCustomersDelivered += count },
    )

    private val escorting = EscortingProcessor(
        customerToTable = customerToTable,
        inHouseGroupsToWaiter = inHouseGroupsToWaiter,
        getInHouseGroups = { getInHouseGroups() },
        getServingPriority = { group -> getServingPriority(group) },
    )

    private val rating = RatingProcessor(
        deliveryGroups = deliveryGroups,
        turnedAwayGroups = turnedAwayGroups,
        getInHouseGroups = { getInHouseGroups() },
        getServingPriority = { group -> getServingPriority(group) },
        removeProcessedGroup = { group -> removeProcessedGroup(group) })

    /** Call with the CustomerGroup and menu.
     *  Returns true if CustomerGroup was processed successfully, false otherwise.
     *  To decide whether to remove the CustomerGroup from the customerQueue, use the formula
     *  processArrivalSeatingOrdering(customerGroup, menu) || customerGroup.isWaitingToBeSeated.
     *  If true, keep in the customerQueue, otherwise remove from the customerQueue. */
    fun processArrival(customerGroup: CustomerGroup, menu: List<Recipe>): Boolean =
        arrival.processArrival(customerGroup, menu)

    /** Call only with Regular- or EventGroup.
     *  Returns true if a reservation has been made and performs side effects on tables and customerToTable. */
    fun reserveTables(regularOrEventCustomerGroup: CustomerGroup): Boolean =
        arrival.reserveTables(regularOrEventCustomerGroup)

    /** Call after processArrivalSeatingOrdering has been called with each customerGroup in customerQueue.
     *  Logs status and then performs side effect by resetting counters. */
    fun logAndResetSeatingOrderingTickStatus() = arrival.logAndResetSeatingOrderingTickStatus()

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
     * Processes customer ratings and updates the positive and negative rating counts.
     * @param positiveRatings current number of positive ratings
     * @param negativeRatings current number of negative ratings
     * @return updated positive and negative rating counts
     */
    fun processRatings(
        positiveRatings: Int, negativeRatings: Int
    ): Pair<Int, Int> = rating.processRatings(positiveRatings, negativeRatings)

    /** Clears the load of waiters */
    fun clearActionLoads() {
        waiters.forEach {
            it.resetActionLoads()
        }
    }

    /**
     * Ends the evening by removing remaining customers, processing their
     * closing ratings, freeing all tables, and clearing waiter assignments.
     *
     * @param positiveRatings current number of positive ratings
     * @param negativeRatings current number of negative ratings
     * @return updated positive and negative rating counts
     */
    fun endFohEvening(
        positiveRatings: Int, negativeRatings: Int
    ): Pair<Int, Int> {
        var positive = positiveRatings
        var negative = negativeRatings
        val inHouseGroups = getInHouseGroups()

        escorting.escortAllAtClosing(inHouseGroups)

        inHouseGroups.forEach { group ->
            val result = rating.rate(
                group, positive, negative, true
            )

            positive = result.first
            negative = result.second
        }

        val customerIds = customerToTable.keys.map { it.id }

        customerIds.forEach {
            escorting.dismantleTable(it)
        }

        inHouseGroupsToWaiter.clear()

        return Pair(positive, negative)
    }

    private fun removeProcessedGroup(group: CustomerGroup) {
        if (getInHouseGroups().contains(group)) {
            removeInHouseGroup(group)
        } else if (deliveryGroups.contains(group)) {
            deliveryGroups.remove(group)
        } else if (turnedAwayGroups.contains(group)) {
            turnedAwayGroups.remove(group)
        }
    }

    private fun removeInHouseGroup(group: CustomerGroup) {
        inHouseGroupsToWaiter.remove(group)
    }
}
