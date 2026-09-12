package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableStatus

private const val THREE_QUARTERS: Double = 3.0 / 4.0

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

    private fun getInHouseGroups(): List<CustomerGroup> {
        return inHouseGroupsToWaiter.keys.toList()
    }

    private fun getAssignedWaiter(customerId: Id): Waiter {
        return inHouseGroupsToWaiter.entries
            .first { it.key.id == customerId }
            .value
    }

    private fun getAssignedTableId(customerId: Id): List<Id> {
        return customerToTable.entries
            .first { it.key.id == customerId }
            .value
            .map { it.id }
    }

    private fun dismantleTable(customerId: Id) {
        val tables = customerToTable.entries
            .first { it.key.id == customerId }
            .value

        tables.forEach {
            it.status = TableStatus.FREE
        }

        customerToTable.remove(
            customerToTable.keys.first { it.id == customerId }
        )
    }

    fun processEscorting() {
        val inHouseGroups = getInHouseGroups()
        inHouseGroups.forEach {
            val order = it.currentOrder
            if (order != null) {
                if (order.areAllDishesEaten()) {
                    val waiter = getAssignedWaiter(it.id)
                    // val tableId = getAssignedTableId(it.id)
                    waiter.escort(it)
                }
            }
            if (it.getCustomersRemainingInRestaurant() == 0) {
                if (it is CasualGroup) dismantleTable(it.id)
            }
        }
    }

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
}
