package de.unisaarland.cs.se.selab

/** shared constants */
object Constants {
    const val DRIVER_SPEED = 5.0
    const val CUSTOMER_DELIVERY_WAIT_TICKS = 3
    const val ACTION_LIMIT = 10
    const val MIN_TABLE_OCCUPANCY: Double = 3.0 / 4.0
    const val HUNDRED = 100.0
    const val PARTIAL_SERVING_WAIT_TICKS = 1 // ticks to wait until an order can start being served partially
    const val UNSERVED_WAIT_TICKS = 5 // max number of ticks a customer waits to be served
    const val ADDITIONAL_UNSERVED_WAIT_TICKS = 2 // max wait ticks after at least another dish in the order is served
    const val EXPECTATION_WINDOW_TICKS = 4 // ticks within ordering that food must arrive for a positive experience
}
