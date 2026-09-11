package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Recipe

class RestaurantStats(
    val restaurantId: Id,
    private val restaurantType: RestaurantType,
    private val openingTickStart: Tick,
    private val openingTickEnd: Tick,
    private val event: Boolean,
    private var positiveRatings: Int,
    private var negativeRatings: Int,
    private val menu: List<Recipe>
)
