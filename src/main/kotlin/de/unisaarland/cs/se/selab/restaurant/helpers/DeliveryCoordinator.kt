package de.unisaarland.cs.se.selab.restaurant.helpers

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.loggers.DeliveryLogger
import kotlin.math.ceil

/** delivery coordinator */
class DeliveryCoordinator(
    private val drivers: List<Driver>,
) {
    /** starts driving drivers who just received a full order, and advances already-driving drivers */
    fun processDelivering() {
        val readyDrivers =
            drivers.filter { it.state == DriverState.WAITING }.sortedBy { it.targetGroup?.id ?: Int.MAX_VALUE }
        for (driver in readyDrivers) {
            prepareDelivery(driver)
        }

        val activeDrivers = drivers.filter { it.state == DriverState.DELIVERING || it.state == DriverState.RETURNING }
            .sortedBy { it.targetGroup?.id ?: Int.MAX_VALUE }
        for (driver in activeDrivers) {
            driver.processTick()
        }
    }

    /** whether any driver is currently free to take on a new delivery */
    fun isDriverAvailable(): Boolean = drivers.any { it.state == DriverState.IDLE }

    /** computes the trip length and starts driving the driver */
    private fun prepareDelivery(driver: Driver) {
        val group = driver.targetGroup as? CasualGroup ?: return
        val order = driver.currentOrder ?: return
        val driverId = driver.id ?: return

        val ticks = ceil(group.deliveryDistance / Constants.DRIVER_SPEED).toInt()
        driver.ticksToDest = ticks
        driver.totalTripTicks = ticks * 2
        driver.state = DriverState.DELIVERING

        DeliveryLogger.logDeliveryPreparation(driverId, order.id, group.id, ticks)
    }
}
