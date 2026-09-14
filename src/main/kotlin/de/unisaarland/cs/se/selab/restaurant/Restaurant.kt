package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Stock

class Restaurant(
    private val restaurantStats: RestaurantStats,
    private val name: String,
    private val staff: RestaurantStaff,
    private val tables: List<Table>,
    private val stock: Stock
) {
    private val eventCustomers: List<EventGroup> = listOf()
    private val customerQueue: ArrayDeque<CustomerGroup> = ArrayDeque<CustomerGroup>()
    private val frontOfHouse: FrontOfHouse
    private val kitchen: Kitchen

    init {
        val pantry: Pantry = Pantry(stock)
        val orderQueue: ArrayDeque<Order> = ArrayDeque<Order>()
        val countertop: Countertop = Countertop(pantry, orderQueue, staff.cooks)
        frontOfHouse = FrontOfHouse(tables, staff.waiters, staff.drivers, countertop)
        kitchen = Kitchen(staff.cooks, pantry, orderQueue)
    }

    // TODO: acceptDeliveryOrder(order, openingEndTick): Boolean
    // acceptDeliveryOrder returns if the maximum cook ticks of a dish in the order + current tick <= openingEndTick

    /**
     * sake of detect
     */
    fun getRestaurantStats(): RestaurantStats {
        return restaurantStats
    }
    /**
     * sake of detect
     */
    fun getRestaurantStaff(): RestaurantStaff {
        return staff
    }

    /** returns whether a driver is available */
    fun isDriverAvailable(): Boolean = frontOfHouse.isDriverAvailable()

    // statistics
    fun getNumberOfCookedMeals(): Int = kitchen.numberOfCookedMeals
    fun getNumberOfCustomersServed(): Int = frontOfHouse.numberOfCustomersServed
    fun getNumberOfCustomersDelivered(): Int = frontOfHouse.numberOfCustomersDelivered
}
