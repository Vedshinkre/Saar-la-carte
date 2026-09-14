package de.unisaarland.cs.se.selab.actors

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.DeliveryLogger

/** driver: waits idle, carries an order out to a customer, then returns */
class Driver {
    var id: Id? = null
    var currentOrder: Order? = null
    var targetGroup: CustomerGroup? = null

    var state: DriverState = DriverState.IDLE
    var totalTripTicks: Tick = 0
    var ticksToDest: Tick = 0
    // remainingTicks field in class diagram was redundant, so not an attribute here

    /** advances driver by one tick only if DELIVERING or RETURNING */
    fun processTick() {
        when (state) {
            DriverState.DELIVERING -> driveToCustomer()
            DriverState.RETURNING -> driveToRestaurant()
            else -> Unit
        }
    }

    /** drives a tick closer to the customer, updates experience in case order is aborted */
    private fun driveToCustomer() {
        val group = targetGroup as? CasualGroup ?: return
        val order = currentOrder ?: return
        val driverId = id ?: return

        if (Time.tick > group.visitingAt + Constants.CUSTOMER_DELIVERY_WAIT_TICKS) {
            abortOrder(order)
            group.experience = ExperienceType.NEGATIVE
            DeliveryLogger.logDeliveryGivenUp(group.id, order.id)
            startReturnTrip()
            return
        }

        ticksToDest -= 1
        DeliveryLogger.logDeliveryDriving(driverId, Constants.DRIVER_SPEED.toInt(), ticksToDest)

        if (ticksToDest <= 0) {
            arriveAtCustomer(driverId, group, order)
        }
    }

    /** "hand over" food to the customer if the order wasn't aborted */
    private fun arriveAtCustomer(driverId: Id, group: CasualGroup, order: Order) {
        DeliveryLogger.logDeliveryArrival(driverId, group.id, order.id)

        if (order.dishes.any { it.status == DishStatus.ABORTED }) {
            DeliveryLogger.logDeliveryFailed(driverId, order.id, group.id)
        } else {
            order.deliveredAt = Time.tick
            DeliveryLogger.logDeliveryFinished(driverId, order.id, group.id)
            group.experience = when {
                Time.tick < group.visitingAt -> ExperienceType.POSITIVE
                Time.tick == group.visitingAt -> ExperienceType.NEUTRAL
                else -> ExperienceType.NEGATIVE
            }
        }

        startReturnTrip()
    }

    private fun abortOrder(order: Order) {
        for (dish in order.dishes) {
            if (dish.status != DishStatus.EATEN) {
                dish.status = DishStatus.ABORTED
            }
        }
    }

    // NOTE: return trip always takes as long as the first one did, regardless of how it ended */
    private fun startReturnTrip() {
        ticksToDest = totalTripTicks / 2
        state = DriverState.RETURNING
    }

    /** drives one tick closer to the restaurant, sets status to IDLE once back */
    private fun driveToRestaurant() {
        val driverId = id ?: return
        ticksToDest -= 1

        if (ticksToDest <= 0) {
            DeliveryLogger.logDeliveryReturned(driverId)
            state = DriverState.IDLE
            currentOrder = null
            targetGroup = null
        }
    }
}
