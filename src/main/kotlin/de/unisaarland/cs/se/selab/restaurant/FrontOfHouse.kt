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

// maybe make an abstract priority in customer group, each customer group will have an attribute for it
private const val REGULAR_PRIORITY = 0
private const val EVENT_PRIORITY = 1
private const val CASUAL_PRIORITY = 2

/** food arriving within this many ticks of ordering counts as a positive experience. */

/** Represents the front of the house. */
class FrontOfHouse(
    private val tables: List<Table>,
    private val waiters: List<Waiter>,
    private val drivers: List<Driver>,
    val countertop: Countertop,
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

    /** Clears the load of waiters */
    fun clearActionLoads() {
        waiters.forEach {
            it.resetActionLoads()
        }
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
                if (!customerGroup.isWaitingToBeSeated) {
                    when (customerGroup) {
                        is RegularGroup -> {
                            customerToTable.remove(customerGroup)
                        }

                        is CasualGroup -> Unit
                    }
                    turnedAwayGroups.addLast(customerGroup)
                    customerGroup.experience = ExperienceType.NEGATIVE
                }
                return false
            }

            when (customerGroup) {
                is RegularGroup -> Unit
                is CasualGroup -> if (!assignTables(customerGroup)) {
                    FohReceptionLogger.logFohNoSeating(customerGroup.id, waiter.id!!)
                    turnedAwayGroups.addLast(customerGroup)
                    customerGroup.experience = ExperienceType.NEGATIVE
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

            val remainingSeatingLoad: Int = ACTION_LIMIT - waiter.tickLoads[ActionType.SEAT]!!
            val seatingLoad: Int = min(remainingSeatingLoad, eventGroupSize)
            waiter.tickLoads[ActionType.SEAT] = waiter.tickLoads[ActionType.SEAT]!! + seatingLoad
            eventGroupSize -= seatingLoad
            consumedWaiters.addLast(waiter)
        }

        if (eventGroupSize != 0) {
            FohReceptionLogger.logFohNoSeatingNoWaitstaff(eventGroup.id)
            customerToTable.remove(eventGroup)
            turnedAwayGroups.addLast(eventGroup)
            eventGroup.experience = ExperienceType.NEGATIVE
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
            waiters.filter { it.tickLoads[ActionType.SEAT]!! + customerGroup.size <= ACTION_LIMIT }

        if (freeWaiters.isEmpty()) {
            return null
        }

        val currentLoadPool: List<Waiter> = freeWaiters.filter { it.currentLoad < ACTION_LIMIT }

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
    fun processServing() {}

    // SERVING END

    // DELIVERING START (drivers were served in SERVING)

    /** main delivering function: starts driving drivers who just received full order, advances already driving drivers */
    fun processDelivering() {}

    /** whether any driver is currently free to take on a new delivery */
    fun isDriverAvailable(): Boolean = drivers.any { it.state == DriverState.IDLE }

    /** computes the trip length and starts driving the driver */

    // DELIVERING END

    // EATING START

    /** main eating function: makes customers that waited too long leave and customers eating progress eating */
    fun processEating() {}

    /** aborts dishes and drops customers who have waited too long */

    // EATING END
    // ESCORTING START
    // TODO(maybe customerID as parameter is more work than just customer)

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
                    val waiters = recruitWaitersForEventGroup(ActionType.ESCORT, it)
                    val customersBefore = it.customersRemainingInRestaurant
                    waiters.forEach { waiter ->
                        waiter.escortEventGroups(it)
                        val customersEscorted = customersBefore - it.customersRemainingInRestaurant
                        logFohEscorting(
                            waiter.id!!, customersEscorted, it.id, getAssignedTableId(it.id).min()
                        )
                        waitstaffNumber++
                        customerEscortingNumber += customersEscorted
                    }

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
    } // ESCORTING END

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

    // END EVENING START
    /** escorts customers inside, frees tables, handles deliveries in closing time */
    fun endFohEvening() {
        // TODO()
    }
}
