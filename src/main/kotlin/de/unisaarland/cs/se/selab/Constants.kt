package de.unisaarland.cs.se.selab

/** Numbers from the specification that are shared across the simulation. */
object Constants {
    /** Kilometres a delivery driver covers per tick. */
    const val DRIVER_SPEED = 5.0

    /** Ticks a delivery group waits after its wanted tick before it gives up. */
    const val CUSTOMER_DELIVERY_WAIT_TICKS = 3

    /** Actions of one type (seat, take order, serve, escort) a waiter can perform per tick. */
    const val ACTION_LIMIT = 10

    /** Share of a table's (or merged table's) seats a group has to fill to be given it. */
    const val MIN_TABLE_OCCUPANCY: Double = 3.0 / 4.0

    /** 100 as a double; currently unused. */
    const val HUNDRED = 100.0

    /** Ticks after the first cooked meal during which a partly cooked order is held back from serving. */
    const val PARTIAL_SERVING_WAIT_TICKS = 1

    /** Waiting limit for food; a group nobody was served leaves this many ticks minus one after ordering. */
    const val UNSERVED_WAIT_TICKS = 5

    /** Extra ticks the rest of a table waits once at least one of its customers has been served. */
    const val ADDITIONAL_UNSERVED_WAIT_TICKS = 2

    /** Food served fewer ticks than this after ordering is a positive experience, exactly this many is neutral. */
    const val EXPECTATION_WINDOW_TICKS = 4

    /** A regular group's visiting tick must lie at least this many ticks before its restaurant closes. */
    const val REGULAR_VISITING_TICK_BUFFER = 3

    /** Ticks in one evening. */
    const val TICKS_PER_EVENING = 24

    /** How much distance can be traveled in a tick */
    const val DISTANCE_PER_TICK = 5

    /** extra ticks a delivery order factors in for cooking, in addition to travel time */
    const val DELIVERY_COOKING_TICKS = 3
}
