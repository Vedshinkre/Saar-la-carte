package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.StaffType
import kotlin.math.absoluteValue

/**
 * STAFF incident: adds or removes cooks, waiters or drivers of a restaurant before an evening.
 *
 * @property number how many staff members to add (positive) or remove (negative)
 * @property cookType the type of cook affected; only set for cook changes
 */
class StaffChangeIncident(
    override val id: Id,
    override val evening: Evening,
    private val number: Int,
    private val staffType: StaffType,
    private val cookType: CookType?,
    private val restaurantStaff: RestaurantStaff
) : Incident(id, evening) {

    override val type: String = "STAFF"

    /** Adds or removes the staff members. Removing more than there are leaves none, never fewer. */
    override fun apply() {
        when (staffType) {
            StaffType.COOK -> applyCookChange()
            StaffType.WAITSTAFF -> applyWaitstaffChange()
            StaffType.DRIVER -> applyDriverChange()
        }
    }

    /** Adds or removes cooks of [cookType]. */
    private fun applyCookChange() {
        val type = cookType ?: return
        if (number > 0) {
            repeat(number) { restaurantStaff.cooks.addFirst(Cook(type)) }
        } else {
            removeCooks(type, number.absoluteValue)
        }
    }

    /** Removes up to [count] cooks of the given [type]. */
    private fun removeCooks(type: CookType, count: Int) {
        var remaining = count
        val iterator = restaurantStaff.cooks.iterator()
        while (iterator.hasNext() && remaining > 0) {
            if (iterator.next().type == type) {
                iterator.remove()
                remaining--
            }
        }
    }

    /** Adds or removes waiters. */
    private fun applyWaitstaffChange() {
        if (number > 0) {
            repeat(number) { restaurantStaff.waiters.addFirst(Waiter()) }
        } else {
            repeat(minOf(number.absoluteValue, restaurantStaff.waiters.size)) {
                restaurantStaff.waiters.removeLast()
            }
        }
    }

    /** Adds or removes drivers. */
    private fun applyDriverChange() {
        if (number > 0) {
            repeat(number) { restaurantStaff.drivers.addFirst(Driver()) }
        } else {
            repeat(minOf(number.absoluteValue, restaurantStaff.drivers.size)) {
                restaurantStaff.drivers.removeLast()
            }
        }
    }
}
