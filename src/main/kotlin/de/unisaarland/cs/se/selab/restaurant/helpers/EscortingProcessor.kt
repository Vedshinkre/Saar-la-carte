package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logFohEscorting
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logFohEscortingStatus
import de.unisaarland.cs.se.selab.restaurant.Table

/** escorting coordinator */
class EscortingProcessor(
    private val customerToTable: MutableMap<CustomerGroup, List<Table>>,
    private val eventGroups: MutableList<EventGroup>,
    private val inHouseGroupsToWaiter: Map<CustomerGroup, Waiter>,
    private val getInHouseGroups: () -> List<CustomerGroup>,
    private val getServingPriority: (CustomerGroup) -> Int,
    private val recruitWaitersForEventGroup: (ActionType, EventGroup) -> List<Waiter>,
    private val getNextWaiterId: () -> Id
) {
    var customerEscortingNumber = 0

    private val escortingWaitstaffSet: MutableSet<Id> = mutableSetOf()

    /** the number of distinct waiters that escorted this tick */
    val waitstaffNumber: Int get() = escortingWaitstaffSet.size

    /**
     * Escorts eligible in-house groups whose dishes have been eaten.
     * Dismantles tables of casual groups after all customers have left
     * and logs the escorting results.
     */

    fun processEscorting() { // the counters describe *this* tick only, so they start over on every call
        customerEscortingNumber = 0
        escortingWaitstaffSet.clear()
        val groupsToBeEscorted =
            (getInHouseGroups() + eventGroups).sortedWith(compareBy({ getServingPriority(it) }, { it.id }))

        groupsToBeEscorted.forEach {
            val order = it.currentOrder
            if (order != null && order.areAllDishesEaten()) {
                if (it is EventGroup) {
                    escortEventGroup(recruitWaitersForEventGroup(ActionType.ESCORT, it), it)
                } else {
                    val waiter = getAssignedWaiter(it.id)
                    val customersBefore = it.customersRemainingInRestaurant
                    val waiterId = waiter.ensureId(getNextWaiterId)
                    waiter.escort(it)
                    logEscortedBy(waiterId, customersBefore - it.customersRemainingInRestaurant, it)
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

    private fun logEscortedBy(waiterId: Id, customersEscorted: Int, group: CustomerGroup) {
        if (customersEscorted <= 0) {
            return
        }
        logFohEscorting(
            waiterId,
            customersEscorted,
            group.id,
            getAssignedTableIds(group.id).min()
        )
        customerEscortingNumber += customersEscorted
    }

    private fun escortEventGroup(waiters: List<Waiter>, group: EventGroup) {
        waiters.forEach { waiter ->
            val waiterId = waiter.ensureId(getNextWaiterId)
            val customersBefore = group.customersRemainingInRestaurant
            waiter.escortEventGroups(group)
            val customersEscorted = customersBefore - group.customersRemainingInRestaurant
            if (customersEscorted > 0) {
                escortingWaitstaffSet.add(waiterId)
                logEscortedBy(waiter.ensureId(getNextWaiterId), customersEscorted, group)
            }

            escortingWaitstaffSet.add(waiterId)
            customerEscortingNumber += customersEscorted
        }
    }

    private fun getAssignedWaiter(customerId: Id): Waiter {
        return inHouseGroupsToWaiter.entries.first { it.key.id == customerId }.value
    }

    private fun getAssignedTableIds(customerId: Id): List<Id> {
        return customerToTable.entries.first { it.key.id == customerId }.value.map { it.id }
    }

    // TODO(maybe customerID as parameter is more work than just customer)
    /** dismantles tables, sets status to free, removes it from the list */
    fun dismantleTable(customerId: Id) {
        val tables = customerToTable.entries.first { it.key.id == customerId }.value

        tables.forEach {
            it.status = TableStatus.FREE
        }

        customerToTable.remove(
            customerToTable.keys.first { it.id == customerId }
        )
    }

    /**
     * Removes all remaining in-house customers at closing.
     * Returns groups that did not finish eating and therefore require
     * a negative rating.
     *
     * @return unfinished customer groups
     */
    fun escortAllAtClosing(groups: List<CustomerGroup>) {
        groups.forEach { group ->
            group.customersRemainingInRestaurant = 0
        }
    }
}
