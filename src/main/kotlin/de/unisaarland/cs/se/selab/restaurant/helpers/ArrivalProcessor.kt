package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.TableStatus
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
    private val turnedAwayGroups: MutableList<CustomerGroup>,
    private val recruitWaitersForEventGroup: (ActionType, EventGroup) -> List<Waiter>,
    private val countertop: Countertop
) {
    private var numberOfTablesSeatedOn: Int = 0
    private var numberOfCustomersSeated: Int = 0
    private var numberOfWaitersSeated: Int = 0
    private var nextWaiterId: Id = 1
        get() = field++

    /** Call with CustomerGroup and menu.
     *  Returns true if CustomerGroup was processed successfully, false otherwise.
     *  To decide whether to remove the CustomerGroup from the customerQueue use the formula
     *  processArrivalSeatingOrdering(customerGroup, menu) || customerGroup.isWaitingToBeSeated.
     *  If true keep in the customerQueue, otherwise remove from the customerQueue. */
    fun processArrivalSeatingOrdering(customerGroup: CustomerGroup/*, menu: List<Recipe>*/): Boolean {
        val isInHouse: Boolean =
            customerGroup is RegularGroup || (customerGroup is CasualGroup && !customerGroup.wantsDelivery)
        if (isInHouse && !seatRegularOrCasualGroup(customerGroup)) {
            return false
        }

        return turnedAwayGroups.first().visitingAt == customerGroup.visitingAt
    }

    /** Call with CustomerGroup and menu. (Method overloading redirects EventGroups to this implementation)
     *  Returns true if EventGroup was processed successfully, false otherwise.
     *  To decide whether to remove the EventGroup from the customerQueue use the formula
     *  processArrivalSeatingOrdering(customerGroup, menu) || customerGroup.isWaitingToBeSeated.
     *  If true keep in the customerQueue, otherwise remove from the customerQueue. */
    fun processArrivalSeatingOrdering(eventGroup: EventGroup/*, menu: List<Recipe>*/): Boolean {
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
        return false
    }

    /** Call after processArrivalSeatingOrdering has been called with each customerGroup in customerQueue.
     *  Logs status and then performs side effect by resetting counters. */
    fun logAndResetSeatingOrderingTickStatus() {
        FohReceptionLogger.logSeatingStatus(numberOfWaitersSeated, numberOfCustomersSeated, numberOfTablesSeatedOn)
        numberOfTablesSeatedOn = 0
        numberOfCustomersSeated = 0
        numberOfWaitersSeated = 0 // NOTE: add ordering status variables, log and then reset them
    }

    private fun seatRegularOrCasualGroup(customerGroup: CustomerGroup): Boolean {
        val waiter: Waiter = assignWaiter(customerGroup) ?: return rejectForNoWaiter(customerGroup)

        if (customerGroup is CasualGroup && !assignTables(customerGroup)) {
            FohReceptionLogger.logFohNoSeating(customerGroup.id, waiter.id!!)
            turnedAwayGroups.addLast(customerGroup)
            customerGroup.experience = ExperienceType.NEGATIVE
            customerGroup.isWaitingToBeSeated = false
            return false
        }

        successfulSeating(customerGroup, listOf(waiter))
        waiter.tickLoads[ActionType.SEAT] = waiter.tickLoads[ActionType.SEAT]!! + customerGroup.size
        waiter.currentLoad += customerGroup.size

        return true
    }

    private fun rejectForNoWaiter(customerGroup: CustomerGroup): Boolean {
        FohReceptionLogger.logFohNoSeatingNoWaitstaff(customerGroup.id)
        if (!customerGroup.isWaitingToBeSeated) {
            if (customerGroup is RegularGroup) {
                customerToTable.remove(customerGroup)
            }
            turnedAwayGroups.addLast(customerGroup)
            customerGroup.experience = ExperienceType.NEGATIVE
        }
        return false
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
