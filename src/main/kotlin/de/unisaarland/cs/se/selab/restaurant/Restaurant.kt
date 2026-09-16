package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.FohReceptionLogger
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger

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
    val eventCustomers: MutableList<EventGroup> = mutableListOf()
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
        val eventGroupsForTonight = eventCustomers.filter { it.isVisitingTonight() }.sortedBy { it.id }
        val comingRegulars = mutableListOf<RegularGroup>()
        val comingEventGroups = mutableListOf<EventGroup>()
        for (regularGroup in regularGroups) {
            if (!frontOfHouse.reserveTables(regularGroup)) {
                InitialAndPrepLogger.logFohNoReservation(regularGroup.id)
            } else {
                customerQueue.addLast(regularGroup)
                comingRegulars.add(regularGroup)
            }
        }
        for (eventGroup in eventGroupsForTonight) {
            if (!frontOfHouse.reserveTables(eventGroup)) {
                InitialAndPrepLogger.logFohNoReservation(eventGroup.id)
            } else {
                customerQueue.addLast(eventGroup)
                comingEventGroups.add(eventGroup)
            }
            eventCustomers.remove(eventGroup)
        }
        val collectiveOrderHistory = mutableListOf<Order>()
        for (regularGroup in comingRegulars) {
            val orderHistory = regularGroup.orderHistory
            collectiveOrderHistory.addAll(orderHistory)
        }
        // val freeSeats = frontOfHouse.getFreeSeats().values.sum()
        // val eventDishes = comingEventGroups.
        // kitchen.planForIngredients(collectiveOrderHistory, freeSeats, restaurantStats.menu, )
        // kitchen.planForIngredients()
        // logPantryRestocked
        // getEventFreeSeats()
        // setAvailableEventSeats
        // getFreeSeats
        // setAvailableSeats
        // getAvailableDrivers
        // setAvailableDrivers
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
            endOfOpeningTime()
        }
        if (Time.maxTicks == Time.getCurrentTick()) {
            endEvening()
        }
    }

    private fun endEvening() {
        frontOfHouse.resetDrivers()
    }

    private fun endOfOpeningTime() { // free tables
        frontOfHouse.endFohOpeningTime(
            positiveRatings = restaurantStats.positiveRatings,
            negativeRatings = restaurantStats.negativeRatings
        )
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

            if (customerGroup.visitingAt == Time.tick &&
                (customerGroup !is CasualGroup || !customerGroup.wantsDelivery)
            ) {
                FohReceptionLogger.logRestaurantArrival(customerGroup.id)
            }

            val keepInQueue =
                frontOfHouse.processArrival(customerGroup, restaurantStats.menu) || customerGroup.isWaitingToBeSeated
            if (!keepInQueue) {
                iterator.remove()
            }
        }
        frontOfHouse.logAndResetSeatingOrderingTickStatus()
    }

    /** returns whether a driver is available */
    fun isDriverAvailable(): Boolean = frontOfHouse.isDriverAvailable()

    // statistics
    /** gets a number of cooked meals */
    fun getNumberOfCookedMeals(): Int = kitchen.numberOfCookedMeals

    /** gets a number of cooked meals */
    fun getNumberOfCustomersServed(): Int = frontOfHouse.numberOfCustomersServed

    /** gets a number of cooked meals */
    fun getNumberOfCustomersDelivered(): Int = frontOfHouse.numberOfCustomersDelivered

    /** Call with CustomerGroup.
     *  Adds customerGroup to customerQueue. */
    fun addToCustomerQueue(customerGroup: CustomerGroup) {
        customerQueue.add(customerGroup)
    }
}
