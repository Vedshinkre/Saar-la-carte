package de.unisaarland.cs.se.selab.system

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.loggers.StatisticsLogger
import de.unisaarland.cs.se.selab.loggers.TickStatusLogger
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.Restaurant

/**
 * Class that runs the simulation in the correct order of events: incidents ->
 * preparation phase -> serving phase (24 ticks) -> repeat per evening -> statistics,
 * once maxTicks has been reached.
 */
class Simulation(simdata: SimulationConfig) {
    var restaurants: List<Restaurant> = simdata.restaurants
    var browser: BrowsingService = BrowsingService(simdata.restaurantStats)
    var incidents: List<Incident> = simdata.incidents
    var customers: List<CustomerGroup> = simdata.customers

    // Total ticks elapsed across all evenings so far. Time only tracks the tick
    // within the current evening.
    private var ticksElapsed: Int = 0

    /**
     * The function that runs the simulation, gets called from main.
     */
    fun runSimulation() {
        InitialAndPrepLogger.logSimulationStart()

        while (ticksElapsed < Time.getMaxTicks()) {
            simulateEvening()
        }

        calculateStatistics()
    }

    /**
     * Runs a single evening: incidents, preparation phase, serving phase.
     */
    private fun simulateEvening() {
        executeIncidents()
        executePreparationPhase()
        executeServingPhase()

        if (ticksElapsed < Time.getMaxTicks()) {
            Time.resetTick()
            Time.incrementEvening()
        }
    }

    /**
     * Logs and applies every incident that occurs before the current evening, in
     * ascending order of incident id.
     */
    private fun executeIncidents() {
        val evening = Time.getEvening()
        val incidentsForTonight = incidents.filter { it.evening == evening }.sortedBy { it.id }

        for (incident in incidentsForTonight) {
            InitialAndPrepLogger.logIncident(incident.id, incident.type)
            incident.apply()
        }
    }

    /**
     * Preparation phase, per the sequence diagram:
     *  - log that the evening has started (now takes the evening number)
     *  - gather event groups actually arriving tonight and regulars visiting tonight
     *  - per restaurant, in ascending id order: tag the Logger with that restaurant's
     *    id, re-fetch tonight's event groups, and call prepareForEvening(regulars)
     */
    private fun executePreparationPhase() {
        InitialAndPrepLogger.logPreparationStart()

        val regularsTonight = getRegularsForTonight().sortedBy { it.id }

        for (restaurant in restaurants.sortedBy { it.getRestaurantStats().restaurantId }) {
            val restaurantId = restaurant.getRestaurantStats().restaurantId
            Logger.restaurantID = restaurantId
            val regularsForRestaurant = regularsTonight.filter { it.restaurantId == restaurantId }

            restaurant.prepareForEvening(regularsForRestaurant)
        }
    }

    private fun reserveForEventGroupsInAdvance(eventGroups: List<EventGroup>) {
        for (eventGroup in eventGroups) {
            val eventRestaurantId = browser.getEligibleRestaurants(eventGroup)
            if (eventRestaurantId != null) {
                TickStatusLogger.logRestaurantDecision(eventGroup.id, eventRestaurantId)
                val eventRestaurant = getRestaurantById(eventRestaurantId)
                eventGroup.currentRestaurantType = eventRestaurant.getRestaurantStats().restaurantType
                eventRestaurant.eventCustomers.addFirst(eventGroup)
            } else {
                TickStatusLogger.logRestaurantNoDecision(eventGroup.id)
            }
        }
    }

    /**
     * All [RegularGroup]s that are visiting tonight, regardless of restaurant.
     */
    private fun getRegularsForTonight(): List<RegularGroup> =
        filterRegularGroups(customers).filter {
            it.isVisitingTonight()
        }

    /**
     * Filters a mixed customer list down to just the [RegularGroup]s.
     */
    private fun filterRegularGroups(customers: List<CustomerGroup>): List<RegularGroup> =
        customers.filterIsInstance<RegularGroup>()

    /**
     * All [CasualGroup]s that are visiting at some point tonight, in ascending id order.
     */
    private fun getCasualsForTonight(): List<CasualGroup> =
        filterCasualGroups(customers).filter { it.isVisitingTonight() }.sortedBy { it.id }

    /**
     * Filters a mixed customer list down to just the [CasualGroup]s.
     */
    private fun filterCasualGroups(customers: List<CustomerGroup>): List<CasualGroup> =
        customers.filterIsInstance<CasualGroup>()

