package customer

import enums.ExperienceType
import enums.TableType
import food.Order
import types.Id
import types.Tick

abstract class CustomerGroup(
    private val id: Id,
    private val size: Int,
    private val tableType: TableType,
    private val visitingAt: Tick,
    private val foodPreference: List<FoodPreference>,
) {
    private val waitingSince: Tick? = null
    private var currentlySeatedCustomers = size
    private var experience: ExperienceType = ExperienceType.NEUTRAL
    var currentOrder: Order? = null
}