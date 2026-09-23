package de.unisaarland.cs.se.selab.system

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.loggers.InitialAndPrepLogger
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.loggers.StatisticsLogger
import de.unisaarland.cs.se.selab.loggers.TickStatusLogger
import de.unisaarland.cs.se.selab.restaurant.BrowsingService
import de.unisaarland.cs.se.selab.restaurant.Restaurant

/**
 * Runs the whole simulation: evening after evening of incidents, preparation and serving until
 * `maxTicks` ticks have been simulated, then the final statistics.
 *
 * @property browser the browsing service casual and event groups use to decide on a restaurant
 */
class Simulation(simdata: SimulationConfig) {
    var restaurants: List<Restaurant> = simdata.restaurants
    var browser: BrowsingService = BrowsingService(simdata.restaurantStats)
    var incidents: List<Incident> = simdata.incidents
    var customers: List<CustomerGroup> = simdata.customers
    private val stock: Stock = simdata.stock

    /** Runs every evening until `maxTicks` is reached and logs the statistics. Called once by `main`. */
    fun runSimulation() {
        Order.resetIds()
        InitialAndPrepLogger.logSimulationStart()

        while (Time.ticksElapsed < Time.getMaxTicks()) {
            simulateEvening()
        }

        calculateStatistics()
    }

    /**
     * Runs one evening: incidents, preparation and serving. The clock only moves on to the next
     * evening if ticks are left.
     */
    private fun simulateEvening() {
        customers.forEach { it.resetForNewEvening() }
        executeIncidents()
        executePreparationPhase()
        executeServingPhase()
        stock.applyUnavailableDurations()

        if (Time.ticksElapsed < Time.getMaxTicks()) {
            Time.resetTick()
            Time.incrementEvening()
        }
    }

    /** Logs and applies the incidents of the current evening, in ascending id. */
    private fun executeIncidents() {
        val evening = Time.getEvening()
        val incidentsForTonight = incidents.filter { it.evening == evening }.sortedBy { it.id }

        for (incident in incidentsForTonight) {
            InitialAndPrepLogger.logIncident(incident.id, incident.type)
            incident.apply()
        }
    }

    /**
     * Logs the start of the preparation phase and lets every restaurant, in ascending id, prepare
     * for the regular groups visiting it tonight.
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

    /**
     * Lets each event group decide on a restaurant three evenings before its event and registers it
     * there, logging the decision or the lack of one.
     */
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

    /** All regular groups visiting tonight, at any restaurant. */
    private fun getRegularsForTonight(): List<RegularGroup> =
        filterRegularGroups(customers).filter {
            it.isVisitingTonight()
        }

    /** The regular groups among [customers]. */
    private fun filterRegularGroups(customers: List<CustomerGroup>): List<RegularGroup> =
        customers.filterIsInstance<RegularGroup>()

    /** All casual groups visiting tonight, in ascending id. */
    private fun getCasualsForTonight(): List<CasualGroup> =
        filterCasualGroups(customers).filter { it.isVisitingTonight() }.sortedBy { it.id }

    /** The casual groups among [customers]. */
    private fun filterCasualGroups(customers: List<CustomerGroup>): List<CasualGroup> =
        customers.filterIsInstance<CasualGroup>()

    /** The groups of [casuals] that decide on a restaurant in the current tick. */
    private fun getCasualsForThisTick(casuals: List<CasualGroup>): List<CasualGroup> =
        casuals.filter { it.isVisitingThisTick() }

    /** Event groups whose event is exactly three evenings away, in ascending id. */
    private fun getEventGroupsForReservation(): List<EventGroup> =
        customers.filterIsInstance<EventGroup>().filter { it.visitingInThreeEvenings() }.sortedBy { it.id }

    /**
     * Runs the 24 ticks of the serving phase. If `maxTicks` runs out in the middle of the evening,
     * it stops there without logging the end of the serving phase.
     */
    private fun executeServingPhase() {
        TickStatusLogger.logServingStart()

        val eventGroupsDecidingTonight = getEventGroupsForReservation()
        val casualsTonight = getCasualsForTonight()
        var stoppedEarly = false

        while (Time.getCurrentTick() <= TICKS_PER_EVENING) {
            executeSingleTick(casualsTonight, eventGroupsDecidingTonight)

            Time.incrementTicksElapsed()
            val reachedMax = Time.ticksElapsed >= Time.getMaxTicks()
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
     * Runs one tick: logs it, lets the deciding groups choose a restaurant (event groups in tick 1
     * only, then casual groups, each in ascending id), and then simulates every restaurant in
     * ascending id.
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
     * Logs the final statistics, four lines per restaurant in ascending id. Only ratings given
     * during the simulation are counted, not the ones the restaurant started with.
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

    /** The restaurant with the given [id]. */
    private fun getRestaurantById(id: Id): Restaurant = restaurants.first { it.getRestaurantStats().restaurantId == id }

    /** Constants of the simulation loop. */
    private companion object {
        const val TICKS_PER_EVENING = 24
    }
}
