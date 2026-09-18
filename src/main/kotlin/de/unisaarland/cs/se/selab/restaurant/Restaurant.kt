package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.FohReceptionLogger

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
        val countertop = Countertop(pantry, orderQueue, staff.cooks, restaurantStats.restaurantType)
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
        // tables are reserved for each EVENT group first and only then for the REGULAR groups
        for (eventGroup in eventGroupsForTonight) {
            if (frontOfHouse.reserveTables(eventGroup)) {
                comingEventGroups.add(eventGroup)
            }
            eventCustomers.remove(eventGroup)
        }
        for (regularGroup in regularGroups) {
            if (frontOfHouse.reserveTables(regularGroup)) {
                comingRegulars.add(regularGroup)
            }
        }
        // the queue is processed by group type (REGULAR, EVENT, CASUAL), not in reservation order
        comingRegulars.forEach { addToCustomerQueue(it) }
        comingEventGroups.forEach { addToCustomerQueue(it) }
        val collectiveOrderHistory = mutableListOf<Order>()
        for (regularGroup in comingRegulars) {
            val orderHistory = regularGroup.orderHistory
            collectiveOrderHistory.addAll(orderHistory)
        }
        val freeSeats = frontOfHouse.getFreeSeats().values.sum()
        val eventDishes = mutableListOf<Pair<Recipe, Int>>()/*

             */
        for (eventGroup in comingEventGroups) {
            val eventDishName = eventGroup.getCurrentEventDish()
            val eventDish = restaurantStats.menu.filter { it.name == eventDishName }.first()
            eventDishes.add(Pair(eventDish, eventGroup.size))
        }
        kitchen.planForIngredients(
            collectiveOrderHistory,
            freeSeats,
            restaurantStats.menu,
            eventDishes
        )
        // setAvailableEventSeats -> I want to know if seats for EventSeats and normal are same
        // getFreeSeats -> Done
        // setAvailableSeats -> Done
        // getAvailableDrivers -> Done
        // setAvailableDrivers -> Done
        restaurantStats.availableDrivers = frontOfHouse.getAvailableDrivers()
        restaurantStats.availableSeats.putAll(frontOfHouse.getAvailableSeats())
        if (restaurantStats.event) {
            restaurantStats.availableEventSeats.putAll(frontOfHouse.getAvailableSeats())
        }
    }

    /**
     * Simulates one tick. Before the restaurant opens nothing happens and nothing is logged
     * between Restaurant Start and Restaurant End.
     */
    fun simulateTick() {
        val tick = Time.getCurrentTick()
        if (tick >= restaurantStats.openingTickStart) {
            simulateOpeningHoursTick(isBeforeClosing = tick <= restaurantStats.openingTickEnd)
        }
        refreshAvailableSeats()
        if (tick == Constants.TICK_PER_EVENING) {
            endEvening()
        }
    }

    /**
     * Items 153/154: the browsing service must see the seats that are free *right now* — tables
     * freed again during this tick are handed back — minus the seats already claimed by casual
     * groups that decided on this restaurant but are not sitting at a table yet. Reservations of
     * REGULAR and EVENT groups need no such correction: their tables are already RESERVED.
     */
    private fun refreshAvailableSeats() {
        val available = frontOfHouse.getAvailableSeats().toMutableMap()
        customerQueue.filterIsInstance<CasualGroup>()
            .filter { !it.wantsDelivery }
            .forEach { available[it.tableType] = (available[it.tableType] ?: 0) - it.size }
        restaurantStats.availableSeats.putAll(available)
    }

    /**
     * The seven tick steps. After the opening time has ended only delivering, eating and rating
     * may still run and log, so [isBeforeClosing] switches the other four steps off.
     */
    private fun simulateOpeningHoursTick(isBeforeClosing: Boolean) {
        frontOfHouse.clearActionLoads()
        if (isBeforeClosing) {
            processArrivalSeatingOrdering()
            kitchen.processCooking()
            frontOfHouse.processServing()
        }
        frontOfHouse.processDelivering()
        frontOfHouse.processEating()
        if (isBeforeClosing) {
            frontOfHouse.processEscorting()
        }

        val isClosingTick = isBeforeClosing && restaurantStats.openingTickEnd == Time.getCurrentTick()
        if (isClosingTick) {
            // the closing ratings belong to this tick's rating step, so everybody still inside is
            // escorted out before it runs rather than afterwards
            frontOfHouse.startFohClosing()
        }
        processRatingStep()
        if (isClosingTick) {
            endOfOpeningTime()
        }
    }

    private fun processRatingStep() {
        val previousPositiveRatings = restaurantStats.positiveRatings
        val previousNegativeRatings = restaurantStats.negativeRatings
        val (positiveRatings, negativeRatings) = frontOfHouse.processRatings(
            previousPositiveRatings,
            previousNegativeRatings
        )
        restaurantStats.positiveRatings = positiveRatings
        restaurantStats.negativeRatings = negativeRatings
        restaurantStats.simulationPositiveRatings += positiveRatings - previousPositiveRatings
        restaurantStats.simulationNegativeRatings += negativeRatings - previousNegativeRatings
    }

    private fun endEvening() {
        frontOfHouse.resetDrivers()
    }

    private fun endOfOpeningTime() { // free tables
        frontOfHouse.endFohOpeningTime()
        kitchen.resetKitchen()
    }

    /**
     * Processes arrival, seating and ordering for every customer group currently in the
     * queue, removing groups that are done per FrontOfHouse.processArrivalSeatingOrdering's
     * keep-in-queue formula, then logs and resets the tick's seating/ordering status.
     */
    private fun processArrivalSeatingOrdering() {
        val processedGroups: MutableList<CustomerGroup> = mutableListOf()
        for (customerGroup in customerQueue.sortedWith(arrivalOrder)) {
            if (Time.tick < customerGroup.visitingAt && !customerGroup.isVisitingThisTick()) { continue }

            if (customerGroup.isVisitingThisTick()) {
                FohReceptionLogger.logRestaurantArrival(customerGroup.id)
            }

            if (frontOfHouse.processArrival(customerGroup, restaurantStats.menu)) {
                processedGroups.add(customerGroup)
            }
        }
        customerQueue.removeAll(processedGroups)
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
     *  Adds customerGroup to customerQueue. Joining the queue starts a new visit, so anything the
     *  group still carries from an earlier evening - its order, its headcount and its experience -
     *  is cleared first. */
    fun addToCustomerQueue(customerGroup: CustomerGroup) {
        customerGroup.startNewVisit()
        customerQueue.add(customerGroup)
    }

    /** Group type (REGULAR, EVENT, CASUAL) first, then ascending id. */
    val arrivalOrder: Comparator<CustomerGroup> = compareBy({
        when (it) {
            is RegularGroup -> 0
            is EventGroup -> 1
            is CasualGroup -> 2
        }
    }, { it.id })
}
