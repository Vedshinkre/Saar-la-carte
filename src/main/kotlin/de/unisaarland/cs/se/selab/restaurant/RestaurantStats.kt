package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
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
    private var positiveRatings: Int,
    private var negativeRatings: Int,
    val menu: List<Recipe>,
) {
    var availableDrivers: Int = 0
    var availableSeats: MutableMap<TableType, Int> = mutableMapOf()
    var availableEventSeats: MutableMap<TableType, Int> = mutableMapOf()


}
