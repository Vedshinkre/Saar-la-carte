package incidents

import actors.RestaurantStaff
import enums.CookType
import enums.StaffType
import types.Evening
import types.Id

class StaffChangeIncident(
    private val id: Id,
    private val evening: Evening,
    private val restaurantId: Id,
    private val number: Int,
    private val staffType: StaffType,
    private val cookType: CookType,
    private val restaurantStaff: RestaurantStaff
) : Incident(id, evening)