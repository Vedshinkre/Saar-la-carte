package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Constants
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

    /** arrival order: group type (REGULAR, EVENT, CASUAL) first, then ascending id */
    val arrivalOrder: Comparator<CustomerGroup> = compareBy({
        when (it) {
            is RegularGroup -> 0
            is EventGroup -> 1
            is CasualGroup -> 2
        }
    }, { it.id })

    /**
     * The restaurant's live capacity and rating statistics.
     *
     * @return this restaurant's [RestaurantStats]
     */
    fun getRestaurantStats(): RestaurantStats {
        return restaurantStats
    }

    /**
     * The restaurant's front-of-house and kitchen staff.
     *
     * @return this restaurant's [RestaurantStaff]
     */
    fun getRestaurantStaff(): RestaurantStaff {
        return staff
    }

    /**
     * Preparation phase of this restaurant: reserves tables for tonight's event groups first and then
     * for [regularGroups], queues the groups that got a table, plans the ingredients for the evening,
     * and publishes the free seats and drivers to the browsing service.
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
        val regularSeatsWithNoHistory = comingRegulars.filter { it.orderHistory.isEmpty() }
            .sumOf { frontOfHouse.getReservedSeats(it) }
        val freeSeats = frontOfHouse.getFreeSeats().values.sum() + regularSeatsWithNoHistory
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
        refreshAvailableDrivers()
        if (tick == Constants.TICK_PER_EVENING) {
            endEvening()
        }
    }

    /** Publishes the current free seats per table type to the browsing service. */
    private fun refreshAvailableSeats() {
        restaurantStats.availableSeats.putAll(frontOfHouse.getAvailableSeats())
    }

    /** Publishes the current number of free drivers to the browsing service. */
    private fun refreshAvailableDrivers() {
        restaurantStats.availableDrivers = frontOfHouse.getAvailableDrivers()
    }

    /**
     * Runs the seven steps of a tick. After the opening time only delivering, eating and rating
     * still run, so [isBeforeClosing] switches the other four off. On the closing tick everyone still
     * inside is sent out before the rating step, so their ratings belong to this tick.
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

    /** Runs the rating step and adds the new ratings to both the running and the simulation-only totals. */
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
     *
     * No new customers are admitted in the last 3 ticks of the opening time (spec adjustment),
     * including groups that already arrived on an earlier tick but could not be seated -
     * the status logs still fire for that tick regardless.
     */
    private fun processArrivalSeatingOrdering() {
        if (restaurantStats.isOpen()) {
            val processedGroups: MutableList<CustomerGroup> = mutableListOf()
            for (customerGroup in customerQueue.sortedWith(arrivalOrder)) {
                if (Time.tick < customerGroup.visitingAt && !customerGroup.isVisitingThisTick()) { continue }
                logArrivalIfInHouse(customerGroup)

                if (frontOfHouse.processArrival(customerGroup, restaurantStats.menu)) {
                    processedGroups.add(customerGroup)
                }
            }
            customerQueue.removeAll(processedGroups)
        } else {
            customerQueue.forEach { frontOfHouse.refuseLateArrival(it) }
            customerQueue.clear()
        }
        frontOfHouse.logAndResetSeatingOrderingTickStatus()
    }

    private fun logArrivalIfInHouse(customerGroup: CustomerGroup) {
        val wantsDelivery = customerGroup is CasualGroup && customerGroup.wantsDelivery

        if (customerGroup.isVisitingThisTick() && !wantsDelivery) {
            FohReceptionLogger.logRestaurantArrival(customerGroup.id)
        }
    }

    /** returns whether a driver is available */
    // DOTO this function doesn't need to exist
    fun isDriverAvailable(): Boolean = frontOfHouse.isDriverAvailable()

    // statistics
    /** meals cooked in this restaurant over the whole simulation */
    fun getNumberOfCookedMeals(): Int = kitchen.numberOfCookedMeals

    /** meals served to in-house customers over the whole simulation */
    fun getNumberOfCustomersServed(): Int = frontOfHouse.numberOfCustomersServed

    /** customers reached by delivery over the whole simulation, one per delivered meal */
    fun getNumberOfCustomersDelivered(): Int = frontOfHouse.numberOfCustomersDelivered

    /** Call with CustomerGroup.
     *  Adds customerGroup to customerQueue. Joining the queue starts a new visit, so anything the
     *  group still carries from an earlier evening - its order, its headcount and its experience -
     *  is cleared first. */
    fun addToCustomerQueue(customerGroup: CustomerGroup) {
        customerGroup.startNewVisit()
        customerQueue.add(customerGroup)
    }
}
