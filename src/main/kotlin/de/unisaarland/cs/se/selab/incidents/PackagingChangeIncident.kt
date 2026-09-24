package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient

/**
 * Incident that changes an ingredient's packaging volume for the rest of the simulation.
 *
 * @property id the incident's unique id
 * @property evening the evening on which the incident fires
 * @property ingredient the ingredient whose packaging volume changes
 * @property packagingVolume the new packaging volume to apply
 */
class PackagingChangeIncident(
    override val id: Id,
    override var evening: Evening,
    private val ingredient: Ingredient,
    private val packagingVolume: Int
) : Incident(id, evening) {

    override val type: String = "PACKAGING"

    /**
     * Applies the new packaging volume to [ingredient].
     */
    override fun apply() {
        ingredient.packagingVolume = packagingVolume
    }
}
