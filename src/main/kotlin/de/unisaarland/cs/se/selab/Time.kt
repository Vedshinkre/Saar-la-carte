package de.unisaarland.cs.se.selab

/**
 * The simulation clock, shared by everything that needs the current time.
 *
 * @property tick the tick of the current evening, from 1 to 24
 * @property evening the current evening, starting at 1
 * @property maxTicks the `--maxTicks` limit on the whole run
 * @property ticksElapsed ticks simulated so far across all evenings, compared against [maxTicks]
 */
object Time {
    internal var tick: Tick = 1
    internal var evening: Evening = 1
    internal var maxTicks: Tick = 0
    internal var ticksElapsed: Tick = 0

    /** Sets the tick back to 1 for the next evening's serving phase. */
    fun resetTick() {
        tick = 1
    }

    /** The current evening. */
    fun getEvening(): Evening {
        return evening
    }

    /** The `--maxTicks` limit on the whole run. */
    fun getMaxTicks(): Tick {
        return maxTicks
    }

    /** The tick of the current evening. */
    fun getCurrentTick(): Tick {
        return tick
    }

    /** Moves on to the next evening. */
    fun incrementEvening() {
        evening += 1
    }

    /** Sets the `--maxTicks` limit on the whole run. */
    fun setMaxTicks(maxTicks: Tick) {
        this.maxTicks = maxTicks
    }

    /** Moves on to the next tick of the evening. */
    fun incrementTick() {
        tick++
    }

    /** Counts one more simulated tick towards [maxTicks]. */
    fun incrementTicksElapsed() {
        ticksElapsed++
    }
}
