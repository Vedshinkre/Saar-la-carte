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
 * Incident that changes the staff of a restaurant
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

    /**
     * applies the staff change incident (adds/removes staff from a restaurant)
     */
    override fun apply() {
        when (staffType) {
            StaffType.COOK -> applyCookChange()
            StaffType.WAITSTAFF -> applyWaitstaffChange()
            StaffType.DRIVER -> applyDriverChange()
        }
    }

    private fun applyCookChange() {
        val type = cookType ?: return
        if (number > 0) {
            repeat(number) { restaurantStaff.cooks.addFirst(Cook(type)) }
        } else {
            removeCooks(type, number.absoluteValue)
        }
    }

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

    private fun applyWaitstaffChange() {
        if (number > 0) {
            repeat(number) { restaurantStaff.waiters.addFirst(Waiter()) }
        } else {
            repeat(minOf(number.absoluteValue, restaurantStaff.waiters.size)) {
                restaurantStaff.waiters.removeLast()
            }
        }
    }

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
