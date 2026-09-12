package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id

abstract class Incident(private val id: Id, private val evening: Evening) {
    /**
     sake of detect
     */
    fun getId(): Id {
        return id
    }
}
