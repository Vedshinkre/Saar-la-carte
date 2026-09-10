package incidents

import Evening
import Id
import actors.RestaurantStaff
import enums.CookType
import enums.StaffType

class StaffChangeIncident(
	private val id: Id,
	private val evening: Evening,
	private val restaurantId: Id,
	private val number: Int,
	private val staffType: StaffType,
	private val cookType: CookType,
	private val restaurantStaff: RestaurantStaff
) : Incident(id, evening)