    /**
     * Of tonight's casuals, the ones specifically due to decide/act this tick.
     * Takes the *current tick* (not the evening) as its first argument, per the
     * sequence diagram.
     */
    private fun getCasualsForThisTick(casuals: List<CasualGroup>): List<CasualGroup> =
        casuals.filter { it.isVisitingThisTick() }

    /**
     * [EventGroup]s whose event is exactly three evenings away in ascending id order.
     */
    private fun getEventGroupsForReservation(): List<EventGroup> =
        customers.filterIsInstance<EventGroup>().filter { it.visitingInThreeEvenings() }.sortedBy { it.id }

    /**
     * Serving phase: 24 ticks (or fewer, if maxTicks is reached first). Tonight's
     * casual groups are computed once, up front, and handed to each tick rather than
     * recomputed every tick.
     */
    private fun executeServingPhase() {
        TickStatusLogger.logServingStart()

        val eventGroupsDecidingTonight = getEventGroupsForReservation()
        val casualsTonight = getCasualsForTonight()
        var stoppedEarly = false

        while (Time.getCurrentTick() <= TICKS_PER_EVENING) {
            executeSingleTick(casualsTonight, eventGroupsDecidingTonight)

            ticksElapsed++
            val reachedMax = ticksElapsed >= Time.getMaxTicks()
            val reachedEndOfEvening = Time.getCurrentTick() == TICKS_PER_EVENING

            if (reachedMax && !reachedEndOfEvening) {
                // maxTicks is not a multiple of 24: stop right here, no "Serving ends" log.
                stoppedEarly = true
                break
            }

            Time.incrementTick()

            if (reachedMax) {
                break
            }
        }

        if (!stoppedEarly) {
            TickStatusLogger.logServingEnd()
        }
    }

    /**
     * Runs one tick:
     *  - log the current tick
     *  - restaurant decisions, EVENT groups first (only in the first tick, three evenings before
     *    their event) and then CASUAL groups, each in ascending id order
     *  - each deciding casual either gets assigned to a restaurant's queue (and a
     *    "decision" is logged) or is logged as unable to decide
     *  - every restaurant then simulates its own tick, in ascending id order, with
     *    the Logger's restaurantID tagged beforehand so its internal log lines carry
     *    the right restaurant id
     */
    private fun executeSingleTick(casualsTonight: List<CasualGroup>, eventGroupsDecidingTonight: List<EventGroup>) {
        TickStatusLogger.logCurrentTick()

        if (Time.getCurrentTick() == 1) {
            reserveForEventGroupsInAdvance(eventGroupsDecidingTonight)
        }

        val casualsThisTick = getCasualsForThisTick(casualsTonight)

        for (group in casualsThisTick) {
            val restaurantId = browser.getEligibleRestaurants(group)
            if (restaurantId != null) {
                TickStatusLogger.logRestaurantDecision(group.id, restaurantId)
                getRestaurantById(restaurantId).addToCustomerQueue(group)
            } else {
                TickStatusLogger.logRestaurantNoDecision(group.id)
            }
        }

        for (restaurant in restaurants.sortedBy { it.getRestaurantStats().restaurantId }) {
            Logger.restaurantID = restaurant.getRestaurantStats().restaurantId
            TickStatusLogger.logRestaurantStart()
            restaurant.simulateTick()
            TickStatusLogger.logRestaurantEnd()
        }
    }

    /**
     * Logs final simulation statistics per restaurant, in ascending restaurant id order.
     * (Not covered by this sequence diagram, so unchanged from before.)
     */
    private fun calculateStatistics() {
        StatisticsLogger.logSimulationStatsCalculated()

        for (restaurant in restaurants.sortedBy { it.getRestaurantStats().restaurantId }) {
            val stats = restaurant.getRestaurantStats()
            Logger.restaurantID = stats.restaurantId

            StatisticsLogger.logSimulationStatsCooked(restaurant.getNumberOfCookedMeals())
            StatisticsLogger.logSimulationStatsServed(restaurant.getNumberOfCustomersServed())
            StatisticsLogger.logSimulationStatsDelivered(restaurant.getNumberOfCustomersDelivered())
            StatisticsLogger.logSimulationStatsRatingsGiven(
                stats.simulationPositiveRatings + stats.simulationNegativeRatings
            )
        }
    }

    /**
     * Looks up a restaurant by its id.
     */
    private fun getRestaurantById(id: Id): Restaurant = restaurants.first { it.getRestaurantStats().restaurantId == id }

    private companion object {
        const val TICKS_PER_EVENING = 24
    }
}
