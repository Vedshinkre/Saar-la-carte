package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Recipe

/**
 * class shared between restaurant and browsing service
 */
class RestaurantStats(
    val restaurantId: Id,
    val restaurantType: RestaurantType,
    val openingTickStart: Tick,
    val openingTickEnd: Tick,
    val event: Boolean,
    var positiveRatings: Int,
    var negativeRatings: Int,
    val menu: List<Recipe>,
) {
    var simulationPositiveRatings: Int = 0
    var simulationNegativeRatings: Int = 0
    var availableDrivers: Int = 0
    val availableSeats: MutableMap<TableType, Int> = mutableMapOf()
    val availableEventSeats: MutableMap<TableType, Int> = mutableMapOf()

    /**
     * sake of detect
     */
    fun isOpen(): Boolean {
        return Time.tick in openingTickStart..openingTickEnd - 3
    }
}
