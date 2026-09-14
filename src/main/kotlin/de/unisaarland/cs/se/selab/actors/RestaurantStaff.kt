package de.unisaarland.cs.se.selab.actors

/**
 * beep beep I;m a document
 */
data class RestaurantStaff(
    val cooks: MutableList<Cook>,
    val waiters: MutableList<Waiter>,
    val drivers: MutableList<Driver>
)
