package de.unisaarland.cs.se.selab

/**
 * Global object that keeps track of the ticks and time of the simulation
 */
object Time {
    internal var tick: Tick = 1
    internal var evening: Evening = 1
    internal var maxTicks: Tick = 0

    /**
     * resets the Current tick to 1 (used before next serving phase)
     */
    fun resetTick() {
        tick = 1
    }

    /**
     * returns the current Evening
     */
    fun getEvening(): Evening {
        return evening
    }

    /**
     * returns the max ticks for the simulation
     */
    fun getMaxTicks(): Tick {
        return maxTicks
    }

    /**
     * returns the current tick
     */
    fun getCurrentTick(): Tick {
        return tick
    }

    /**
     * increments the evening by 1
     */
    fun incrementEvening() {
        evening += 1
    }

    /**
     * sets the max ticks
     */
    fun setMaxTicks(maxTicks: Tick) {
        this.maxTicks = maxTicks
    }

    /**
     * increments the tick by 1
     */
    fun incrementTick() {
        tick++
    }
}
