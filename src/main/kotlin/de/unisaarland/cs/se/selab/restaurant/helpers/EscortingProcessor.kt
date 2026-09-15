package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logFohEscorting
import de.unisaarland.cs.se.selab.loggers.FohServiceLogger.logFohEscortingStatus
import de.unisaarland.cs.se.selab.restaurant.Table

/** escorting coordinator */
class EscortingProcessor(
    private val customerToTable: MutableMap<CustomerGroup, List<Table>>,
    private val inHouseGroupsToWaiter: Map<CustomerGroup, Waiter>,
    private val getInHouseGroups: () -> List<CustomerGroup>,
    private val getServingPriority: (CustomerGroup) -> Int,
) {
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
                    // waiter was assigned during seating, so it already has an id.
                    logFohEscorting(
                        requireNotNull(waiter.id),
                        customersEscorted,
                        it.id,
                        getAssignedTableIds(it.id).min()
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

    /** escorts customers still inside the FOH after closing time
     * @return return the groups that need a forced negative rating
     * */
    fun escortAllAtClosing(): List<CustomerGroup> {
        val groups = getInHouseGroups()
        val unfinishedGroups = mutableListOf<CustomerGroup>()

        groups.forEach { group ->
            val order = group.currentOrder

            if (order == null || !order.areAllDishesEaten()) {
                unfinishedGroups.add(group)
            }

            val waiter = getAssignedWaiter(group.id)
            waiter.escort(group)
        }

        return unfinishedGroups
    }
}
