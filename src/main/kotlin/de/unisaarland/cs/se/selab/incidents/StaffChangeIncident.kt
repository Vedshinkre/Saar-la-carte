package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.StaffType

class StaffChangeIncident(
    private val id: Id,
    private val evening: Evening,
    private val restaurantId: Id,
    private val number: Int,
    private val staffType: StaffType,
    private val cookType: CookType?,
    private val restaurantStaff: RestaurantStaff
) : Incident(id, evening) {

}
