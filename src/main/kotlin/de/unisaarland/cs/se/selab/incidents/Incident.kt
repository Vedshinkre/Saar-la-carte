package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id

/**
 * abstract class for the incident
 */
abstract class Incident(open val id: Id, open val evening: Evening) {

    /**
     * Type of incident string for logging purposes
     */
    abstract val type: String

    /**
     * Application of Incident
     */
    abstract fun apply(): Unit
}
