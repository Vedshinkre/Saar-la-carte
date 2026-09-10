package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order

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
