package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id

/**
 * A scheduled disruption that changes some part of the simulation state when it fires.
 *
 * @property id the incident's unique id
 * @property evening the evening on which the incident fires
 */
abstract class Incident(open val id: Id, open val evening: Evening) {

    /**
     * Type of incident string for logging purposes
     */
    abstract val type: String

    /**
     * Applies this incident's effect to the simulation state.
     */
    abstract fun apply(): Unit
}
