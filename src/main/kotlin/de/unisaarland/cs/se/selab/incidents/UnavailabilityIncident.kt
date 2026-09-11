package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock

/**
 * Unavailablity Incident Class
 */
class UnavailabilityIncident(
    private val id: Id,
    private val evening: Evening,
    private val ingredient: Ingredient,
    private val duration: Int,
    private val stock: Stock
) : Incident(id, evening) {
    /**
     * to check Overlapping
     */
    fun overlapsWith(other: UnavailabilityIncident): Boolean {
        if (this.ingredient != other.ingredient) return false
        val thisEnd = this.evening + this.duration
        val otherEnd = other.evening + other.duration
        return this.evening < otherEnd && other.evening < thisEnd
    }

    /**
     * to put Error in case of conficting Durations
     */

    fun conflictMessage(other: UnavailabilityIncident): String =
        "Ingredient $ingredient: incident $id (evening $evening, duration $duration) " +
            "overlaps incident ${other.id} (evening ${other.evening}, duration ${other.duration})"
}
