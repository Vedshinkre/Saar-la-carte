package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
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
    private val customerQueue: ArrayDeque<CustomerGroup> = ArrayDeque()
    private val frontOfHouse: FrontOfHouse
    private val kitchen: Kitchen

    init {
        val pantry = Pantry(stock)
        val orderQueue: ArrayDeque<Order> = ArrayDeque()
        val countertop = Countertop(pantry, orderQueue, staff.cooks)
        frontOfHouse = FrontOfHouse(tables, staff.waiters, staff.drivers, countertop)
        kitchen = Kitchen(staff.cooks, pantry, orderQueue, restaurantStats.restaurantType)
    }

    /**
     acceptDeliveryOrder returns if the maximum cook ticks of a dish in the order and current tick <= openingEndTick
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
        frontOfHouse.clearActionLoads()
        processArrivalSeatingOrdering()
        kitchen.processCooking()

        frontOfHouse.processServing()
        frontOfHouse.processDelivering()
        frontOfHouse.processEating()
        frontOfHouse.processEscorting()

        val (positiveRatings, negativeRatings) = frontOfHouse.processRatings(
            restaurantStats.positiveRatings,
            restaurantStats.negativeRatings
        )
        restaurantStats.positiveRatings = positiveRatings
        restaurantStats.negativeRatings = negativeRatings
        if (restaurantStats.openingTickEnd == Time.getCurrentTick()) {
            endEvening()
        }
    }

    private fun endEvening() {
        // free tables
        frontOfHouse.endFohEvening()
        kitchen.resetKitchen()
    }

    /**
     * Processes arrival, seating and ordering for every customer group currently in the
     * queue, removing groups that are done per FrontOfHouse.processArrivalSeatingOrdering's
     * keep-in-queue formula, then logs and resets the tick's seating/ordering status.
     */
    private fun processArrivalSeatingOrdering() {
        val iterator = customerQueue.iterator()
        while (iterator.hasNext()) {
            val customerGroup = iterator.next()
            val keepInQueue = frontOfHouse.processArrival(customerGroup, restaurantStats.menu) ||
                customerGroup.isWaitingToBeSeated
            if (!keepInQueue) {
                iterator.remove()
            }
        }
        frontOfHouse.logAndResetSeatingOrderingTickStatus()
    }

    /** returns whether a driver is available */
    fun isDriverAvailable(): Boolean = frontOfHouse.isDriverAvailable()

    // statistics
    /**
     * gets a number of cooked meals
     */
    fun getNumberOfCookedMeals(): Int = kitchen.numberOfCookedMeals

    /**
     * gets a number of cooked meals
     */
    fun getNumberOfCustomersServed(): Int = frontOfHouse.numberOfCustomersServed

    /**
     * gets a number of cooked meals
     */
    fun getNumberOfCustomersDelivered(): Int = frontOfHouse.numberOfCustomersDelivered

    /** Call with CustomerGroup.
     *  Adds customerGroup to customerQueue. */
    fun addToCustomerQueue(customerGroup: CustomerGroup) {
        customerQueue.add(customerGroup)
    }
}
