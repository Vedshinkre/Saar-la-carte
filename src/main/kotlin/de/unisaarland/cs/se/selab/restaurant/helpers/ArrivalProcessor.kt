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
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order
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
    // item 95: the (merged) tables that were seated on this tick. A merge set contributes its
    // merged id once, and a table a group vacated again this tick - because nobody could order -
    // is not counted twice when the next group is seated on it.
    private val tablesSeatedOn: MutableSet<Id> = mutableSetOf()
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
            } else {
                // the group has left, so it keeps no claim on the table it was just seated at.
                // A REGULAR group's table stays reserved for the rest of the evening (item 164).
                customerToTable.remove(customerGroup)?.forEach { it.status = TableStatus.FREE }
            }
            inHouseGroupsToWaiter.remove(customerGroup)
            turnedAwayGroups.addLast(customerGroup)
        }
        orderSuccess(customerGroup)

        return true
    }

    /** Call with CustomerGroup and menu.
     *  Returns true if CustomerGroup should be removed from the customerQueue, false otherwise. */
    fun processArrival(eventGroup: EventGroup, menu: List<Recipe>): Boolean {
        val recruitedWaiters: List<Waiter> = recruitWaitersForEventGroup(ActionType.SEAT, eventGroup)
        // EVENT seating is all-or-nothing, so the distribution is planned first and the SEATING
        // tick load is only charged once the whole group is known to fit.
        val seatingPlan: MutableList<Pair<Waiter, Int>> = mutableListOf()
        var eventGroupSize: Int = eventGroup.size
        for (waiter in recruitedWaiters) {
            if (eventGroupSize <= 0) {
                break
            }
            val remainingSeatingLoad: Int = Constants.ACTION_LIMIT - waiter.getTickLoad(ActionType.SEAT)
            val seatingLoad: Int = min(remainingSeatingLoad, eventGroupSize)
            eventGroupSize -= seatingLoad
            seatingPlan.addLast(waiter to seatingLoad)
        }
        // every waiter in the plan attempted to seat, so each keeps an id even if the attempt fails
        val consumedWaiters: List<Waiter> = seatingPlan.map { (waiter, _) ->
            waiter.also { it.ensureId(getNextWaiterId) }
        }

        if (eventGroupSize != 0) {
            FohReceptionLogger.logFohNoSeatingNoWaitstaff(eventGroup.id)
            customerToTable.remove(eventGroup)
            turnedAwayGroups.addLast(eventGroup)
            eventGroup.experience = ExperienceType.NEGATIVE
        } else {
            seatingPlan.forEach { (waiter, seatingLoad) -> waiter.addToTickLoad(ActionType.SEAT, seatingLoad) }
            successfulSeating(eventGroup, consumedWaiters)
            if (eventGroup.placeOrder(consumedWaiters, menu, countertop)) {
                val currentOrder = eventGroup.currentOrder
                val waitStaffId = consumedWaiters.mapNotNull { it.id }
                if (currentOrder != null) {
                    FohReceptionLogger.logFohOrdering(
                        eventGroup.id,
                        currentOrder.id,
                        currentOrder.dishNameToAmount(),
                        waitStaffId
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

    /** The restaurant does not accept new customers in the last 3 ticks of its opening time, not
     *  even ones that arrived earlier and could not be seated yet. Such a group simply leaves:
     *  its reservation is released and it is turned away without an arrival, seating or ordering
     *  log, but it still gets to rate the restaurant this tick. */
    fun refuseLateArrival(customerGroup: CustomerGroup) {
        customerToTable[customerGroup]?.forEach { it.status = TableStatus.FREE }
        customerToTable.remove(customerGroup)
        if (customerGroup is RegularGroup) {
            customerGroup.failedAttempts++
        }
        customerGroup.experience = ExperienceType.NEGATIVE
        turnedAwayGroups.addLast(customerGroup)
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
    private fun orderSuccess(customerGroup: CustomerGroup) {
        val currentOrder = customerGroup.currentOrder
        if (currentOrder != null) {
            logPlacedOrder(customerGroup, currentOrder)
        }
        // item 104: the group reports the customers who found no dish whether or not the rest of
        // the group managed to order, so this line also fires when nobody in the group could order
        val customersWhoLeftAfterOrdering = customerGroup.getCustomersWhoLeft()
        if (customersWhoLeftAfterOrdering > 0) {
            FohReceptionLogger.logFohNoOrdering(
                customerGroup.id,
                customersWhoLeftAfterOrdering
            )
        }
    }

    private fun logPlacedOrder(customerGroup: CustomerGroup, currentOrder: Order) {
        val assignedWaiter = inHouseGroupsToWaiter[customerGroup]
        if (customerGroup is RegularGroup) {
            // the streak is only broken once the group is actually served (see EatingProcessor),
            // otherwise two consecutive "nobody was served" evenings could never add up to two failures
            customerGroup.addOrderToHistory(currentOrder)
        }
        if (assignedWaiter != null) {
            val assignedWaiterId = assignedWaiter.ensureId(getNextWaiterId)
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
        }
        // counted for the ordering status whether they ate in or ordered a delivery
        customersOrdered.add(customerGroup)
    }

    /** Call after processArrivalSeatingOrdering has been called with each customerGroup in customerQueue.
     *  Logs status and then performs side effect by resetting counters. */
    fun logAndResetSeatingOrderingTickStatus() {
        FohReceptionLogger.logSeatingStatus(numberOfWaitersSeated.size, numberOfCustomersSeated, tablesSeatedOn.size)
        tablesSeatedOn.clear()
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
            FohReceptionLogger.logFohNoSeating(customerGroup.id, waiter.ensureId(getNextWaiterId))
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
                // the table stays reserved for the group for the rest of the evening (item 164)
                // and is only released when the opening time ends, so the mapping has to survive:
                // dropping it here would strand the table as RESERVED for every later evening
                customerGroup.failedAttempts++
            }
            turnedAwayGroups.addLast(customerGroup)
            customerGroup.experience = ExperienceType.NEGATIVE
            return CustomerStatus.TO_LEAVE
        }
        return CustomerStatus.NO_WAITER
    }

    private fun successfulSeating(customerGroup: CustomerGroup, waiters: List<Waiter>) {
        val assignedTables: List<Table> = customerToTable[customerGroup] ?: listOf()
        // without a table there is no seating to report; leaving early keeps this from throwing
        val mergeId: Id = assignedTables.minByOrNull { it.id }?.id ?: return
        if (assignedTables.size > 1) {
            FohReceptionLogger.logFohMergingTables(
                customerGroup.id,
                assignedTables.map { it.id }.sorted(),
                mergeId
            )
        }

        numberOfCustomersSeated += customerGroup.size
        numberOfWaitersSeated.addAll(waiters)
        // a merged table counts only once towards the number of tables seated on
        tablesSeatedOn.add(mergeId)

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
                .last().also { it.ensureId(getNextWaiterId) }
        }

        return freeWaiters.sortedWith(compareBy(nullsLast()) { it.id }).sortedBy { it.currentLoad }.first()
            .also { it.ensureId(getNextWaiterId) }
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
        // BAR tables cannot be merged, and the candidates are all of the group's own table type
        if (customerGroup.tableType == TableType.BAR) {
            return null
        }
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

        val tableIterator: MutableIterator<Table> = acc.iterator()
        while (tableIterator.hasNext()) {
            val table: Table = tableIterator.next()
            if (customerGroup.size < mergeSize && customerGroup.size <= mergeSize - table.size) {
                tableIterator.remove()
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
