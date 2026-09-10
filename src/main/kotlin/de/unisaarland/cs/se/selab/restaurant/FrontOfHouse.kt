package restaurant

import actors.Waiter
import customer.CustomerGroup

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
}