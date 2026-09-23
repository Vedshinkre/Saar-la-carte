package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.DeliveryLogger
import kotlin.math.ceil

/** delivery coordinator */
class DeliveryProcessor(
    private val drivers: List<Driver>,
    private val deliveryGroups: List<CustomerGroup>,
) {
    // log every delivery log type for all drivers that need it in asc group id, before next type follows
    // one phase per log type instead of per driver
    /** process delivery */
    fun processDelivering() {
        // the drivers that were already on the road when this tick started: a driver that only
        // prepares in this tick starts driving in the next one
        val outbound = driversInState(DriverState.DELIVERING)
        val returning = driversInState(DriverState.RETURNING)

        // 1. delivery preparation only for drivers holding a full order
        driversInState(DriverState.WAITING)
            .filter { it.currentOrder?.areAllDishesServed() == true }
            .forEach { prepareDelivery(it) }

        // 2. delivery driving
        outbound.forEach { it.driveTowardsCustomer() }
        returning.forEach { it.driveTowardsRestaurant() }

        // 3. delivery arrival and 4. delivery finished / delivery failed
        val arriving = outbound.filter { it.hasReachedDestination() }
        arriving.forEach { it.logArrival() }
        arriving.forEach { it.handOverToCustomer() }

        // 5. delivery given up
        processAbortions()

        // DOTO: check if this fix is required and if there's a neater way to refactor later
        // release drivers still waiting for the rest of an order that will never be completed
        // (given up, or aborted because the kitchen stopped)
        releaseStrandedDrivers()

        // 6. delivery returned
        returning.filter { it.hasReachedDestination() }.forEach { it.finishReturnTrip() }
    }

    /**
     * Frees drivers that are still waiting outside with an order that can no longer be completed.
     * They never left the restaurant, so nothing is logged; they become available again
     */
    private fun releaseStrandedDrivers() {
        for (driver in driversInState(DriverState.WAITING)) {
            val order = driver.currentOrder ?: continue
            if (order.dishes.none { it.status == DishStatus.ABORTED }) continue

            driver.state = DriverState.IDLE
            driver.currentOrder = null
            driver.targetGroup = null
        }
    }

    /** whether any driver is currently free to take on a new delivery */
    fun isDriverAvailable(): Boolean = drivers.any { it.state == DriverState.IDLE }

    /** computes the trip length and sends the driver off in the next tick */
    private fun prepareDelivery(driver: Driver) {
        val group = driver.targetGroup as? CasualGroup ?: return
        val order = driver.currentOrder ?: return
        val driverId = driver.id ?: return

        val ticks = ceil(group.deliveryDistance / Constants.DRIVER_SPEED).toInt()
        driver.ticksToDest = ticks
        driver.totalTripTicks = ticks * 2
        driver.tripDistance = group.deliveryDistance
        driver.distanceDriven = 0
        driver.state = DriverState.DELIVERING

        DeliveryLogger.logDeliveryPreparation(driverId, order.id, group.id, ticks)
    }

    // abort if not reached by end of third tick after order was wanted, decided per delivery group
    private fun processAbortions() {
        val rejecting = deliveryGroups.sortedBy { it.id }
            .mapNotNull { group -> group.currentOrder?.let { order -> group to order } }
            .filter { (group, order) -> hasAborted(group, order) }

        for ((group, order) in rejecting) {
            // NOTE: no more setting ABORTED, it will be deprecated later when code is desphagettified
            // order.dishes.forEach { if (it.status != DishStatus.EATEN) it.status = DishStatus.ABORTED }
            order.deliveryGivenUp = true
            group.experience = ExperienceType.NEGATIVE
            DeliveryLogger.logDeliveryGivenUp(group.id, order.id)
        }
    }

    private fun hasAborted(group: CustomerGroup, order: Order): Boolean {
        if (order.deliveredAt != null) {
            return false
        }
        // a delivery is only given up once, but a group whose meals were aborted for another
        // reason (never cooked, kitchen closed) is still waiting and still gives up
        if (order.deliveryGivenUp) {
            return false
        }
        return Time.tick >= group.visitingAt + Constants.CUSTOMER_DELIVERY_WAIT_TICKS
    }

    private fun driversInState(state: DriverState): List<Driver> =
        drivers.filter { it.state == state }.sortedBy { it.targetGroup?.id ?: Int.MAX_VALUE }
}
