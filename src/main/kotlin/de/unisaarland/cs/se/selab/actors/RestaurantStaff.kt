package de.unisaarland.cs.se.selab.actors

/**
 * The staff of one restaurant. The lists are mutable so staff change incidents can add and remove
 * members between evenings.
 */
data class RestaurantStaff(
    val cooks: MutableList<Cook>,
    val waiters: MutableList<Waiter>,
    val drivers: MutableList<Driver>
)
