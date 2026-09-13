package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order

/** Represents an abstract customer group. */
sealed class CustomerGroup(
    val id: Id,
    val size: Int,
    val tableType: TableType,
    private val visitingAt: Tick, // Could you make this public
    private val foodPreferences: List<FoodPreference>, // Could you make this public
) {
    private val waitingSince: Tick? = null

    // TODO: remove waitingSince
    // TODO: add var customersRemainingInRestaurant = size
    // TODO: add var experience = NEUTRAL
    // TODO: add var currentOrder = null

    // relevant to F27
    var currentOrder: Order? = null
    var customersRemainingInRestaurant = size
}
