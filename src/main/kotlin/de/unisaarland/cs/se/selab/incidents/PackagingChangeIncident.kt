package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient

/**
 * This class handles Packaging Change Incident
 */
class PackagingChangeIncident(
    override val id: Id,
    override var evening: Evening,
    private val ingredient: Ingredient,
    private val packagingVolume: Int
) : Incident(id, evening) {
    /**
     * This function Handles the incident
     */
    fun apply() {
        ingredient.setPackagingVolume(packagingVolume)
    }
}
