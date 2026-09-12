package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.TableStatus

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
}
