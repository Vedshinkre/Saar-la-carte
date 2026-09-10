package restaurant

import actors.RestaurantStaff
import customer.CustomerGroup
import customer.EventGroup
import food.Order
import food.Stock
import java.util.Queue
import kotlin.collections.ArrayDeque
import kotlin.collections.List
import kotlin.collections.listOf

class Restaurant(
    private val restaurantStats: RestaurantStats,
    private val name: String,
    private val staff: RestaurantStaff,
    private val tables: List<Table>,
    private val stock: Stock
) {
    private val eventCustomers: List<EventGroup> = listOf()
    private val customerQueue: Queue<CustomerGroup> = ArrayDeque<CustomerGroup>()
    private val frontOfHouse: FrontOfHouse
    private val kitchen: Kitchen

    init {
        val pantry: Pantry = Pantry(stock)
        val orderQueue: Queue<Order> = ArrayDeque<Order>()
        val countertop: Countertop = Countertop(pantry, orderQueue, staff.cooks)
        frontOfHouse = FrontOfHouse(tables, staff.waiters, staff.drivers, countertop)
        kitchen = Kitchen(staff.cooks, pantry, orderQueue)
    }
}