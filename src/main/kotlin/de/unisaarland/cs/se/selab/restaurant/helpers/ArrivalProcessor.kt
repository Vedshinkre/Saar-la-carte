package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.CustomerStatus
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.FohReceptionLogger
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.Table
import kotlin.math.min

/** Encapsulates seating and ordering functionality. */
class ArrivalProcessor(
    private val tables: List<Table>,
    private val waiters: List<Waiter>,
    private val customerToTable: MutableMap<CustomerGroup, List<Table>>,
    private val inHouseGroupsToWaiter: MutableMap<CustomerGroup, Waiter>,
    private val turnedAwayGroups: MutableList<CustomerGroup>,
    private val eventGroups: MutableList<EventGroup>,
    private val countertop: Countertop,
    private val recruitWaitersForEventGroup: (ActionType, EventGroup) -> List<Waiter>,
    private val getNextWaiterId: () -> Id
) {
    private var numberOfTablesSeatedOn: Int = 0
    private var numberOfCustomersSeated: Int = 0
    private val numberOfWaitersSeated: MutableSet<Waiter> = mutableSetOf()

    // Sets used for logging purposes
    private val customersOrdered: MutableSet<CustomerGroup> = mutableSetOf()
    private val waitersOrdered: MutableSet<Waiter> = mutableSetOf()

    /** Call with CustomerGroup and menu.
     *  Returns true if CustomerGroup should be removed from the customerQueue, false otherwise. */
    fun processArrival(customerGroup: CustomerGroup, menu: List<Recipe>): Boolean {
        val isInHouse: Boolean =
            customerGroup is RegularGroup || (customerGroup is CasualGroup && !customerGroup.wantsDelivery)
        if (isInHouse) {
            when (seatRegularOrCasualGroup(customerGroup)) {
                CustomerStatus.NO_WAITER -> return false
                CustomerStatus.TO_LEAVE -> return true
                CustomerStatus.SEATED -> Unit
            }
        }

        val assignedWaiter = inHouseGroupsToWaiter[customerGroup]
        var somebodyOrdered: Boolean
        if (assignedWaiter != null) {
            somebodyOrdered = customerGroup.placeOrder(listOf(assignedWaiter), menu, countertop)
            assignedWaiter.currentLoad -= customerGroup.size - customerGroup.customersRemainingInRestaurant
        } else {
            somebodyOrdered = customerGroup.placeOrder(listOf(), menu, countertop)
        }

        if (!somebodyOrdered) {
            if (customerGroup is RegularGroup) {
                customerGroup.failedAttempts++
            }
            inHouseGroupsToWaiter.remove(customerGroup)
            turnedAwayGroups.addLast(customerGroup)
        } else if (customerGroup is RegularGroup) {
            customerGroup.failedAttempts = 0
        }

        val currentOrder = customerGroup.currentOrder ?: return true
        if (assignedWaiter != null) {
            val assignedWaiterId = assignedWaiter.id ?: getNextWaiterId()
            FohReceptionLogger.logFohOrdering(
                customerGroup.id,
                currentOrder.id,
                currentOrder.dishNameToAmount(),
                listOf(assignedWaiterId)
            )
            waitersOrdered.add(assignedWaiter)
        } else {
            FohReceptionLogger.logFohOrdering(
                customerGroup.id,
                currentOrder.id,
                currentOrder.dishNameToAmount(),
                null
            )
            customersOrdered.add(customerGroup)
        }
        val customersWhoLeftAfterOrdering = customerGroup.getCustomersWhoLeft()
        if (customersWhoLeftAfterOrdering > 0) {
            FohReceptionLogger.logFohNoOrdering(
                customerGroup.id,
                customersWhoLeftAfterOrdering
            )
        }

        return true
    }

    /** Call with CustomerGroup and menu.
     *  Returns true if CustomerGroup should be removed from the customerQueue, false otherwise. */
    fun processArrival(eventGroup: EventGroup, menu: List<Recipe>): Boolean {
        val recruitedWaiters: List<Waiter> = recruitWaitersForEventGroup(ActionType.SEAT, eventGroup)
        val consumedWaiters: MutableList<Waiter> = mutableListOf()
        var eventGroupSize: Int = eventGroup.size
        for (waiter in recruitedWaiters) {
            if (eventGroupSize <= 0) {
                break
            }
            if (waiter.id == null) {
                waiter.id = getNextWaiterId()
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
            turnedAwayGroups.addLast(eventGroup)
            eventGroup.experience = ExperienceType.NEGATIVE
        } else {
            successfulSeating(eventGroup, consumedWaiters)
            if (eventGroup.placeOrder(consumedWaiters, menu, countertop)) {
                val currentOrder = eventGroup.currentOrder
                if (currentOrder != null) {
                    FohReceptionLogger.logFohOrdering(
                        eventGroup.id,
                        currentOrder.id,
                        currentOrder.dishNameToAmount(),
                        null
                    )
                }

                eventGroups.add(eventGroup)
                customersOrdered.add(eventGroup)
                waitersOrdered.addAll(consumedWaiters)
            } else {
                turnedAwayGroups.addLast(eventGroup)
                eventGroup.experience = ExperienceType.NEGATIVE
            }
        }
        val customersWhoLeftAfterOrdering = eventGroup.getCustomersWhoLeft()
        if (customersWhoLeftAfterOrdering > 0) {
            FohReceptionLogger.logFohNoOrdering(
                eventGroup.id,
                customersWhoLeftAfterOrdering
            )
        }
        return true
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
        } // Step 1: Filter and sort
        val sortedTables: List<Table> = getSortedPreferredFreeTables(
            regularOrEventCustomerGroup
        ) // Step 2 and 3: Perfect fit or single table with three quarters rule
        trySingleTable(
            regularOrEventCustomerGroup,
            sortedTables,
            true
        )?.also { return reserveSingleTable(it) } // Step 4: Multiple tables with three quarters rule
        mergeTables(
            regularOrEventCustomerGroup,
            sortedTables,
            true
        )?.also { return reserveMultipleTables(it) } // Step 5: Single table without three quarters rule
        trySingleTable(
            regularOrEventCustomerGroup,
            sortedTables,
            false
        )?.also { return reserveSingleTable(it) } // Step 6: Multiple tables without three quarters rule
        mergeTables(regularOrEventCustomerGroup, sortedTables, false)?.also { return reserveMultipleTables(it) }

        InitialAndPrepLogger.logFohNoReservation(regularOrEventCustomerGroup.id)
        regularOrEventCustomerGroup.experience = ExperienceType.NEGATIVE
        turnedAwayGroups.addLast(regularOrEventCustomerGroup)
        if (regularOrEventCustomerGroup is RegularGroup) {
            regularOrEventCustomerGroup.failedAttempts++
        }

        return false
    }

    /** Call after processArrivalSeatingOrdering has been called with each customerGroup in customerQueue.
     *  Logs status and then performs side effect by resetting counters. */
    fun logAndResetSeatingOrderingTickStatus() {
        FohReceptionLogger.logSeatingStatus(numberOfWaitersSeated.size, numberOfCustomersSeated, numberOfTablesSeatedOn)
        numberOfTablesSeatedOn = 0
        numberOfCustomersSeated = 0
        numberOfWaitersSeated.clear()

        val numberOfCustomersOrdered = customersOrdered.sumOf {
                customerGroup ->
            customerGroup.customersRemainingInRestaurant
        }
        val numberOfWaitersOrdered = waitersOrdered.size
        FohReceptionLogger.logOrderingStatus(numberOfCustomersOrdered, numberOfWaitersOrdered)
        customersOrdered.clear()
        waitersOrdered.clear()
    }

    private fun seatRegularOrCasualGroup(customerGroup: CustomerGroup): CustomerStatus {
        val waiter: Waiter = assignWaiter(customerGroup) ?: return rejectForNoWaiter(customerGroup)

        if (customerGroup is CasualGroup && !assignTables(customerGroup)) {
            // DOTO: try to fix the waiter.id
            FohReceptionLogger.logFohNoSeating(customerGroup.id, waiter.id ?: getNextWaiterId())
            turnedAwayGroups.addLast(customerGroup)
            customerGroup.experience = ExperienceType.NEGATIVE
            return CustomerStatus.TO_LEAVE
        }

        successfulSeating(customerGroup, listOf(waiter))
        inHouseGroupsToWaiter[customerGroup] = waiter
        waiter.tickLoads[ActionType.SEAT] = waiter.tickLoads[ActionType.SEAT]!! + customerGroup.size
        waiter.currentLoad += customerGroup.size

        return CustomerStatus.SEATED
    }

    private fun rejectForNoWaiter(customerGroup: CustomerGroup): CustomerStatus {
        FohReceptionLogger.logFohNoSeatingNoWaitstaff(customerGroup.id)
        if (!customerGroup.isVisitingThisTick()) {
            if (customerGroup is RegularGroup) {
                customerToTable.remove(customerGroup)
                customerGroup.failedAttempts++
            }
            turnedAwayGroups.addLast(customerGroup)
            customerGroup.experience = ExperienceType.NEGATIVE
            return CustomerStatus.TO_LEAVE
        }
        return CustomerStatus.NO_WAITER
    }

    private fun successfulSeating(customerGroup: CustomerGroup, waiters: List<Waiter>) {
        // DOTO: fix empty list
        val assignedTables: List<Table> = customerToTable[customerGroup] ?: listOf()
        val mergeId: Id = assignedTables.minBy { it.id }.id
        if (assignedTables.size > 1) {
            FohReceptionLogger.logFohMergingTables(
                customerGroup.id,
                assignedTables.map { it.id }.sorted(),
                mergeId
            )
        }

        numberOfCustomersSeated += customerGroup.size
        numberOfWaitersSeated.addAll(waiters)
        numberOfTablesSeatedOn += assignedTables.size

        FohReceptionLogger.logFohSeating(customerGroup.id, mergeId, waiters.map { it.id!! })
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
                .last().also { if (it.id == null) it.id = getNextWaiterId() }
        }

        return freeWaiters.sortedWith(compareBy(nullsLast()) { it.id }).sortedBy { it.currentLoad }.first()
            .also { if (it.id == null) it.id = getNextWaiterId() }
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
        } // Step 1: Filter and sort
        val sortedTables: List<Table> =
            tables.filter { it.status == TableStatus.FREE }.filter { it.tableType == casualGroup.tableType }
                .sortedBy { it.id }
                .sortedBy { it.size } // Step 2 and 3: Perfect fit or single table with three quarters rule
        trySingleTable(
            casualGroup,
            sortedTables,
            true
        )?.also { return reserveSingleTable(it) } // Step 4: Multiple tables with three quarters rule
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
}
