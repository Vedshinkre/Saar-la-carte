package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock

/**
 * Incident that makes an ingredient unavailable for a number of evenings.
 *
 * @property id the incident's unique id
 * @property evening the evening on which the incident fires
 * @property ingredient the ingredient that becomes unavailable
 * @property duration how many evenings the ingredient stays unavailable
 * @property stock the stock to mark the ingredient unavailable in
 */
class UnavailabilityIncident(
    override val id: Id,
    override val evening: Evening,
    private val ingredient: Ingredient,
    private var duration: Int,
    private val stock: Stock
) : Incident(id, evening) {

    override val type: String = "UNAVAILABLE"

    /**
     * Marks [ingredient] unavailable in [stock] for [duration] evenings.
     */
    override fun apply() { // set duration in Int to stock <ingredient,<bool;int>>
        stock.setIngredientToUnavailable(ingredient, duration)
    }

    /**
     * Whether this incident's unavailability window for [ingredient] overlaps with [other]'s.
     *
     * @param other the other unavailability incident to compare against
     * @return `true` if the two incidents affect the same ingredient and their evening ranges overlap
     */
    fun overlapsWith(other: UnavailabilityIncident): Boolean {
        if (this.ingredient != other.ingredient) return false
        val thisEnd = this.evening + this.duration
        val otherEnd = other.evening + other.duration
        return this.evening < otherEnd && other.evening < thisEnd
    }

    /**
     * to put Error in case of tconflicting Durations
     */

    fun conflictMessage(other: UnavailabilityIncident): String =
        "Ingredient $ingredient: incident $id (evening $evening, duration $duration) " +
            "overlaps incident ${other.id} (evening ${other.evening}, duration ${other.duration})"
}
