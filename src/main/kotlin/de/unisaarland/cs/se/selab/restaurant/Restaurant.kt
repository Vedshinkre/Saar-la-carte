package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Stock

/**
 * Class that coordinates the simulation of one tick for a restaurant
 */
class Restaurant(
    private val restaurantStats: RestaurantStats,
    val name: String,
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
        kitchen = Kitchen(staff.cooks, pantry, orderQueue, restaurantStats.restaurantType)
    }

    /**
     acceptDeliveryOrder returns if the maximum cook ticks of a dish in the order + current tick <= openingEndTick
     */
    fun acceptDeliveryOrder(order: Order, openingEndTick: Tick): Boolean {
        return order.orderedAt == openingEndTick
    }

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

    /**
     * reserves tables for regulars and eventGroups. Plans the ingredients needed for them
     */
    fun prepareForEvening(regularGroups: List<RegularGroup>) {
        regularGroups.size
        eventCustomers.filter { it.isVisitingTonight() }
    }

    /**
     * Simulates one tick
     */
    fun simulateTick() {
        frontOfHouse.processArrivalSeatingOrdering(customerQueue.first(), restaurantStats.menu)
    }

    /** returns whether a driver is available */
    fun isDriverAvailable(): Boolean = frontOfHouse.isDriverAvailable()

    // statistics
    /**
     * gets number of cooked meals
     */
    fun getNumberOfCookedMeals(): Int = kitchen.numberOfCookedMeals

    /**
     * gets number of cooked meals
     */
    fun getNumberOfCustomersServed(): Int = frontOfHouse.numberOfCustomersServed

    /**
     * gets number of cooked meals
     */
    fun getNumberOfCustomersDelivered(): Int = frontOfHouse.numberOfCustomersDelivered

    /** Call with CustomerGroup.
     *  Adds customerGroup to customerQueue. */
    fun addToCustomerQueue(customerGroup: CustomerGroup) {
        customerQueue.add(customerGroup)
    }
}
