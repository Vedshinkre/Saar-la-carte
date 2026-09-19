package de.unisaarland.cs.se.selab.actors

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.DeliveryLogger

// NOTE: each delivery log type logged for all drivers before next log type starts
// trip split into single steps of delivering phase instead of one `processTick` for this
/** driver: waits idle, carries an order to a customer, then returns */
class Driver {
    var id: Id? = null
    var currentOrder: Order? = null
    var targetGroup: CustomerGroup? = null

    var state: DriverState = DriverState.IDLE
    var totalTripTicks: Tick = 0
    var ticksToDest: Tick = 0

    var tripDistance: Int = 0 // one way trip distance (for cumulative distance logs)
    var distanceDriven: Int = 0
    // remainingTicks field in class diagram was redundant, so not an attribute here

    /** whether this leg of the trip is over, i.e. the driver is at the customer or back home */
    fun hasReachedDestination(): Boolean = ticksToDest <= 0

    /** drives a tick closer to the customer and logs how far the driver got */
    fun driveTowardsCustomer() {
        val driverId = id ?: return
        // a trip of 0 ticks, customer is less than 5 km away, has no distance left to drive
        // driver is already there, so no distance is covered and the remaining-tick counter must not drop below 0
        if (ticksToDest <= 0) return
        ticksToDest -= 1
        // need to accumulated distance covered when logging (it's not only distance covered in that tick)
        distanceDriven = minOf(distanceDriven + Constants.DRIVER_SPEED.toInt(), tripDistance)
        DeliveryLogger.logDeliveryDriving(driverId, distanceDriven, ticksToDest)
    }

    /** drives a tick closer to the restaurant; the return trip is not logged per tick */
    fun driveTowardsRestaurant() {
        if (ticksToDest <= 0) return
        ticksToDest -= 1
    }

    /** logs the arrival at the customer, still in the tick the last leg was driven */
    fun logArrival() {
        val driverId = id ?: return
        val group = targetGroup ?: return
        val order = currentOrder ?: return

        DeliveryLogger.logDeliveryArrival(driverId, group.id, order.id)
    }

    /** "hands over" the food in the arrival tick, then turns around */
    fun handOverToCustomer() {
        val driverId = id ?: return
        val group = targetGroup ?: return
        val order = currentOrder ?: return

        // a rejected or otherwise aborted order can no longer be delivered
        if (order.dishes.any { it.status == DishStatus.ABORTED }) {
            DeliveryLogger.logDeliveryFailed(driverId, order.id, group.id)
        } else {
            order.deliveredAt = Time.tick
            DeliveryLogger.logDeliveryFinished(driverId, order.id, group.id)
            // a group that already has a negative experience, stays negative however early the rest arrives
            group.experience = when {
                group.experience == ExperienceType.NEGATIVE -> ExperienceType.NEGATIVE
                Time.tick < group.visitingAt -> ExperienceType.POSITIVE
                Time.tick == group.visitingAt -> ExperienceType.NEUTRAL
                else -> ExperienceType.NEGATIVE
            }
        }

        startReturnTrip()
    }

    /** logs the arrival back at the restaurant and frees the driver for the next order */
    fun finishReturnTrip() {
        id?.let { DeliveryLogger.logDeliveryReturned(it) }
        state = DriverState.IDLE
        currentOrder = null
        targetGroup = null
        totalTripTicks = 0
        ticksToDest = 0
        tripDistance = 0
        distanceDriven = 0
    }

    // NOTE: return trip always takes as long as the first one did, regardless of how it ended */
    private fun startReturnTrip() {
        ticksToDest = totalTripTicks / 2
        state = DriverState.RETURNING
    }
}
